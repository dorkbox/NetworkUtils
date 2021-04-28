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
import java.net.*
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
     * Gets the version number.
     */
    const val version = "2.7"

    /**
     * Returns `true` if IPv4 should be used even if the system supports both IPv4 and IPv6. Setting this
     * property to `true` will disable IPv6 support. The default value of this property is `false`.
     *
     * @see [Java SE networking properties](https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html)
     */
    val isPreferred = Common.getBoolean("java.net.preferIPv4Stack", false)

    /**
     * true if there is an IPv4 network interface available
     */
    val isAvailable: Boolean by lazy {
        // Retrieve the list of available network interfaces.
        val netInterfaces = mutableListOf<NetworkInterface>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    val iface: NetworkInterface = interfaces.nextElement()
                    // Use the interface with proper INET addresses only.
                    if (SocketUtils.addressesFromNetworkInterface(iface).hasMoreElements()) {
                        netInterfaces.add(iface)
                    }
                }
            }
        } catch (e: SocketException) {
            Common.logger.warn("Failed to retrieve the list of available network interfaces", e)
        }

        // check if IPv4 is possible.
        var ipv4Possible = false
        loop@ for (iface in netInterfaces) {
            val i = SocketUtils.addressesFromNetworkInterface(iface)
            while (i.hasMoreElements()) {
                val addr: InetAddress = i.nextElement()
                if (addr is Inet4Address) {
                    ipv4Possible = true
                    break@loop
                }
            }
        }

        ipv4Possible
    }


    /**
     * The [Inet4Address] that represents the IPv4 loopback address '127.0.0.1'
     */
    val LOCALHOST: Inet4Address by lazy {
        // Create IPv4 address, this will ALWAYS work
        InetAddress.getByAddress("localhost", byteArrayOf(127, 0, 0, 1)) as Inet4Address
    }

    /**
     * The [Inet4Address] that represents the IPv4 wildcard address '0.0.0.0'
     */
    val WILDCARD: Inet4Address by lazy {
        // Create IPv4 address, this will ALWAYS work
        InetAddress.getByAddress(null, byteArrayOf(0, 0, 0, 0)) as Inet4Address
    }

    /**
     * the length of an address in this particular family.
     */
    val length = 4

    private val SLASH_REGEX = "\\.".toRegex()

    private val private10 = toInt("10.0.0.0")
    private val private172 = toInt("172.16.0.0")
    private val private192 = toInt("192.168.0.0")
    private val loopback127 = toInt("127.0.0.0")

    /**
     * @return if the specified address is of this family type
     */
    fun isFamily(address: InetAddress) : Boolean {
        return address is Inet4Address
    }

    /**
     * Determines if this IP address is a private address or not.
     *
     * Private addresses are defined as
     *  10.0.0.0/8
     *  172.16.0.0/12
     *  192.168.0.0/16
     */
    fun isSiteLocal(ipAsString: String): Boolean {
        return isSiteLocal(toInt(ipAsString))
    }

    /**
     * Determines if this IP address is a private address or not.
     *
     * Private addresses are defined as
     *  10.0.0.0/8
     *  172.16.0.0/12
     *  192.168.0.0/16
     */
    fun isSiteLocal(ipAsInt: Int): Boolean {
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


    fun toBytesOrNull(ip: String): ByteArray? {
        if (!isValid(ip)) {
            return null
        }

        var i: Int
        return byteArrayOf(ipv4WordToByte(ip, 0, ip.indexOf('.', 1).also { i = it }),
                           ipv4WordToByte(ip, i + 1, ip.indexOf('.', i + 2).also { i = it }),
                           ipv4WordToByte(ip, i + 1, ip.indexOf('.', i + 2).also { i = it }),
                           ipv4WordToByte(ip, i + 1, ip.length))
    }

    /**
     * Creates an byte[] based on an ip Address String. No error handling is performed here.
     */
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

    /**
     * Creates an int[] based on an ip address String. No error handling is performed here.
     */
    fun toInts(ip: String): IntArray {
        var i: Int

        var index = ip.indexOf('.', 1)
        val a = ipv4WordToInt(ip, 0, index)
        i = index

        index = ip.indexOf('.', index + 2)
        val b = ipv4WordToInt(ip, i + 1, index)
        i = index

        index = ip.indexOf('.', index + 2)
        val c = ipv4WordToInt(ip, i + 1, index)
        i = index

        return intArrayOf(a, b, c, ipv4WordToInt(ip, i + 1, ip.length))
    }

    private fun decimalDigit(str: CharSequence, pos: Int): Int {
        return str[pos] - '0'
    }

    private fun ipv4WordToByte(ip: CharSequence, from: Int, toExclusive: Int): Byte {
        return ipv4WordToInt(ip, from, toExclusive).toByte()
    }

    private fun ipv4WordToInt(ip: CharSequence, from: Int, toExclusive: Int): Int {
        var newFrom = from

        var ret = decimalDigit(ip, newFrom)
        newFrom++

        if (newFrom == toExclusive) {
            return ret
        }

        ret = ret * 10 + decimalDigit(ip, newFrom)
        newFrom++

        return if (newFrom == toExclusive) {
            ret
        } else {
            ret * 10 + decimalDigit(ip, newFrom)
        }
    }

    /**
     * Finds the first available subnet (as CIDR) with a corresponding gateway
     */
    fun findFreeSubnet24Cidr(): ByteArray? {
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
     * @param cidrPrefix the CIDR notation, ie: /24, /16, etc that we want to convert into a netmask
     *
     * @return the netmask (as a signed int), or if there were errors, the default /0 netmask
     */
    fun cidrPrefixToSubnetMask(cidrPrefix: Int): Int {
        /**
         * Perform the shift on a long and downcast it to int afterwards.
         * This is necessary to handle a cidrPrefix of zero correctly.
         * The left shift operator on an int only uses the five least
         * significant bits of the right-hand operand. Thus -1 << 32 evaluates
         * to -1 instead of 0. The left shift operator applied on a long
         * uses the six least significant bits.
         *
         * Also see https://github.com/netty/netty/issues/2767
         */
        return (-1L shl 32 - cidrPrefix and -0x1).toInt()
    }

    fun cidrPrefixFromSubnetMask(mask: String): Int {
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


    /**
     * Converts a byte array into a 32-bit integer
     */
    fun toInt(ipBytes: ByteArray): Int {
        return ipBytes[0].toInt() shl 24 or
                (ipBytes[1].toInt() shl 16) or
                (ipBytes[2].toInt() shl 8) or
                (ipBytes[3].toInt())
    }

    /**
     * Converts an IP address into a 32-bit integer
     */
    fun toInt(ipAsString: String): Int {
        return if (isValid(ipAsString)) {
            toIntUnsafe(ipAsString)
        } else {
            0
        }
    }

    /**
     * Converts an IP address into a 32-bit integer, no validity checks are performed
     */
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
     * Converts a 32-bit integer into a dotted-quad IPv4 address.
     */
    fun toString(ipAddress: Int): String {
        val buf = StringBuilder(15)
        toString(ipAddress, buf)
        return buf.toString()
    }

    /**
     * Converts a 32-bit integer into a dotted-quad IPv4 address.
     */
    fun toString(ipAddress: Int, buf: StringBuilder) {
        buf.append(ipAddress shr 24 and 0xFF)
        buf.append('.')
        buf.append(ipAddress shr 16 and 0xFF)
        buf.append('.')
        buf.append(ipAddress shr 8 and 0xFF)
        buf.append('.')
        buf.append(ipAddress and 0xFF)
    }

    /**
     * Converts a byte array into a dotted-quad IPv4 address.
     */
    fun toString(ipBytes: ByteArray): String {
        val buf = StringBuilder(15)
        toString(ipBytes, buf)
        return buf.toString()
    }

    /**
     * Converts a byte array into a dotted-quad IPv4 address.
     */
    fun toString(ipBytes: ByteArray, buf: StringBuilder) {
        buf.append(ipBytes[0].toInt() and 0xFF)
        buf.append('.')
        buf.append(ipBytes[1].toInt() and 0xFF)
        buf.append('.')
        buf.append(ipBytes[2].toInt() and 0xFF)
        buf.append('.')
        buf.append(ipBytes[3].toInt() and 0xFF)
    }

    /**
     * Converts an int array into a dotted-quad IPv4 address.
     */
    fun toString(ipInts: IntArray): String {
        val buf = StringBuilder(15)
        toString(ipInts, buf)
        return buf.toString()
    }

    /**
     * Converts an int array into a dotted-quad IPv4 address.
     */
    fun toString(ipInts: IntArray, buf: StringBuilder) {
        buf.append(ipInts[0])
        buf.append('.')
        buf.append(ipInts[1])
        buf.append('.')
        buf.append(ipInts[2])
        buf.append('.')
        buf.append(ipInts[3])
    }

    /**
     * Returns the [String] representation of an [InetAddress]. Results are identical to [InetAddress.getHostAddress]
     *
     * @param ip [InetAddress] to be converted to an address string
     *
     * @return `String` containing the text-formatted IP address
     */
    fun toString(ip: Inet4Address): String {
        return ip.hostAddress
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

    /**
     * Returns the [Inet4Address] representation of a [String] IP address.
     *
     * This method will treat all IPv4 type addresses as "IPv4 mapped" (see [.getByName])
     *
     * @param ip [String] IP address to be converted to a [Inet4Address]
     * @return [Inet4Address] representation of the `ip` or an exception if not a valid IP address.
     */
    @Throws(UnknownHostException::class)
    fun toAddressUnsafe(ip: String): Inet4Address {
        val asBytes = toBytes(ip)
        return Inet4Address.getByAddress(null, asBytes) as Inet4Address
    }

    /**
     * Returns the [Inet4Address] representation of a [String] IP address.
     *
     * This method will treat all IPv4 type addresses as "IPv4 mapped" (see [.getByName])
     *
     * @param ip [String] IP address to be converted to a [Inet4Address]
     * @return [Inet4Address] representation of the `ip` or `null` if not a valid IP address.
     */
    fun toAddress(ip: String): Inet4Address? {
        return if (isValid(ip)) {
            return toAddressUnsafe(ip)
        } else {
            null
        }
    }

    /**
     * Truncates an address to the specified number of bits.  For example,
     * truncating the address 10.1.2.3 to 8 bits would yield 10.0.0.0.
     *
     * @param address The source address
     * @param maskLength The number of bits to truncate the address to.
     */
    fun truncate(address: Inet4Address, maskLength: Int): InetAddress? {
        val maxMaskLength = IPv6.length * 8

        require(!(maskLength < 0 || maskLength > maxMaskLength)) { "invalid mask length" }

        if (maskLength == maxMaskLength) {
            return address
        }

        val bytes = address.address
        for (i in maskLength / 8 + 1 until bytes.size) {
            bytes[i] = 0
        }

        val maskBits = maskLength % 8
        var bitmask = 0
        for (i in 0 until maskBits) {
            bitmask = bitmask or (1 shl 7 - i)
        }

        bytes[maskLength / 8] = (bytes[maskLength / 8].toInt() and bitmask).toByte()

        return try {
            InetAddress.getByAddress(bytes)
        } catch (e: UnknownHostException) {
            throw IllegalArgumentException("invalid address")
        }
    }
}
