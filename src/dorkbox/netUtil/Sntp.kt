package dorkbox.netUtil

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * SNTP client for retrieving time.
 *
 * https://tools.ietf.org/html/rfc2030
 *
 * Sample usage:
 * ```
 * val adjustedSystemTime = Sntp.update("time.mit.edu").now
 *```
 */
object Sntp {
    /**
     * Gets the version number.
     */
    const val version = "2.5"

    /**
     * SNTP client for retrieving time.
     *
     * https://tools.ietf.org/html/rfc2030
     *
     * Sample usage:
     * ```
     * val adjustedSystemTime = Sntp.update("time.mit.edu").now
     *```
     */
    class SntpClient {
        companion object {
            private class InvalidServerReplyException(message: String) : Exception(message)

            private const val REFERENCE_TIME_OFFSET = 16
            private const val ORIGINATE_TIME_OFFSET = 24
            private const val RECEIVE_TIME_OFFSET = 32
            private const val TRANSMIT_TIME_OFFSET = 40
            private const val NTP_PACKET_SIZE = 48
            private const val NTP_PORT = 123
            private const val NTP_MODE_CLIENT = 3
            private const val NTP_MODE_SERVER = 4
            private const val NTP_MODE_BROADCAST = 5
            private const val NTP_VERSION = 3
            private const val NTP_LEAP_NOSYNC = 3
            private const val NTP_STRATUM_DEATH = 0
            private const val NTP_STRATUM_MAX = 15

            // Number of seconds between Jan 1, 1900 and Jan 1, 1970
            // 70 years plus 17 leap days
            private const val OFFSET_1900_TO_1970 = (365L * 70L + 17L) * 24L * 60L * 60L

            @Throws(InvalidServerReplyException::class)
            private fun checkValidServerReply(leap: Int, mode: Int, stratum: Int, transmitTime: Long) {
                if (leap == NTP_LEAP_NOSYNC) {
                    throw InvalidServerReplyException("unsynchronized server")
                }
                if (mode != NTP_MODE_SERVER && mode != NTP_MODE_BROADCAST) {
                    throw InvalidServerReplyException("untrusted mode: $mode")
                }
                if (stratum == NTP_STRATUM_DEATH || stratum > NTP_STRATUM_MAX) {
                    throw InvalidServerReplyException("untrusted stratum: $stratum")
                }
                if (transmitTime == 0L) {
                    throw InvalidServerReplyException("zero transmitTime")
                }
            }
        }

        /**
         * This is a two-bit code warning of an impending leap second to be
         * inserted/deleted in the last minute of the current day.  It's values
         * may be as follows:
         *
         * Value     Meaning
         * -----     -------
         * 0         no warning
         * 1         last minute has 61 seconds
         * 2         last minute has 59 seconds)
         * 3         alarm condition (clock not synchronized)
         */
        var leap: Int = 0
            private set


        /**
         * This value indicates the NTP/SNTP version number.  The version number
         * is 3 for Version 3 (IPv4 only) and 4 for Version 4 (IPv4, IPv6 and OSI).
         * If necessary to distinguish between IPv4, IPv6 and OSI, the
         * encapsulating context must be inspected.
         */
        var version: Int = 0
            private set

        /**
         * This value indicates the mode, with values defined as follows:
         *
         * Mode     Meaning
         * ----     -------
         * 0        reserved
         * 1        symmetric active
         * 2        symmetric passive
         * 3        client
         * 4        server
         * 5        broadcast
         * 6        reserved for NTP control message
         * 7        reserved for private use
         *
         * In unicast and anycast modes, the client sets this field to 3 (client)
         * in the request and the server sets it to 4 (server) in the reply. In
         * multicast mode, the server sets this field to 5 (broadcast).
         */
        var mode: Int = 0
            private set

        /**
         * This value indicates the stratum level of the local clock, with values
         * defined as follows:
         *
         * Stratum  Meaning
         * ----------------------------------------------
         * 0        unspecified or unavailable
         * 1        primary reference (e.g., radio clock)
         * 2-15     secondary reference (via NTP or SNTP)
         * 16-255   reserved
         */
        var stratum: Int = 0
            private set

        /**
         * This value indicates the maximum interval between successive messages,
         * in seconds to the nearest power of two. The values that can appear in
         * this field presently range from 4 (16 s) to 14 (16284 s); however, most
         * applications use only the sub-range 6 (64 s) to 10 (1024 s).
         */
        var pollInterval: Int = 0
            private set

