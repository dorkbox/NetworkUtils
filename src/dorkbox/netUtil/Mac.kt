@file:Suppress("unused")

package dorkbox.netUtil

import dorkbox.executor.Executor
import mu.KLogger
import java.io.File
import java.io.Writer
import java.math.BigInteger
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.regex.Pattern

/**
 *
 */
object Mac {
    enum class MacDelimiter(val delimiter: String) {
        COLON(":"),
        PERIOD("."),
        SPACE(" ");

        val regex = delimiter.toRegex()
    }

    private const val MAC_ADDRESS_LENGTH = 6

    private val random = Random()
    private val fakeMacsInUse: MutableSet<String> = HashSet()

    private val MAC_ADDRESS_PATTERN by lazy {
        Pattern.compile("^([a-fA-F0-9]{2}[:\\\\.-]?){5}[a-fA-F0-9]{2}$")
    }

    private val COLON_REGEX = ":".toRegex()

    init {
        synchronized(fakeMacsInUse) {}
    }

    fun getMacAddress(ip: String, logger: KLogger? = null): String {
        try {
            val mac = getMacAddressByte(ip, logger)
            if (mac == null) {
                logger?.error("Unable to get MAC address for IP '{}'", ip)
                return ""
            }

            val s = StringBuilder(18)
            for (b in mac) {
                if (s.isNotEmpty()) {
                    s.append(':')
                }
                s.append(String.format("%02x", b))
            }
            return s.toString()
        } catch (e: Exception) {
            if (logger != null) {
                logger.error("Unable to get MAC address for IP '{}'", ip, e)
            }
            else {
                e.printStackTrace()
            }
        }

        logger?.error("Unable to get MAC address for IP '{}'", ip)

        return ""
    }

    fun getMacAddressByte(ip: String, logger: KLogger? = null): ByteArray? {
        try {
            val addr = InetAddress.getByName(ip)
            val networkInterface = NetworkInterface.getByInetAddress(addr)
            if (networkInterface == null) {
                logger?.error("Unable to get MAC address for IP '{}'", ip)
                return null
            }

            val mac = networkInterface.hardwareAddress
            if (mac == null) {
                logger?.error("Unable to get MAC address for IP '{}'", ip)
                return null
            }

            return mac
        } catch (e: Exception) {
            if (logger != null) {
                logger.error("Unable to get MAC address for IP '{}'", ip, e)
            }
            else {
                e.printStackTrace()
            }
        }

        return null
    }

    /**
     * Removes a mac that was in use, to free it's use later on
     */
    fun freeFakeMac(fakeMac: String) {
        synchronized(fakeMacsInUse) { fakeMacsInUse.remove(fakeMac) }
    }

    /**
     * Removes a mac that was in use, to free it's use later on
     */
    fun freeFakeMac(vararg fakeMacs: String) {
        synchronized(fakeMacsInUse) {
            fakeMacs.forEach {
                fakeMacsInUse.remove(it)
            }
        }
    }

    /**
     * Removes a mac that was in use, to free it's use later on
     */
    fun freeFakeMac(fakeMacs: Iterable<String>) {
        synchronized(fakeMacsInUse) {
            fakeMacs.forEach {
                fakeMacsInUse.remove(it)
            }
        }
    }

    /**
     * will also make sure that this mac doesn't already exist
     *
     * @return a unique MAC that can be used for VPN devices + setup. This is a LOWERCASE string!
     */
    fun fakeMac(): String {
        synchronized(fakeMacsInUse) {
            var mac = fakeVpnMacUnsafe()

            // gotta make sure it doesn't already exist
            while (fakeMacsInUse.contains(mac)) {
                mac = fakeVpnMacUnsafe()
            }

            fakeMacsInUse.add(mac)
            return mac
        }
    }

    /**
     * will also make sure that this mac doesn't already exist
     *
     * @return a unique MAC that can be used for VPN devices + setup. This is a LOWERCASE string!
     */
    fun fakeDockerMac(): String {
        synchronized(fakeMacsInUse) {
            var mac = fakeDockerMacUnsafe()

            // gotta make sure it doesn't already exist
            while (fakeMacsInUse.contains(mac)) {
                mac = fakeDockerMacUnsafe()
            }
            fakeMacsInUse.add(mac)

            return mac
        }
    }

