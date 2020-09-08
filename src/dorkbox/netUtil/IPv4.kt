/*
 * Copyright 2020 Dorkbox, llc
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package dorkbox.netUtil

import java.io.IOException
import java.io.Writer
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/**
 * A class that holds a number of network-related constants, also from:
 * (Netty, apache 2.0 license)
 * https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/NetUtil.java
 *
 * This class borrowed some of its methods from a modified fork of the
 * [Inet6Util class]
 * (http://svn.apache.org/repos/asf/harmony/enhanced/java/branches/java6/classlib/modules/luni/src/main/java/org/apache/harmony/luni/util/Inet6Util.java) which was part of Apache Harmony.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
object IPv4 {
    /**
     * Returns `true` if IPv4 should be used even if the system supports both IPv4 and IPv6. Setting this
     * property to `true` will disable IPv6 support. The default value of this property is `false`.
     *
     * @see [Java SE networking properties](https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html)
     */
    val isPreferred = Common.getBoolean("java.net.preferIPv4Stack", false)

    /**
     * The [Inet4Address] that represents the IPv4 loopback address '127.0.0.1'
     */
    val LOCALHOST: Inet4Address by lazy {
        // Create IPv4 loopback address.
        // this will ALWAYS work
        InetAddress.getByAddress("localhost", byteArrayOf(127, 0, 0, 1)) as Inet4Address
    }

    /**
     * Windows is unable to work with 0.0.0.0 directly, and if you use LOOPBACK, you might not be able to access the server from another
     * machine.
     *
     * What this does is open a connection to 1.1.1.1 and see get the interface this traffic was on, and use that interface IP address
     */
    val WILDCARD: String by lazy {
        if (Common.OS_WINDOWS) {
            // silly windows can't work with 0.0.0.0, BUT we can't use loopback because we might need to reach this machine from a different host
            // what we do is open a connection to 1.1.1.1 and see what interface this happened on, and this is used as the accessible
            // interface
            var ip = "127.0.0.1"

            runCatching {
                Socket().use {
                    it.connect(InetSocketAddress("1.1.1.1", 80))
                    ip = it.localAddress.hostAddress
                }
            }.onFailure {
                Common.logger.error("Unable to determine outbound traffic local address. Using loopback instead.", it)
            }

            ip
        } else {
            // everyone else works correctly
            "0.0.0.0"
        }
    }

    private val SLASH_REGEX = "\\.".toRegex()


    private val private10 = toInt("10.0.0.0")
    private val private172 = toInt("172.16.0.0")
    private val private192 = toInt("192.168.0.0")
    private val loopback127 = toInt("127.0.0.0")

    /**
     * Determines if this IP address is a private address or not.
     *
     * Private addresses are defined as
     *  10.0.0.0/8
     *  172.16.0.0/12
     *  192.168.0.0/16
     */
    fun isPrivate(ipAsString: String): Boolean {
        return isPrivate(toInt(ipAsString))
    }

    /**
     * Determines if this IP address is a private address or not.
     *
     * Private addresses are defined as
     *  10.0.0.0/8
     *  172.16.0.0/12
     *  192.168.0.0/16
     */
    fun isPrivate(ipAsInt: Int): Boolean {
        // check from smaller to larger
        return isInRange(ipAsInt, private192, 16) || isInRange(ipAsInt, private172, 12) || isInRange(ipAsInt, private10, 8)
    }

    /**
     * A loopback address is defined as
     *  127.0.0.0/8
     */
    fun isLoopback(ipAsString: String): Boolean {
        return isInRange(toInt(ipAsString), loopback127, 8)
    }

    /**
     * A loopback address is defined as
     *  127.0.0.0/8
     */
    fun isLoopback(ipAsInt: Int): Boolean {
        return isInRange(ipAsInt, loopback127, 8)
    }

    /**
     * Determine whether a given string is a valid CIDR IP address. Accepts only 1.2.3.4/24
     *
     * @param ipAsString The string that will be checked.
     *
     * @return return true if the string is a valid IP address, false if it is not.
     */
    fun isValidCidr(ipAsString: String): Boolean {
        if (ipAsString.isEmpty()) {
            return false
        }

        val slashIndex = ipAsString.indexOf('/')
        if (slashIndex < 6) {
            // something is malformed.
            return false
        }

        val ipOnly = ipAsString.substring(0, slashIndex)
        if (!isValid(ipOnly)) {
            return false
        }

        try {
            val cidr = ipAsString.substring(slashIndex + 1).toInt()
            if (cidr in 0..32) {
                return true
            }
        } catch (ignored: Exception) {
        }

        return false
    }

    /**
     * Takes a [String] and parses it to see if it is a valid IPV4 address.
     *
     * @return true, if the string represents an IPV4 address in dotted notation, false otherwise
     */
    fun isValid(ip: String): Boolean {
        return isValidIpV4Address(ip, 0, ip.length)
    }

    internal fun isValidIpV4Address(ip: String, from: Int, toExcluded: Int): Boolean {
        val len = toExcluded - from
        if (len !in 7..15) {
            return false
        }

        @Suppress("NAME_SHADOWING")
        var from = from
        var i = ip.indexOf('.', from)

        if (i <= 0 || !isValidIpV4Word(ip, from, i)) {
            return false
        }

        from = i + 1
        i = ip.indexOf('.', from)
        if (i <= 0 || !isValidIpV4Word(ip, from, i)) {
            return false
        }

        from = i + 1
        i = ip.indexOf('.', from)
        if (i <= 0 || !isValidIpV4Word(ip, from, i)) {
            return false
        }

        if (i <= 0 || !isValidIpV4Word(ip, i + 1, toExcluded)) {
            return false
        }

        return true
    }

    private fun isValidIpV4Word(word: CharSequence, from: Int, toExclusive: Int): Boolean {
        val len = toExclusive - from
        var c0 = ' '
        var c1: Char
        var c2 = ' '

        if (len < 1 || len > 3 || word[from].also { c0 = it } < '0') {
            return false
        }
        return if (len == 3) {
            word[from + 1].also { c1 = it } >= '0'
            && word[from + 2].also { c2 = it } >= '0'
            && (c0 <= '1' && c1 <= '9' && c2 <= '9' || c0 == '2' && c1 <= '5' && (c2 <= '5' || c1 < '5' && c2 <= '9'))
        }
        else c0 <= '9' && (len == 1 || isValidNumericChar(word[from + 1]))
    }

    internal fun isValidNumericChar(c: Char): Boolean {
        return c in '0'..'9'
    }

    internal fun isValidIPv4MappedChar(c: Char): Boolean {
        return c == 'f' || c == 'F'
    }


    fun toBytesorNull(ip: String): ByteArray? {
        if (!isValid(ip)) {
            return null
        }

        var i: Int
        return byteArrayOf(ipv4WordToByte(ip, 0, ip.indexOf('.', 1).also { i = it }),
                           ipv4WordToByte(ip, i + 1, ip.indexOf('.', i + 2).also { i = it }),
                           ipv4WordToByte(ip, i + 1, ip.indexOf('.', i + 2).also { i = it }),
                           ipv4WordToByte(ip, i + 1, ip.length))
    }

    fun toBytes(ip: String): ByteArray {
        var i: Int

        var index = ip.indexOf('.', 1)
        val a = ipv4WordToByte(ip, 0, index)
        i = index

        index = ip.indexOf('.', index + 2)
        val b = ipv4WordToByte(ip, i + 1, index)
        i = index

        index = ip.indexOf('.', index + 2)
        val c = ipv4WordToByte(ip, i + 1, index)
        i = index

        return byteArrayOf(a, b, c, ipv4WordToByte(ip, i + 1, ip.length))
    }

    private fun decimalDigit(str: CharSequence, pos: Int): Int {
        return str[pos] - '0'
    }

    private fun ipv4WordToByte(ip: CharSequence, from: Int, toExclusive: Int): Byte {
        var newFrom = from
        var ret = decimalDigit(ip, newFrom)
        newFrom++

        if (newFrom == toExclusive) {
            return ret.toByte()
        }

        ret = ret * 10 + decimalDigit(ip, newFrom)
        newFrom++

        return if (newFrom == toExclusive) {
            ret.toByte()
        } else (ret * 10 + decimalDigit(ip, newFrom)).toByte()
    }

    fun findFreeSubnet24(): ByteArray? {
        Common.logger.info("Scanning for available cidr...")

        // have to find a free cidr
        // start with 10.x.x.x /24 and just march through starting at 0 -> 200 for each, ping to see if there is a gateway (should be)
        // and go from there.


        // on linux, PING has the setuid bit set - so it runs "as root". isReachable() requires either java to have the setuid bit set
        // (ie: sudo setcap cap_net_raw=ep /usr/lib/jvm/jdk/bin/java) or it requires to be run as root. We run as root in production, so it
        // works.
        val ip = byteArrayOf(10, 0, 0, 0)
        var subnet24Counter = 0

        while (true) {
            ip[3]++

            if (ip[3] > 255) {
                ip[3] = 1
                ip[2]++
                subnet24Counter = 0
            }
            if (ip[2] > 255) {
                ip[2] = 0
                ip[1]++
            }
            if (ip[1] > 255) {
                Common.logger.error("Exhausted all ip searches. FATAL ERROR.")
                return null
            }

            try {
                val address = InetAddress.getByAddress(ip)
                val reachable = address.isReachable(100)

                if (!reachable) {
                    subnet24Counter++
                }
                if (subnet24Counter == 250) {
                    // this means that we tried all /24 IPs, and ALL of them came back an "non-responsive". 100ms timeout is OK, because
                    // we are on a LAN, that should have MORE than one IP in the cidr, and it should be fairly responsive (ie: <10ms ping)

                    // we found an empty cidr
                    ip[3] = 1
                    return ip
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }
    }

    /**
     * Scans for existing IP addresses on the network.
     *
     * @param startingIp the IP address to start scanning at
     * @param numberOfHosts the number of hosts to scan for. A /28 is 14 hosts : 2^(32-28) - 2 = 14
     *
     * @return true if no hosts were reachable (pingable)
     */
    fun scanHosts(startingIp: String, numberOfHosts: Int): Boolean {
        Common.logger.info("Scanning {} hosts, starting at IP {}.", numberOfHosts, startingIp)


        val split = startingIp.split(SLASH_REGEX).toTypedArray()
        val a = split[0].toByte()
        val b = split[1].toByte()
        val c = split[2].toByte()
        val d = split[3].toByte()

        val ip = byteArrayOf(a, b, c, d)
        var counter = numberOfHosts

        while (counter >= 0) {
            counter--

            ip[3]++
            if (ip[3] > 255) {
                ip[3] = 1
                ip[2]++
            }
            if (ip[2] > 255) {
                ip[2] = 0
                ip[1]++
            }
            if (ip[1] > 255) {
                Common.logger.error("Exhausted all ip searches. FATAL ERROR.")
                return false
            }

            try {
                val address = InetAddress.getByAddress(ip)
                val reachable = address.isReachable(100)
                if (reachable) {
                    Common.logger.error("IP address {} is already reachable on the network. Unable to continue.", address.hostAddress)
                    return false
                }
            } catch (e: IOException) {
                Common.logger.error("Error pinging the IP address", e)
                return false
            }
        }
        return true
    }

    /**
     * @param cidr the CIDR notation, ie: 24, 16, etc. That we want to convert into a netmask, as a string
     *
     * @return the netmask or if there were errors, the default /0 netmask
     */
    fun getCidrAsNetmask(cidr: Int): String {
        return when (cidr) {
            32 -> "255.255.255.255"
            31 -> "255.255.255.254"
            30 -> "255.255.255.252"
            29 -> "255.255.255.248"
            28 -> "255.255.255.240"
            27 -> "255.255.255.224"
            26 -> "255.255.255.192"
            25 -> "255.255.255.128"
            24 -> "255.255.255.0"
            23 -> "255.255.254.0"
            22 -> "255.255.252.0"
            21 -> "255.255.248.0"
            20 -> "255.255.240.0"
            19 -> "255.255.224.0"
            18 -> "255.255.192.0"
            17 -> "255.255.128.0"
            16 -> "255.255.0.0"
            15 -> "255.254.0.0"
            14 -> "255.252.0.0"
            13 -> "255.248.0.0"
            12 -> "255.240.0.0"
            11 -> "255.224.0.0"
            10 -> "255.192.0.0"
            9 -> "255.128.0.0"
            8 -> "255.0.0.0"
            7 -> "254.0.0.0"
            6 -> "252.0.0.0"
            5 -> "248.0.0.0"
            4 -> "240.0.0.0"
            3 -> "224.0.0.0"
            2 -> "192.0.0.0"
            1 -> "128.0.0.0"
            else -> "0.0.0.0"
        }
    }

    /**
     * @param cidr the CIDR notation, ie: 24, 16, etc. That we want to convert into a netmask, as a SIGNED INTEGER (the bits are still
     * correct, but to see this "as unix would", you must convert to an unsigned integer.
     *
     * @return the netmask (as a signed int), or if there were errors, the default /0 netmask
     */
    fun getCidrAsIntNetmask(cidr: Int): Int {
        return when (cidr) {
            32 -> -1
            31 -> -2
            30 -> -4
            29 -> -8
            28 -> -16
            27 -> -32
            26 -> -64
            25 -> -128
            24 -> -256
            23 -> -512
            22 -> -1024
            21 -> -2048
            20 -> -4096
            19 -> -8192
            18 -> -16384
            17 -> -32768
            16 -> -65536
            15 -> -131072
            14 -> -262144
            13 -> -524288
            12 -> -1048576
            11 -> -2097152
            10 -> -4194304
            9 -> -8388608
            8 -> -16777216
            7 -> -33554432
            6 -> -67108864
            5 -> -134217728
            4 -> -268435456
            3 -> -536870912
            2 -> -1073741824
            1 -> -2147483648
            else -> 0
        }
    }

    fun getCidrFromMask(mask: String): Int {
        return when (mask) {
            "255.255.255.255" -> 32
            "255.255.255.254" -> 31
            "255.255.255.252" -> 30
            "255.255.255.248" -> 29
            "255.255.255.240" -> 28
            "255.255.255.224" -> 27
            "255.255.255.192" -> 26
            "255.255.255.128" -> 25
            "255.255.255.0" -> 24
            "255.255.254.0" -> 23
            "255.255.252.0" -> 22
            "255.255.248.0" -> 21
            "255.255.240.0" -> 20
            "255.255.224.0" -> 19
            "255.255.192.0" -> 18
            "255.255.128.0" -> 17
            "255.255.0.0" -> 16
            "255.254.0.0" -> 15
            "255.252.0.0" -> 14
            "255.248.0.0" -> 13
            "255.240.0.0" -> 12
            "255.224.0.0" -> 11
            "255.192.0.0" -> 10
            "255.128.0.0" -> 9
            "255.0.0.0" -> 8
            "254.0.0.0" -> 7
            "252.0.0.0" -> 6
            "248.0.0.0" -> 5
            "240.0.0.0" -> 4
            "224.0.0.0" -> 3
            "192.0.0.0" -> 2
            "128.0.0.0" -> 1
            else -> 0
        }
    }

    private val CIDR2MASK = intArrayOf(0x00000000,
                                       -0x80000000,
                                       -0x40000000,
                                       -0x20000000,
                                       -0x10000000,
                                       -0x8000000,
                                       -0x4000000,
                                       -0x2000000,
                                       -0x1000000,
                                       -0x800000,
                                       -0x400000,
                                       -0x200000,
                                       -0x100000,
                                       -0x80000,
                                       -0x40000,
                                       -0x20000,
                                       -0x10000,
                                       -0x8000,
                                       -0x4000,
                                       -0x2000,
                                       -0x1000,
                                       -0x800,
                                       -0x400,
                                       -0x200,
                                       -0x100,
                                       -0x80,
                                       -0x40,
                                       -0x20,
                                       -0x10,
                                       -0x8,
                                       -0x4,
                                       -0x2,
                                       -0x1)

    fun range2Cidr(startIp: String, endIp: String): List<String> {
        var start = toInt(startIp).toLong()
        val end = toInt(endIp).toLong()

        val pairs: MutableList<String> = ArrayList()

        while (end >= start) {
            var maxsize = 32.toByte()
            while (maxsize > 0) {
                val mask = CIDR2MASK[maxsize - 1].toLong()
                val maskedBase = start and mask
                if (maskedBase != start) {
                    break
                }
                maxsize--
            }

            val x = ln(end - start + 1.toDouble()) / ln(2.0)
            val maxDiff = (32 - floor(x)).toInt().toByte()
            if (maxsize < maxDiff) {
                maxsize = maxDiff
            }

            val ip = toString(start)
            pairs.add("$ip/$maxsize")
            start += 2.0.pow(32 - maxsize.toDouble()).toLong()
        }
        return pairs
    }

    /**
     * Check if the IP address is in the range of a specific IP/CIDR
     *
     * a prefix of 0 will ALWAYS return true
     *
     * @param address the address to check
     * @param networkAddress the network address that will have the other address checked against
     * @param networkPrefix 0-32 the network prefix (subnet) to use for the network address
     *
     * @return true if it is in range
     */
    fun isInRange(address: String, networkAddress: String, networkPrefix: Int): Boolean {
        return isInRange(toInt(address), toInt(networkAddress), networkPrefix)
    }

    /**
     * Check if the IP address is in the range of a specific IP/CIDR
     *
     * a prefix of 0 will ALWAYS return true
     *
     * @param address the address to check
     * @param networkAddress the network address that will have the other address checked against
     * @param networkPrefix 0-32 the network prefix (subnet) to use for the network address
     *
     * @return true if it is in range
     */
    fun isInRange(address: Int, networkAddress: Int, networkPrefix: Int): Boolean {
        // System.err.println(" ip: " + IP.toString(address));
        // System.err.println(" networkAddress: " + IP.toString(networkAddress));
        // System.err.println(" networkSubnetPrefix: " + networkPrefix);
        if (networkPrefix == 0) {
            // a prefix of 0 means it is always true (even though the broadcast address is '-1'). So we short-cut it here
            return true
        }
        val netmask = ((1 shl 32 - networkPrefix) - 1).inv()

        // Calculate base network address
        val network = (networkAddress and netmask).toUInt()
//        System.err.println("   network " + IPv4.toString(network.toInt()))

        //  Calculate broadcast address
        val broadcast = network or netmask.inv().toUInt()
//        System.err.println("   broadcast " + IPv4.toString(broadcast.toInt()))

//        val addressLong = (address.toLong() and UNSIGNED_INT_MASK)
        return address.toUInt() in network..broadcast
    }


    fun toInt(ipBytes: ByteArray): Int {
        return ipBytes[0].toInt() shl 24 or
                (ipBytes[1].toInt() shl 16) or
                (ipBytes[2].toInt() shl 8) or
                (ipBytes[3].toInt())
    }

    /**
     * Converts a 32-bit integer into an IPv4 address.
     */
    fun toString(ipAddress: Int): String {
        val buf = StringBuilder(15)
        buf.append(ipAddress shr 24 and 0xFF)
        buf.append('.')
        buf.append(ipAddress shr 16 and 0xFF)
        buf.append('.')
        buf.append(ipAddress shr 8 and 0xFF)
        buf.append('.')
        buf.append(ipAddress and 0xFF)
        return buf.toString()
    }

    fun toString(ipBytes: ByteArray): String {
        val buf = StringBuilder(15)
        buf.append(ipBytes[0].toInt() and 0xFF)
        buf.append('.')
        buf.append(ipBytes[1].toInt() and 0xFF)
        buf.append('.')
        buf.append(ipBytes[2].toInt() and 0xFF)
        buf.append('.')
        buf.append(ipBytes[3].toInt() and 0xFF)
        return buf.toString()
    }

    /**
     * Returns the [String] representation of an [InetAddress]. Results are identical to [InetAddress.getHostAddress]
     *
     * @param ip [InetAddress] to be converted to an address string
     *
     * @return `String` containing the text-formatted IP address
     */
    fun toString(ip: InetAddress): String {
        return (ip as Inet4Address).hostAddress
    }


    @Throws(Exception::class)
    fun writeString(ipAddress: Int, writer: Writer) {
        writer.write((ipAddress shr 24 and 0xFF).toString())
        writer.write('.'.toInt())
        writer.write((ipAddress shr 16 and 0xFF).toString())
        writer.write('.'.toInt())
        writer.write((ipAddress shr 8 and 0xFF).toString())
        writer.write('.'.toInt())
        writer.write((ipAddress and 0xFF).toString())
    }

    fun toString(ipAddress: Long): String {
        val ipString = StringBuilder(15)
        ipString.append(ipAddress shr 24 and 0xFF)
        ipString.append('.')
        ipString.append(ipAddress shr 16 and 0xFF)
        ipString.append('.')
        ipString.append(ipAddress shr 8 and 0xFF)
        ipString.append('.')
        ipString.append(ipAddress and 0xFF)
        return ipString.toString()
    }

    fun toBytes(bytes: Int): ByteArray {
        return byteArrayOf((bytes shr 24 and 0xFF).toByte(),
                           (bytes shr 16 and 0xFF).toByte(),
                           (bytes shr 8 and 0xFF).toByte(),
                           (bytes and 0xFF).toByte())
    }

    fun toInt(ipAsString: String): Int {
        return if (isValid(ipAsString)) {
            toIntUnsafe(ipAsString)
        } else {
            0
        }
    }

    fun toIntUnsafe(ipAsString: String): Int {
        val bytes = toBytes(ipAsString)

        var address = 0
        for (element in bytes) {
            address = address shl 8
            address = address or (element.toInt() and 0xff)
        }

        return address
    }

    /**
     * Returns the [Inet4Address] representation of a [String] IP address.
     *
     * This method will treat all IPv4 type addresses as "IPv4 mapped" (see [.getByName])
     *
     * @param ip [String] IP address to be converted to a [Inet4Address]
     * @return [Inet4Address] representation of the `ip` or `null` if not a valid IP address.
     */
    fun getByNameUnsafe(ip: String): Inet4Address {
        val asBytes = toBytes(ip)
        return Inet4Address.getByAddress(ip, asBytes) as Inet4Address
    }

    /**
     * Returns the [Inet4Address] representation of a [String] IP address.
     *
     * This method will treat all IPv4 type addresses as "IPv4 mapped" (see [.getByName])
     *
     * @param ip [String] IP address to be converted to a [Inet4Address]
     * @return [Inet4Address] representation of the `ip` or `null` if not a valid IP address.
     */
    fun getByName(ip: String): Inet4Address? {
        return if (isValid(ip)) {
            return getByNameUnsafe(ip)
        } else {
            null
        }
    }
}