        /**
         * This value indicates the precision of the local clock, in seconds to
         * the nearest power of two.  The values that normally appear in this field
         * range from -6 for mains-frequency clocks to -20 for microsecond clocks
         * found in some workstations.
         */
        var precision: Int = 0
            private set

        /**
         * This value indicates the total roundtrip delay to the primary reference
         * source, in seconds.  Note that this variable can take on both positive
         * and negative values, depending on the relative time and frequency
         * offsets. The values that normally appear in this field range from
         * negative values of a few milliseconds to positive values of several
         * hundred milliseconds.
         */
        var rootDelay = 0.0


        /**
         * This value indicates the nominal error relative to the primary reference
         * source, in seconds.  The values that normally appear in this field
         * range from 0 to several hundred milliseconds.
         */
        var rootDispersion = 0.0

        /**
         * This is the time at which the local clock was last set or corrected, in
         * seconds since 00:00 1-Jan-1900.
         */
        var referenceTimestamp: Long = 0L
            private set

        /**
         * This is the time at which the request departed the client for the
         * server, in seconds since 00:00 1-Jan-1900.
         */
        var originateTimestamp: Long = 0L
            private set

        /**
         * This is the time at which the request arrived at the server, in seconds
         * since 00:00 1-Jan-1900.
         */
        var receiveTimestamp: Long = 0L
            private set

        /**
         * This is the time at which the reply departed the server for the client,
         * in seconds since 00:00 1-Jan-1900.
         */
        var transmitTimestamp: Long = 0L
            private set

        /**
         * This is the time at which the client received the NTP server response
         */
        var destinationTimestamp: Long = 0L
            private set


        /**
         * @return time value computed from NTP server response.
         */
        var ntpTime: Long = 0
            private set

        /**
         * Returns the round trip time of the NTP transaction
         *
         * @return round trip time in milliseconds.
         */
        var roundTripDelay: Long = 0
            private set

        var localClockOffset: Long = 0
            private set

        /**
         * adjusted system time (via values from the NTP server) in milliseconds since January 1, 1970
         */
        val now: Long
            get() = System.currentTimeMillis() + this.localClockOffset

        /**
         * Sends an SNTP request to the given host and processes the response.
         *
         * @param host host name of the server.
         * @param timeoutMS network timeout in milliseconds to wait for a response.
         *
         * @return true if the transaction was successful.
         */
        internal fun requestTime(host: String, timeoutMS: Int): SntpClient {
            var socket: DatagramSocket? = null
            var address: InetAddress? = null

            try {
                socket = DatagramSocket()
                socket.soTimeout = timeoutMS
                address = InetAddress.getByName(host)

                val buffer = ByteArray(NTP_PACKET_SIZE)
                val request = DatagramPacket(buffer, buffer.size, address, NTP_PORT)

                // set mode = 3 (client) and version = 3
                // mode is in low 3 bits of first byte
                // version is in bits 3-5 of first byte
                buffer[0] = (NTP_MODE_CLIENT or (NTP_VERSION shl 3)).toByte()


                // don't want random number generation polluting the timing
                val randomNumber = kotlin.random.Random.nextInt(255).toByte()

                // get current time and write it to the request packet
                val timeAtSend = System.currentTimeMillis() // this is UTC from epic
                writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, timeAtSend, randomNumber)

                socket.send(request)

                // read the response
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)

                destinationTimestamp = System.currentTimeMillis() // this is UTC from epic

                // we don't want the socket close operation time to pollute the NTP response timing, so close it afterwards
                socket.close()

                // extract the results

                // See the packet format diagram in RFC 2030 for more information
                leap         = buffer[0].toInt() shr 6 and 0x3
                version      = buffer[0].toInt() shr 3 and 0x7
                mode         = buffer[0].toInt() and 0x7
                stratum      = buffer[1].toInt()
                pollInterval = buffer[2].toInt()
                precision    = buffer[3].toInt()

                rootDelay = (buffer[4] * 256.0) +
                        buffer[5].toInt() +
                        (buffer[6].toInt() / 256.0) +
                        (buffer[7].toInt() / 65536.0)

                rootDispersion = (buffer[8].toInt() * 256.0) +
                        buffer[9].toInt() +
                        (buffer[10].toInt() / 256.0) +
                        (buffer[11].toInt() / 65536.0)