    /**
     * http://serverfault.com/questions/40712/what-range-of-mac-addresses-can-i-safely-use-for-my-virtual-machines
     *
     * @return a mac that is safe to use for fake interfaces. THIS IS LOWERCASE!
     */
    fun fakeVpnMac(): String {
        var mac = fakeVpnMacUnsafe()

        synchronized(fakeMacsInUse) {
            // gotta make sure it doesn't already exist
            while (fakeMacsInUse.contains(mac)) {
                mac = fakeVpnMacUnsafe()
            }
            fakeMacsInUse.add(mac)
        }

        return mac
    }

    /**
     * http://serverfault.com/questions/40712/what-range-of-mac-addresses-can-i-safely-use-for-my-virtual-machines
     *
     * @return a mac that is safe to use for fake interfaces. THIS IS LOWERCASE!
     */
    fun fakeVpnMacUnsafe(): String {
        var vpnID = randHex
        while (vpnID == "d0") {
            // this is what is used for docker. NEVER assign it to a VPN mac!!
            vpnID = randHex
        }

        return "02:$vpnID:$randHex:$randHex:$randHex:$randHex"
    }

    /**
     * http://serverfault.com/questions/40712/what-range-of-mac-addresses-can-i-safely-use-for-my-virtual-machines
     *
     * @return a mac that is safe to use for fake interfaces. THIS IS LOWERCASE!
     */
    fun fakeDockerMacUnsafe(): String {
        return "02:d0:$randHex:$randHex:$randHex:$randHex"
    }

    /**
     * will also make sure that this mac doesn't already exist
     *
     * @return a unique MAC that can be used for VPN devices + setup
     */
    fun fakeVpnMacWithDir(vpnKeyDirForExistingVpnMacs: String): String {
        synchronized(fakeMacsInUse) {
            var mac = fakeVpnMacUnsafe()

            // gotta make sure it doesn't already exist
            while (fakeMacsInUse.contains(mac) || File(vpnKeyDirForExistingVpnMacs, "$mac.crt").exists()) {
                mac = fakeVpnMacUnsafe()
            }

            fakeMacsInUse.add(mac)
            return mac
        }
    }

    private val randHex: String
        get() {
            val i = random.nextInt(255)

            return if (i < 16) {
                "0" + Integer.toHexString(i)
            }
            else {
                Integer.toHexString(i)
            }
        }

    /**
     * Converts a long into a properly formatted lower-case string
     */
    fun toStringLowerCase(mac: Long): String {
        val macBytes = toBytes(mac)

        // mac should have AT LEAST bytes.
        val buf = StringBuilder()

        // we only use the right-most 6 bytes (of 8 bytes).
        (2..7).forEach { index ->
            val byte = macBytes[index]
            if (buf.isNotEmpty()) {
                buf.append(':')
            }

            if (byte in 0..15) {
                buf.append('0')
            }

            @Suppress("EXPERIMENTAL_API_USAGE")
            buf.append(Integer.toHexString(byte.toUByte().toInt()))
        }

        return buf.toString()
    }

    @Throws(Exception::class)
    fun writeStringLowerCase(mac: Long, writer: Writer) {
        // we only use the right-most 6 bytes.
        val macBytes = toBytes(mac)

        // we only use the right-most 6 bytes (of 8 bytes).
        var bytesWritten = 0
        (2..7).forEach { index ->
            val byte = macBytes[index]

            if (bytesWritten != 0) {
                writer.write(":")
                bytesWritten++
            }

            if (byte in 0..15) {
                writer.append('0')
            }

            writer.write(Integer.toHexString(byte.toInt() and 0xFF))
            bytesWritten += 2
        }
    }

    private fun toBytes(x: Long): ByteArray {
        return byteArrayOf((x shr 56).toByte(),
                           (x shr 48).toByte(),
                           (x shr 40).toByte(),
                           (x shr 32).toByte(),
                           (x shr 24).toByte(),
                           (x shr 16).toByte(),
                           (x shr 8).toByte(),
                           (x shr 0).toByte())
    }


    fun toStringLowerCase(mac: ByteArray): String {
        // mac should have 6 bytes.
        val buf = StringBuilder()
        for (b in mac) {
            if (buf.isNotEmpty()) {
                buf.append(':')
            }
            if (b in 0..15) {
                buf.append('0')
            }

            buf.append(Integer.toHexString(if (b < 0) b + 256 else b.toInt()))
        }
        return buf.toString()
    }

