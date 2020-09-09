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

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException

/**
 * A class that holds a number of network-related constants, also from:
 * (Netty, apache 2.0 license)
 * https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/NetUtil.java
 *
 * This class borrowed some of its methods from a modified fork of the
 * [Inet6Util class]
 * (http://svn.apache.org/repos/asf/harmony/enhanced/java/branches/java6/classlib/modules/luni/src/main/java/org/apache/harmony/luni/util/Inet6Util.java) which was part of Apache Harmony.
 */
object IPv6 {
    /**
     * Maximum amount of value adding characters in between IPV6 separators
     */
    private const val IPV6_MAX_CHAR_BETWEEN_SEPARATOR = 4

    /**
     * Minimum number of separators that must be present in an IPv6 string
     */
    private const val IPV6_MIN_SEPARATORS = 2

    /**
     * Maximum number of separators that must be present in an IPv6 string
     */
    private const val IPV6_MAX_SEPARATORS = 8

    /**
     * Maximum amount of value adding characters in between IPV4 separators
     */
    private const val IPV4_MAX_CHAR_BETWEEN_SEPARATOR = 3

    /**
     * Number of separators that must be present in an IPv4 string
     */
    private const val IPV4_SEPARATORS = 3

    /**
     * Number of bytes needed to represent and IPV6 value
     */
    private const val IPV6_BYTE_COUNT = 16

    /**
     * This defines how many words (represented as ints) are needed to represent an IPv6 address
     */
    private const val IPV6_WORD_COUNT = 8


    /**
     * The maximum number of characters for an IPV6 string with no scope
     */
    private const val IPV6_MAX_CHAR_COUNT = 39

    /**
     * Returns `true` if an IPv6 address should be preferred when a host has both an IPv4 address and an IPv6
     * address. The default value of this property is `false`.
     *
     * @see [Java SE
     * networking properties](https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html)
     */
    val isPreferred = Common.getBoolean("java.net.preferIPv6Addresses", false)