                referenceTimestamp = readTimeStamp(buffer, REFERENCE_TIME_OFFSET)
                originateTimestamp = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET)
                receiveTimestamp = readTimeStamp(buffer, RECEIVE_TIME_OFFSET)
                transmitTimestamp = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET)

                checkValidServerReply(leap, mode, stratum, transmitTimestamp)

                // Formula for delay according to the RFC2030
                //  Timestamp Name          ID   When Generated
                //  ------------------------------------------------------------
                //  Originate Timestamp     T1   time request sent by client
                //  Receive Timestamp       T2   time request received by server
                //  Transmit Timestamp      T3   time reply sent by server
                //  Destination Timestamp   T4   time reply received by client
                //
                //  The roundtrip delay d and local clock offset t are defined as follows:
                //
                //     delay = (T4 - T1) - (T3 - T2)
                //     offset = ((T2 - T1) + (T3 - T4)) / 2

                roundTripDelay = (destinationTimestamp - originateTimestamp) - (transmitTimestamp - receiveTimestamp)
                localClockOffset = ((receiveTimestamp - originateTimestamp) + (transmitTimestamp - destinationTimestamp)) / 2L
            } catch (e: Exception) {
                System.err.println("Error with NTP to $address. $e")
            } finally {
                socket?.close()
            }

            return this
        }

        /**
         * Reads an unsigned 32 bit big endian number from the given offset in the buffer.
         */
        private fun read32(buffer: ByteArray, offset: Int): Long {
            val b0 = buffer[offset].toInt()
            val b1 = buffer[offset + 1].toInt()
            val b2 = buffer[offset + 2].toInt()
            val b3 = buffer[offset + 3].toInt()

            // convert signed bytes to unsigned values
            val i0 = (if (b0 and 0x80 == 0x80) (b0 and 0x7F) + 0x80 else b0).toLong()
            val i1 = (if (b1 and 0x80 == 0x80) (b1 and 0x7F) + 0x80 else b1).toLong()
            val i2 = (if (b2 and 0x80 == 0x80) (b2 and 0x7F) + 0x80 else b2).toLong()
            val i3 = (if (b3 and 0x80 == 0x80) (b3 and 0x7F) + 0x80 else b3).toLong()
            return (i0 shl 24) + (i1 shl 16) + (i2 shl 8) + i3
        }

        /**
         * Reads the NTP time stamp at the given offset in the buffer and returns
         * it as a system time (milliseconds since January 1, 1970).
         */
        private fun readTimeStamp(buffer: ByteArray, offset: Int): Long {
            val seconds = read32(buffer, offset)
            val fraction = read32(buffer, offset + 4)

            // Special case: zero means zero.
            return if (seconds == 0L && fraction == 0L) {
                0
            } else (seconds - OFFSET_1900_TO_1970) * 1000 + fraction * 1000L / 0x100000000L
        }

        /**
         * Writes system time (milliseconds since January 1, 1970) as an NTP time stamp
         * at the given offset in the buffer.
         */
        private fun writeTimeStamp(buffer: ByteArray, @Suppress("SameParameterValue") offset_: Int, time: Long, randomNumber: Byte) {
            var offset = offset_

            if (time == 0L) {
                // Special case: zero means zero.
                Arrays.fill(buffer, offset, offset + 8, 0x00.toByte())
                return
            }

            var seconds: Long = TimeUnit.MILLISECONDS.toSeconds(time)
            val remainingNotSeconds = time - TimeUnit.SECONDS.toMillis(seconds)

            // now apply the required offset (NTP is based on 1900)
            seconds += OFFSET_1900_TO_1970

            val fraction = remainingNotSeconds * 0x100000000L / 1000L

            // write seconds in big endian format
            buffer[offset++] = (seconds shr 24).toByte()
            buffer[offset++] = (seconds shr 16).toByte()
            buffer[offset++] = (seconds shr 8).toByte()
            buffer[offset++] = (seconds shr 0).toByte()

            // write fraction in big endian format
            buffer[offset++] = (fraction shr 24).toByte()
            buffer[offset++] = (fraction shr 16).toByte()
            buffer[offset++] = (fraction shr 8).toByte()

            // From RFC 2030: It is advisable to fill the non-significant
            // low order bits of the timestamp with a random, unbiased
            // bitstring, both to avoid systematic roundoff errors and as
            // a means of loop detection and replay detection.
            buffer[offset] = randomNumber
        }
    }

    /**
     * Sends an SNTP request to the given host and processes the response.
     *
     * @param server host name of the SNTP server.
     * @param timeoutInMS network timeout in milliseconds to wait for a response.
     */
    fun update(server: String, timeoutInMS: Int = 10_000): SntpClient {
        return SntpClient().requestTime(server, timeoutInMS)
    }
}