    fun toStringUpperCase(mac: ByteArray): String {
        val buf = StringBuilder()
        for (b in mac) {
            if (buf.isNotEmpty()) {
                buf.append(':')
            }
            if (b in 0..15) {
                buf.append('0')
            }

            buf.append(Integer.toHexString(b.toInt() and 0xFF)
                           .toUpperCase())
        }
        return buf.toString()
    }

    fun toBytes(mac: String): ByteArray {
        val s = mac.replace(COLON_REGEX, "")
        return BigInteger(s, 16).toByteArray()
    }


    fun toLongSafe(macAsString: String): Long {
        return toLong(macAsString, getMacDelimiter(macAsString))
    }

    fun toLong(mac: String): Long {
        return toLong(mac, MacDelimiter.COLON)
    }

    fun toLong(mac: ByteArray): Long {
        return ((mac[5].toLong() and 0xff) +
                (mac[4].toLong() and 0xff shl 8) +
                (mac[3].toLong() and 0xff shl 16) +
                (mac[2].toLong() and 0xff shl 24) +
                (mac[1].toLong() and 0xff shl 32) +
                (mac[0].toLong() and 0xff shl 40))
    }

    fun toLong(mac: String, delimiter: MacDelimiter? = MacDelimiter.COLON): Long {
        val addressInBytes = ByteArray(MAC_ADDRESS_LENGTH)
        try {
            val elements: Array<String?>

            if (delimiter != null) {
                elements = mac.split(delimiter.regex).toTypedArray()
            }
            else {
                elements = arrayOfNulls(MAC_ADDRESS_LENGTH)
                var index = 0
                var substringPos = 0
                while (index < MAC_ADDRESS_LENGTH) {
                    elements[index] = mac.substring(substringPos, substringPos + 2)
                    index++
                    substringPos += 2
                }
            }

            for (i in 0 until MAC_ADDRESS_LENGTH) {
                val element = elements[i]
                addressInBytes[i] = element!!.toInt(16).toByte()
            }
        } catch (e: Exception) {
            Common.logger.error("Error parsing MAC address '{}'", mac, e)
        }

        return toLong(addressInBytes)
    }

    fun isValid(macAsString: String): Boolean {
        if (macAsString.isEmpty()) {
            return false
        }

        // Check whether mac is separated by a colon, period, space, or nothing at all.
        // Must standardize to a colon.
        var normalizedMac: String = macAsString
        if (normalizedMac.split(COLON_REGEX)
                    .toTypedArray().size != 6) {

            // Does not already use colons, must modify the mac to use colons.
            when {
                normalizedMac.contains(".") -> {
                    // Period notation
                    normalizedMac = normalizedMac.replace(".", ":")
                }
                normalizedMac.contains(" ") -> {
                    // Space notation
                    normalizedMac = normalizedMac.replace(" ", ":")
                }
                else -> {
                    // No delimiter, manually add colons.
                    normalizedMac = ""
                    var index = 0
                    var substringPos = 0

                    // We ensure that the substring function will not exceed the length of the given string if the string is not at least
                    //(MAC_ADDRESS_LENGTH * 2) characters long.
                    while (index < MAC_ADDRESS_LENGTH && macAsString.length >= substringPos + 2) {
                        // Reconstruct the string, adding colons.
                        normalizedMac = if (index != MAC_ADDRESS_LENGTH - 1) {
                            normalizedMac + macAsString.substring(substringPos, substringPos + 2) + ":"
                        }
                        else {
                            normalizedMac + macAsString.substring(substringPos, substringPos + 2)
                        }

                        index++
                        substringPos += 2
                    }
                }
            }
        }
        val matcher = MAC_ADDRESS_PATTERN.matcher(normalizedMac)
        return matcher.matches()
    }

    /**
     * Returns the type of delmiter based on how a mac address is constructed. Returns ":" if no delimiter could be detected.
     *
     * @return a MacDelimiter if there is one, or null if the mac address is one long string.
     */
    fun getMacDelimiter(macAsString: String): MacDelimiter? {
        return when {
            macAsString.contains(":") -> {
                MacDelimiter.COLON
            }
            macAsString.contains(".") -> {
                MacDelimiter.PERIOD
            }
            macAsString.contains(" ") -> {
                MacDelimiter.SPACE
            }
            else -> MacDelimiter.COLON
        }
    }
    fun assign(interfaceName: String, macAddress: String) {
        if (Common.OS_LINUX) {
            Executor.run("/sbin/ifconfig", interfaceName, "hw", "ether", macAddress)
        }
        else {
            throw RuntimeException("NOT IMPL.")
        }
    }
}