    /**
     * true if there is an IPv6 network interface available
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
        var ipv6Possible = false
        loop@ for (iface in netInterfaces) {
            val i = SocketUtils.addressesFromNetworkInterface(iface)
            while (i.hasMoreElements()) {
                val addr: InetAddress = i.nextElement()
                if (addr is Inet6Address) {
                    ipv6Possible = true
                    break@loop
                }
            }
        }

        ipv6Possible
    }


    /**
     * The [Inet6Address] that represents the IPv6 loopback address '::1'
     */
    val LOCALHOST: Inet6Address by lazy {
        // Create IPv6 address, this will ALWAYS work
        InetAddress.getByAddress("localhost", byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1)) as Inet6Address
    }

    /**
     * The [Inet4Address] that represents the IPv4 wildcard address '0.0.0.0'
     */
    val WILDCARD: Inet6Address by lazy {
        // Create IPv6 address, this will ALWAYS work
        InetAddress.getByAddress("", byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)) as Inet6Address
    }

    /**
     * Takes a [String] and parses it to see if it is a valid IPV6 address.
     *
     * @return true, if the string represents an IPV6 address, false otherwise
     */
    fun isValid(ip: String): Boolean {
        var end = ip.length
        if (end < 2) {
            return false
        }

        // strip "[]"
        var start: Int
        var c = ip[0]
        if (c == '[') {
            end--
            if (ip[end] != ']') {
                // must have a close ]
                return false
            }
            start = 1
            c = ip[1]
        }
        else {
            start = 0
        }
        var colons: Int
        var compressBegin: Int
        if (c == ':') {
            // an IPv6 address can start with "::" or with a number
            if (ip[start + 1] != ':') {
                return false
            }
            colons = 2
            compressBegin = start
            start += 2
        }
        else {
            colons = 0
            compressBegin = -1
        }
        var wordLen = 0
        loop@ for (i in start until end) {
            c = ip[i]
            if (isValidHexChar(c)) {
                if (wordLen < 4) {
                    wordLen++
                    continue
                }
                return false
            }
            when (c) {
                ':' -> {
                    if (colons > 7) {
                        return false
                    }
                    if (ip[i - 1] == ':') {
                        if (compressBegin >= 0) {
                            return false
                        }
                        compressBegin = i - 1
                    }
                    else {
                        wordLen = 0
                    }
                    colons++
                }
                '.' -> {
                    // case for the last 32-bits represented as IPv4 x:x:x:x:x:x:d.d.d.d

                    // check a normal case (6 single colons)
                    if (compressBegin < 0 && colons != 6 ||  // a special case ::1:2:3:4:5:d.d.d.d allows 7 colons with an
                        // IPv4 ending, otherwise 7 :'s is bad
                        colons == 7 && compressBegin >= start || colons > 7) {
                        return false
                    }

                    // Verify this address is of the correct structure to contain an IPv4 address.
                    // It must be IPv4-Mapped or IPv4-Compatible
                    // (see https://tools.ietf.org/html/rfc4291#section-2.5.5).
                    val ipv4Start = i - wordLen
                    var j = ipv4Start - 2 // index of character before the previous ':'.
                    if (IPv4.isValidIPv4MappedChar(ip[j])) {
                        if (!IPv4.isValidIPv4MappedChar(ip[j - 1])
                            || !IPv4.isValidIPv4MappedChar(ip[j - 2])
                            || !IPv4.isValidIPv4MappedChar(ip[j - 3])) {
                            return false
                        }
                        j -= 5
                    }
                    while (j >= start) {
                        val tmpChar = ip[j]
                        if (tmpChar != '0' && tmpChar != ':') {
                            return false
                        }
                        --j
                    }

                    // 7 - is minimum IPv4 address length
                    var ipv4End = ip.indexOf('%', ipv4Start + 7)
                    if (ipv4End < 0) {
                        ipv4End = end
                    }
                    return IPv4.isValidIpV4Address(ip, ipv4Start, ipv4End)
                }
                '%' -> {
                    // strip the interface name/index after the percent sign
                    end = i
                    break@loop
                }
                else -> return false
            }
        }

        // normal case without compression
        return if (compressBegin < 0) {
            colons == 7 && wordLen > 0
        }
        else compressBegin + 2 == end ||  // 8 colons is valid only if compression in start or end
             wordLen > 0 && (colons < 8 || compressBegin <= start)
    }

    private fun isValidHexChar(c: Char): Boolean {
        return c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f'
    }

    /**
     * Returns the [Inet6Address] representation of a [CharSequence] IP address.
     *
     * This method will treat all IPv4 type addresses as "IPv4 mapped" (see [.getByName])
     *
     * @param ip [CharSequence] IP address to be converted to a [Inet6Address]
     * @return [Inet6Address] representation of the `ip` or `null` if not a valid IP address.
     */
    fun getByName(ip: String): Inet6Address? {
        return getByName(ip, true)
    }


    /**
     * Returns the [Inet6Address] representation of a [String] IP address.
     * <p>
     * The [ipv4Mapped] parameter specifies how IPv4 addresses should be treated. The "IPv4 mapped" format is
     * defined in <a href="http://tools.ietf.org/html/rfc4291#section-2.5.5">rfc 4291 section 2</a> is supported.
     *
     * @param ip [String] IP address to be converted to a [Inet6Address]
     * @param ipv4Mapped
     * <ul>
     * <li>{@code true} To allow IPv4 mapped inputs to be translated into {@link Inet6Address}</li>
     * <li>{@code false} Consider IPv4 mapped addresses as invalid.</li>
     * </ul>
     * @return [Inet6Address] representation of the [ip] or [null] if not a valid IP address.
     */
    fun getByName(ip: String, ipv4Mapped: Boolean): Inet6Address? {
        val bytes = getIPv6ByName(ip, ipv4Mapped) ?: return null
        return try {
            Inet6Address.getByAddress(null, bytes, -1)
        } catch (e: UnknownHostException) {
            throw RuntimeException(e) // Should never happen
        }
    }

    /**
     * Returns the byte array representation of a [CharSequence] IP address.
     *
     *
     * The `ipv4Mapped` parameter specifies how IPv4 addresses should be treated.
     * "IPv4 mapped" format as
     * defined in [rfc 4291 section 2](http://tools.ietf.org/html/rfc4291#section-2.5.5) is supported.
     * @param ip [CharSequence] IP address to be converted to a [Inet6Address]
     * @param ipv4Mapped
     *
     *  * `true` To allow IPv4 mapped inputs to be translated into [Inet6Address]
     *  * `false` Consider IPv4 mapped addresses as invalid.
     *
     * @return byte array representation of the `ip` or `null` if not a valid IP address.
     */
    private fun getIPv6ByName(ip: CharSequence, ipv4Mapped: Boolean): ByteArray? {
        val bytes = ByteArray(IPV6_BYTE_COUNT)
        val ipLength = ip.length
        var compressBegin = 0
        var compressLength = 0
        var currentIndex = 0
        var value = 0
        var begin = -1
        var i = 0
        var ipv6Separators = 0
        var ipv4Separators = 0
        var tmp: Int
        var needsShift = false
        while (i < ipLength) {
            val c = ip[i]
            when (c) {
                ':' -> {
                    ++ipv6Separators
                    if (i - begin > IPV6_MAX_CHAR_BETWEEN_SEPARATOR || ipv4Separators > 0 || ipv6Separators > IPV6_MAX_SEPARATORS || currentIndex + 1 >= bytes.size) {
                        return null
                    }
                    value = value shl (IPV6_MAX_CHAR_BETWEEN_SEPARATOR - (i - begin) shl 2)
                    if (compressLength > 0) {
                        compressLength -= 2
                    }

                    // The value integer holds at most 4 bytes from right (most significant) to left (least significant).
                    // The following bit shifting is used to extract and re-order the individual bytes to achieve a
                    // left (most significant) to right (least significant) ordering.
                    bytes[currentIndex++] = (value and 0xf shl 4 or (value shr 4 and 0xf)).toByte()
                    bytes[currentIndex++] = (value shr 8 and 0xf shl 4 or (value shr 12 and 0xf)).toByte()
                    tmp = i + 1
                    if (tmp < ipLength && ip[tmp] == ':') {
                        ++tmp
                        if (compressBegin != 0 || tmp < ipLength && ip[tmp] == ':') {
                            return null
                        }
                        ++ipv6Separators
                        needsShift = ipv6Separators == 2 && value == 0
                        compressBegin = currentIndex
                        compressLength = bytes.size - compressBegin - 2
                        ++i
                    }
                    value = 0
                    begin = -1
                }
                '.' -> {
                    ++ipv4Separators
                    tmp = i - begin // tmp is the length of the current segment.
                    if (tmp > IPV4_MAX_CHAR_BETWEEN_SEPARATOR || begin < 0 || ipv4Separators > IPV4_SEPARATORS || ipv6Separators > 0 && currentIndex + compressLength < 12 || i + 1 >= ipLength || currentIndex >= bytes.size || ipv4Separators == 1 &&

                        // We also parse pure IPv4 addresses as IPv4-Mapped for ease of use.
                        (!ipv4Mapped || currentIndex != 0 && !isValidIPv4Mapped(bytes, currentIndex, compressBegin, compressLength) || tmp == 3 && (!IPv4.isValidNumericChar(
                                ip[i - 1]) || !IPv4.isValidNumericChar(ip[i - 2]) || !IPv4.isValidNumericChar(ip[i - 3])) || tmp == 2 && (!IPv4.isValidNumericChar(
                                ip[i - 1]) || !IPv4.isValidNumericChar(ip[i - 2])) || tmp == 1 && !IPv4.isValidNumericChar(ip[i - 1]))) {
                        return null
                    }
                    value = value shl (IPV4_MAX_CHAR_BETWEEN_SEPARATOR - tmp shl 2)

                    // The value integer holds at most 3 bytes from right (most significant) to left (least significant).
                    // The following bit shifting is to restructure the bytes to be left (most significant) to
                    // right (least significant) while also accounting for each IPv4 digit is base 10.
                    begin = (value and 0xf) * 100 + (value shr 4 and 0xf) * 10 + (value shr 8 and 0xf)
                    if (begin < 0 || begin > 255) {
                        return null
                    }
                    bytes[currentIndex++] = begin.toByte()
                    value = 0
                    begin = -1
                }
                else -> {
                    if (!isValidHexChar(c) || ipv4Separators > 0 && !IPv4.isValidNumericChar(c)) {
                        return null
                    }
                    if (begin < 0) {
                        begin = i
                    }
                    else if (i - begin > IPV6_MAX_CHAR_BETWEEN_SEPARATOR) {
                        return null
                    }
                    // The value is treated as a sort of array of numbers because we are dealing with
                    // at most 4 consecutive bytes we can use bit shifting to accomplish this.
                    // The most significant byte will be encountered first, and reside in the right most
                    // position of the following integer
                    value += Common.decodeHexNibble(c) shl (i - begin shl 2)
                }
            }
            ++i
        }
        val isCompressed = compressBegin > 0
        // Finish up last set of data that was accumulated in the loop (or before the loop)
        if (ipv4Separators > 0) {
            if (begin > 0 && i - begin > IPV4_MAX_CHAR_BETWEEN_SEPARATOR || ipv4Separators != IPV4_SEPARATORS || currentIndex >= bytes.size) {
                return null
            }
            if (ipv6Separators == 0) {
                compressLength = 12
            }
            else if (ipv6Separators >= IPV6_MIN_SEPARATORS && (!isCompressed && ipv6Separators == 6 && ip[0] != ':' || isCompressed && ipv6Separators < IPV6_MAX_SEPARATORS && (ip[0] != ':' || compressBegin <= 2))) {
                compressLength -= 2
            }
            else {
                return null
            }
            value = value shl (IPV4_MAX_CHAR_BETWEEN_SEPARATOR - (i - begin) shl 2)

            // The value integer holds at most 3 bytes from right (most significant) to left (least significant).
            // The following bit shifting is to restructure the bytes to be left (most significant) to
            // right (least significant) while also accounting for each IPv4 digit is base 10.
            begin = (value and 0xf) * 100 + (value shr 4 and 0xf) * 10 + (value shr 8 and 0xf)
            if (begin < 0 || begin > 255) {
                return null
            }
            bytes[currentIndex++] = begin.toByte()
        }
        else {
            tmp = ipLength - 1
            if (begin > 0 && i - begin > IPV6_MAX_CHAR_BETWEEN_SEPARATOR || ipv6Separators < IPV6_MIN_SEPARATORS || !isCompressed && (ipv6Separators + 1 != IPV6_MAX_SEPARATORS || ip[0] == ':' || ip[tmp] == ':') || isCompressed && (ipv6Separators > IPV6_MAX_SEPARATORS || ipv6Separators == IPV6_MAX_SEPARATORS && (compressBegin <= 2 && ip[0] != ':' || compressBegin >= 14 && ip[tmp] != ':')) || currentIndex + 1 >= bytes.size || begin < 0 && ip[tmp - 1] != ':' || compressBegin > 2 && ip[0] == ':') {
                return null
            }
            if (begin >= 0 && i - begin <= IPV6_MAX_CHAR_BETWEEN_SEPARATOR) {
                value = value shl (IPV6_MAX_CHAR_BETWEEN_SEPARATOR - (i - begin) shl 2)
            }
            // The value integer holds at most 4 bytes from right (most significant) to left (least significant).
            // The following bit shifting is used to extract and re-order the individual bytes to achieve a
            // left (most significant) to right (least significant) ordering.
            bytes[currentIndex++] = (value and 0xf shl 4 or (value shr 4 and 0xf)).toByte()
            bytes[currentIndex++] = (value shr 8 and 0xf shl 4 or (value shr 12 and 0xf)).toByte()
        }
        i = currentIndex + compressLength
        if (needsShift || i >= bytes.size) {
            // Right shift array
            if (i >= bytes.size) {
                ++compressBegin
            }
            i = currentIndex
            while (i < bytes.size) {
                begin = bytes.size - 1
                while (begin >= compressBegin) {
                    bytes[begin] = bytes[begin - 1]
                    --begin
                }
                bytes[begin] = 0
                ++compressBegin
                ++i
            }
        }
        else {
            // Selectively move elements
            i = 0
            while (i < compressLength) {
                begin = i + compressBegin
                currentIndex = begin + compressLength
                if (currentIndex < bytes.size) {
                    bytes[currentIndex] = bytes[begin]
                    bytes[begin] = 0
                }
                else {
                    break
                }
                ++i
            }
        }
        if (ipv4Separators > 0) {
            // We only support IPv4-Mapped addresses [1] because IPv4-Compatible addresses are deprecated [2].
            // [1] https://tools.ietf.org/html/rfc4291#section-2.5.5.2
            // [2] https://tools.ietf.org/html/rfc4291#section-2.5.5.1
            bytes[11] = 0xff.toByte()
            bytes[10] = bytes[11]
        }
        return bytes
    }

    private fun isValidIPv4Mapped(bytes: ByteArray, currentIndex: Int, compressBegin: Int, compressLength: Int): Boolean {
        val mustBeZero = compressBegin + compressLength >= 14
        return currentIndex <= 12 && currentIndex >= 2 && (!mustBeZero || compressBegin < 12)
               && isValidIPv4MappedSeparators(bytes[currentIndex - 1], bytes[currentIndex - 2], mustBeZero) &&
               isZero(bytes, 0, currentIndex - 3)
    }

    private fun isZero(bytes: ByteArray, startPos: Int, length: Int): Boolean {
        var start = startPos
        val end = start + length
        val zeroByteByte = 0.toByte()

        while (start < end) {
            if (bytes[start++] != zeroByteByte) {
                return false
            }
        }
        return true
    }

    private fun isValidIPv4MappedSeparators(b0: Byte, b1: Byte, mustBeZero: Boolean): Boolean {
        // We allow IPv4 Mapped (https://tools.ietf.org/html/rfc4291#section-2.5.5.1)
        // and IPv4 compatible (https://tools.ietf.org/html/rfc4291#section-2.5.5.1).
        // The IPv4 compatible is deprecated, but it allows parsing of plain IPv4 addressed into IPv6-Mapped addresses.
        return b0 == b1 && (b0.toInt() == 0 || !mustBeZero && b1.toInt() == -1)
    }

    /**
     * Creates an byte[] based on an ipAddressString. No error handling is performed here.
     */
    fun toBytesOrNull(ipAddress: String): ByteArray? {
        if (isValid(ipAddress)) {
            return getByName(ipAddress)?.address
        }

        return null
    }
    /**
     * Creates an byte[] based on an ipAddressString. No error handling is performed here.
     */
    fun toBytes(ipAddress: String): ByteArray {
        // always return a byte array
        var fixedIp = ipAddress
        if (fixedIp[0] == '[') {
            fixedIp = fixedIp.substring(1, fixedIp.length - 1)
        }
        val percentPos: Int = fixedIp.indexOf('%')
        if (percentPos >= 0) {
            fixedIp = fixedIp.substring(0, percentPos)
        }

        return getByName(fixedIp)?.address ?: ByteArray(32)
    }


    /**
     * Returns the [String] representation of an [InetAddress].
     *
     *  * Inet4Address results are identical to [InetAddress.getHostAddress]
     *  * Inet6Address results adhere to
     * [rfc 5952 section 4](http://tools.ietf.org/html/rfc5952#section-4) if
     * `ipv4Mapped` is false.  If `ipv4Mapped` is true then "IPv4 mapped" format
     * from [rfc 4291 section 2](http://tools.ietf.org/html/rfc4291#section-2.5.5) will be supported.
     * The compressed result will always obey the compression rules defined in
     * [rfc 5952 section 4](http://tools.ietf.org/html/rfc5952#section-4)
     *
     *
     *
     * The output does not include Scope ID.
     *
     * @param bytes [InetAddress] to be converted to an address string
     *
     * @return `String` containing the text-formatted IP address
     */
    fun toString(bytes: ByteArray): String {
        return toString(bytes, 0)
    }

    /**
     * Returns the [String] representation of an [InetAddress].
     *
     *  * Inet4Address results are identical to [InetAddress.getHostAddress]
     *  * Inet6Address results adhere to
     * [rfc 5952 section 4](http://tools.ietf.org/html/rfc5952#section-4) if
     * `ipv4Mapped` is false.  If `ipv4Mapped` is true then "IPv4 mapped" format
     * from [rfc 4291 section 2](http://tools.ietf.org/html/rfc4291#section-2.5.5) will be supported.
     * The compressed result will always obey the compression rules defined in
     * [rfc 5952 section 4](http://tools.ietf.org/html/rfc5952#section-4)
     *
     *
     *
     * The output does not include Scope ID.
     *
     * @param bytes [InetAddress] to be converted to an address string
     *
     * @return `String` containing the text-formatted IP address
     */
    fun toString(bytes: ByteArray, offset: Int): String {
        return toAddressString(bytes, offset, false)
    }

    /**
     * Returns the [String] representation of an [InetAddress].
     *
     *  * Inet4Address results are identical to [InetAddress.getHostAddress]
     *  * Inet6Address results adhere to
     * [rfc 5952 section 4](http://tools.ietf.org/html/rfc5952#section-4) if
     * `ipv4Mapped` is false.  If `ipv4Mapped` is true then "IPv4 mapped" format
     * from [rfc 4291 section 2](http://tools.ietf.org/html/rfc4291#section-2.5.5) will be supported.
     * The compressed result will always obey the compression rules defined in
     * [rfc 5952 section 4](http://tools.ietf.org/html/rfc5952#section-4)
     *
     *
     *
     * The output does not include Scope ID.
     *
     * @param ip [InetAddress] to be converted to an address string
     * @param ipv4Mapped
     *
     *  * `true` to stray from strict rfc 5952 and support the "IPv4 mapped" format
     * defined in [rfc 4291 section 2](http://tools.ietf.org/html/rfc4291#section-2.5.5) while still
     * following the updated guidelines in
     * [rfc 5952 section 4](http://tools.ietf.org/html/rfc5952#section-4)
     *
     *  * `false` to strictly follow rfc 5952
     *
     * @return `String` containing the text-formatted IP address
     */
    fun toString(ip: Inet6Address, ipv4Mapped: Boolean = false): String {
        return toAddressString(ip.address, 0, ipv4Mapped)
    }

    private fun toAddressString(bytes: ByteArray, offset: Int, ipv4Mapped: Boolean): String {
        val words = IntArray(IPV6_WORD_COUNT)
        var i: Int
        val end = offset + words.size
        i = offset
        while (i < end) {
            words[i] = bytes[i shl 1].toInt() and 0xff shl 8 or (bytes[(i shl 1) + 1].toInt() and 0xff)
            ++i
        }

        // Find longest run of 0s, tie goes to first found instance
        var currentStart = -1
        var currentLength: Int
        var shortestStart = -1
        var shortestLength = 0
        i = 0
        while (i < words.size) {
            if (words[i] == 0) {
                if (currentStart < 0) {
                    currentStart = i
                }
            }
            else if (currentStart >= 0) {
                currentLength = i - currentStart
                if (currentLength > shortestLength) {
                    shortestStart = currentStart
                    shortestLength = currentLength
                }
                currentStart = -1
            }
            ++i
        }
        // If the array ends on a streak of zeros, make sure we account for it
        if (currentStart >= 0) {
            currentLength = i - currentStart
            if (currentLength > shortestLength) {
                shortestStart = currentStart
                shortestLength = currentLength
            }
        }
        // Ignore the longest streak if it is only 1 long
        if (shortestLength == 1) {
            shortestLength = 0
            shortestStart = -1
        }

        // Translate to string taking into account longest consecutive 0s
        val shortestEnd = shortestStart + shortestLength
        val b = StringBuilder(IPV6_MAX_CHAR_COUNT)
        if (shortestEnd < 0) { // Optimization when there is no compressing needed
            b.append(Integer.toHexString(words[0]))
            i = 1
            while (i < words.size) {
                b.append(':')
                b.append(Integer.toHexString(words[i]))
                ++i
            }
        }
        else { // General case that can handle compressing (and not compressing)
            // Loop unroll the first index (so we don't constantly check i==0 cases in loop)
            val isIpv4Mapped: Boolean
            isIpv4Mapped = if (inRangeEndExclusive(0, shortestStart, shortestEnd)) {
                b.append("::")
                ipv4Mapped && shortestEnd == 5 && words[5] == 0xffff
            }
            else {
                b.append(Integer.toHexString(words[0]))
                false
            }
            i = 1
            while (i < words.size) {
                if (!inRangeEndExclusive(i, shortestStart, shortestEnd)) {
                    if (!inRangeEndExclusive(i - 1, shortestStart, shortestEnd)) {
                        // If the last index was not part of the shortened sequence
                        if (!isIpv4Mapped || i == 6) {
                            b.append(':')
                        }
                        else {
                            b.append('.')
                        }
                    }
                    if (isIpv4Mapped && i > 5) {
                        b.append(words[i] shr 8)
                        b.append('.')
                        b.append(words[i] and 0xff)
                    }
                    else {
                        b.append(Integer.toHexString(words[i]))
                    }
                }
                else if (!inRangeEndExclusive(i - 1, shortestStart, shortestEnd)) {
                    // If we are in the shortened sequence and the last index was not
                    b.append("::")
                }
                ++i
            }
        }
        return b.toString()
    }

    /**
     * Does a range check on `value` if is within `start` (inclusive) and `end` (exclusive).
     * @param value The value to checked if is within `start` (inclusive) and `end` (exclusive)
     * @param start The start of the range (inclusive)
     * @param end The end of the range (exclusive)
     * @return
     *
     *  * `true` if `value` if is within `start` (inclusive) and `end` (exclusive)
     *  * `false` otherwise
     *
     */
    private fun inRangeEndExclusive(value: Int, start: Int, end: Int): Boolean {
        return value >= start && value < end
    }


    /**
     * IPv6 address reserved for loopback use is 0000:0000:0000:0000:0000:0000:0000:0001/128.
     *
     * This loopback address is so lengthy and can be further simplified as ::1/128, or just ::1
     *
     * /128 means EXACTLY this address
     */
    fun isLoopback(ipAsString: String): Boolean {
        var oneCount = 0
        // can only be with one "1", and nothing else
        ipAsString.forEach { char ->
            when (char) {
                ':' -> {
                    // this is always ok
                }
                '1' -> {
                    oneCount++
                    if (oneCount > 1) {
                        return false
                    }
                }
                else -> {
                    // not OK
                    return false
                }
            }
        }

        return oneCount == 1
    }
}
