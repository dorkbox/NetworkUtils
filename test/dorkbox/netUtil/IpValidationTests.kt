/*
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

import dorkbox.util.Sys
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException

class IpValidationTests {
    companion object {
        private val validIpV4Hosts = mapOf(
//                "192.168.1.0" to "c0a80100",
                "10.255.255.254" to "0afffffe",
                "172.18.5.4" to "ac120504",
                "0.0.0.0" to "00000000",
                "127.0.0.1" to "7f000001",
                "255.255.255.255" to "ffffffff",
                "1.2.3.4" to "01020304")

        private val invalidIpV4Hosts = mapOf(
                "1.256.3.4" to null,
                "256.0.0.1" to null,
                "1.1.1.1.1" to null,
                "x.255.255.255" to null,
                "0.1:0.0" to null,
                "0.1.0.0:" to null,
                "127.0.0." to null,
                "1.2..4" to null,
                "192.0.1" to null,
                "192.0.1.1.1" to null,
                "192.0.1.a" to null,
                "19a.0.1.1" to null,
                "a.0.1.1" to null,
                ".0.1.1" to null,
                "127.0.0" to null,
                "192.0.1.256" to null,
                "0.0.200.259" to null,
                "1.1.-1.1" to null,
                "1.1. 1.1" to null,
                "1.1.1.1 " to null,
                "1.1.+1.1" to null,
                "0.0x1.0.255" to null,
                "0.01x.0.255" to null,
                "0.x01.0.255" to null,
                "0.-.0.0" to null,
                "0..0.0" to null,
                "0.A.0.0" to null,
                "0.1111.0.0" to null,
                "..." to null)

        private val validIpV6Hosts = mapOf(
                "::ffff:5.6.7.8" to "00000000000000000000ffff05060708",
                "fdf8:f53b:82e4::53" to "fdf8f53b82e400000000000000000053",
                "fe80::200:5aee:feaa:20a2" to "fe8000000000000002005aeefeaa20a2",
                "2001::1" to "20010000000000000000000000000001",
                "2001:0000:4136:e378:8000:63bf:3fff:fdd2" to "200100004136e378800063bf3ffffdd2",
                "2001:0002:6c::430" to "20010002006c00000000000000000430",
                "2001:10:240:ab::a" to "20010010024000ab000000000000000a",
                "2002:cb0a:3cdd:1::1" to "2002cb0a3cdd00010000000000000001",
                "2001:db8:8:4::2" to "20010db8000800040000000000000002",
                "ff01:0:0:0:0:0:0:2" to "ff010000000000000000000000000002",
                "[fdf8:f53b:82e4::53]" to "fdf8f53b82e400000000000000000053",
                "[fe80::200:5aee:feaa:20a2]" to "fe8000000000000002005aeefeaa20a2",
                "[2001::1]" to "20010000000000000000000000000001",
                "[2001:0000:4136:e378:8000:63bf:3fff:fdd2]" to "200100004136e378800063bf3ffffdd2",
                "0:1:2:3:4:5:6:789a" to "0000000100020003000400050006789a",
                "0:1:2:3::f" to "0000000100020003000000000000000f",
                "0:0:0:0:0:0:10.0.0.1" to "00000000000000000000ffff0a000001",
                "0:0:0:0:0::10.0.0.1" to "00000000000000000000ffff0a000001",
                "0:0:0:0::10.0.0.1" to "00000000000000000000ffff0a000001",
                "::0:0:0:0:0:10.0.0.1" to "00000000000000000000ffff0a000001",
                "0::0:0:0:0:10.0.0.1" to "00000000000000000000ffff0a000001",
                "0:0::0:0:0:10.0.0.1" to "00000000000000000000ffff0a000001",
                "0:0:0::0:0:10.0.0.1" to "00000000000000000000ffff0a000001",
                "0:0:0:0::0:10.0.0.1" to "00000000000000000000ffff0a000001",
                "0:0:0:0:0:ffff:10.0.0.1" to "00000000000000000000ffff0a000001",
                "::ffff:192.168.0.1" to "00000000000000000000ffffc0a80001",
                // Test if various interface names after the percent sign are recognized.
                "[::1%1]" to "00000000000000000000000000000001",
                "[::1%eth0]" to "00000000000000000000000000000001",
                "[::1%%]" to "00000000000000000000000000000001",
                "0:0:0:0:0:ffff:10.0.0.1%" to "00000000000000000000ffff0a000001",
                "0:0:0:0:0:ffff:10.0.0.1%1" to "00000000000000000000ffff0a000001",
                "[0:0:0:0:0:ffff:10.0.0.1%1]" to "00000000000000000000ffff0a000001",
                "[0:0:0:0:0::10.0.0.1%1]" to "00000000000000000000ffff0a000001",
                "[::0:0:0:0:ffff:10.0.0.1%1]" to "00000000000000000000ffff0a000001",
                "::0:0:0:0:ffff:10.0.0.1%1" to "00000000000000000000ffff0a000001",
                "::1%1" to "00000000000000000000000000000001",
                "::1%eth0" to "00000000000000000000000000000001",
                "::1%%" to "00000000000000000000000000000001",
                // Tests with leading or trailing compression
                "0:0:0:0:0:0:0::" to "00000000000000000000000000000000",
                "0:0:0:0:0:0::" to "00000000000000000000000000000000",
                "0:0:0:0:0::" to "00000000000000000000000000000000",
                "0:0:0:0::" to "00000000000000000000000000000000",
                "0:0:0::" to "00000000000000000000000000000000",
                "0:0::" to "00000000000000000000000000000000",
                "0::" to "00000000000000000000000000000000",
                "::" to "00000000000000000000000000000000",
                "::0" to "00000000000000000000000000000000",
                "::0:0" to "00000000000000000000000000000000",
                "::0:0:0" to "00000000000000000000000000000000",
                "::0:0:0:0" to "00000000000000000000000000000000",
                "::0:0:0:0:0" to "00000000000000000000000000000000",
                "::0:0:0:0:0:0" to "00000000000000000000000000000000",
                "::0:0:0:0:0:0:0" to "00000000000000000000000000000000")


        private val invalidIpV6Hosts = mapOf(
                // Test method with garbage.
                "Obvious Garbage" to null,
                // Test method with preferred style, too many :
                "0:1:2:3:4:5:6:7:8" to null,
                // Test method with preferred style, not enough :
                "0:1:2:3:4:5:6" to null,
                // Test method with preferred style, bad digits.
                "0:1:2:3:4:5:6:x" to null,
                // Test method with preferred style, adjacent :
                "0:1:2:3:4:5:6::7" to null,
                // Too many : separators trailing
                "0:1:2:3:4:5:6:7::" to null,
                // Too many : separators leading
                "::0:1:2:3:4:5:6:7" to null,
                // Too many : separators trailing
                "1:2:3:4:5:6:7:" to null,
                // Too many : separators leading
                ":1:2:3:4:5:6:7" to null,
                // Compression with : separators trailing
                "0:1:2:3:4:5::7:" to null,
                "0:1:2:3:4::7:" to null,
                "0:1:2:3::7:" to null,
                "0:1:2::7:" to null,
                "0:1::7:" to null,
                "0::7:" to null,
                // Compression at start with : separators trailing
                "::0:1:2:3:4:5:7:" to null,
                "::0:1:2:3:4:7:" to null,
                "::0:1:2:3:7:" to null,
                "::0:1:2:7:" to null,
                "::0:1:7:" to null,
                "::7:" to null,
                // The : separators leading and trailing
                ":1:2:3:4:5:6:7:" to null,
                ":1:2:3:4:5:6:" to null,
                ":1:2:3:4:5:" to null,
                ":1:2:3:4:" to null,
                ":1:2:3:" to null,
                ":1:2:" to null,
                ":1:" to null,
                // Compression with : separators leading
                ":1::2:3:4:5:6:7" to null,
                ":1::3:4:5:6:7" to null,
                ":1::4:5:6:7" to null,
                ":1::5:6:7" to null,
                ":1::6:7" to null,
                ":1::7" to null,
                ":1:2:3:4:5:6::7" to null,
                ":1:3:4:5:6::7" to null,
                ":1:4:5:6::7" to null,
                ":1:5:6::7" to null,
                ":1:6::7" to null,
                ":1::" to null,
                // Compression trailing with : separators leading
                ":1:2:3:4:5:6:7::" to null,
                ":1:3:4:5:6:7::" to null,
                ":1:4:5:6:7::" to null,
                ":1:5:6:7::" to null,
                ":1:6:7::" to null,
                ":1:7::" to null,
                // Double compression
                "1::2:3:4:5:6::" to null,
                "::1:2:3:4:5::6" to null,
                "::1:2:3:4:5:6::" to null,
                "::1:2:3:4:5::" to null,
                "::1:2:3:4::" to null,
                "::1:2:3::" to null,
                "::1:2::" to null,
                "::0::" to null,
                "12::0::12" to null,
                // Too many : separators leading 0
                "0::1:2:3:4:5:6:7" to null,
                // Test method with preferred style, too many digits.
                "0:1:2:3:4:5:6:789abcdef" to null,
                // Test method with compressed style, bad digits.
                "0:1:2:3::x" to null,
                // Test method with compressed style to too many adjacent :
                "0:1:2:::3" to null,
                // Test method with compressed style, too many digits.
                "0:1:2:3::abcde" to null,
                // Test method with compressed style, not enough :
                "0:1" to null,
                // Test method with ipv4 style, bad ipv6 digits.
                "0:0:0:0:0:x:10.0.0.1" to null,
                // Test method with ipv4 style, bad ipv4 digits.
                "0:0:0:0:0:0:10.0.0.x" to null,
                // Test method with ipv4 style, too many ipv6 digits.
                "0:0:0:0:0:00000:10.0.0.1" to null,
                // Test method with ipv4 style, too many :
                "0:0:0:0:0:0:0:10.0.0.1" to null,
                // Test method with ipv4 style, not enough :
                "0:0:0:0:0:10.0.0.1" to null,
                // Test method with ipv4 style, too many .
                "0:0:0:0:0:0:10.0.0.0.1" to null,
                // Test method with ipv4 style, not enough .
                "0:0:0:0:0:0:10.0.1" to null,
                // Test method with ipv4 style, adjacent .
                "0:0:0:0:0:0:10..0.0.1" to null,
                // Test method with ipv4 style, leading .
                "0:0:0:0:0:0:.0.0.1" to null,
                // Test method with ipv4 style, leading .
                "0:0:0:0:0:0:.10.0.0.1" to null,
                // Test method with ipv4 style, trailing .
                "0:0:0:0:0:0:10.0.0." to null,
                // Test method with ipv4 style, trailing .
                "0:0:0:0:0:0:10.0.0.1." to null,
                // Test method with compressed ipv4 style, bad ipv6 digits.
                "::fffx:192.168.0.1" to null,
                // Test method with compressed ipv4 style, bad ipv4 digits.
                "::ffff:192.168.0.x" to null,
                // Test method with compressed ipv4 style, too many adjacent :
                ":::ffff:192.168.0.1" to null,
                // Test method with compressed ipv4 style, too many ipv6 digits.
                "::fffff:192.168.0.1" to null,
                // Test method with compressed ipv4 style, too many ipv4 digits.
                "::ffff:1923.168.0.1" to null,
                // Test method with compressed ipv4 style, not enough :
                ":ffff:192.168.0.1" to null,
                // Test method with compressed ipv4 style, too many .
                "::ffff:192.168.0.1.2" to null,
                // Test method with compressed ipv4 style, not enough .
                "::ffff:192.168.0" to null,
                // Test method with compressed ipv4 style, adjacent .
                "::ffff:192.168..0.1" to null,
                // Test method, bad ipv6 digits.
                "x:0:0:0:0:0:10.0.0.1" to null,
                // Test method, bad ipv4 digits.
                "0:0:0:0:0:0:x.0.0.1" to null,
                // Test method, too many ipv6 digits.
                "00000:0:0:0:0:0:10.0.0.1" to null,
                // Test method, too many ipv4 digits.
                "0:0:0:0:0:0:10.0.0.1000" to null,
                // Test method, too many :
                "0:0:0:0:0:0:0:10.0.0.1" to null,
                // Test method, not enough :
                "0:0:0:0:0:10.0.0.1" to null,
                // Test method, out of order trailing :
                "0:0:0:0:0:10.0.0.1:" to null,
                // Test method, out of order leading :
                ":0:0:0:0:0:10.0.0.1" to null,
                // Test method, out of order leading :
                "0:0:0:0::10.0.0.1:" to null,
                // Test method, out of order trailing :
                ":0:0:0:0::10.0.0.1" to null,
                // Test method, too many .
                "0:0:0:0:0:0:10.0.0.0.1" to null,
                // Test method, not enough .
                "0:0:0:0:0:0:10.0.1" to null,
                // Test method, adjacent .
                "0:0:0:0:0:0:10.0.0..1" to null,
                // Empty contents
                "" to null,
                // Invalid single compression
                ":" to null,
                ":::" to null,
                // Trailing : (max number of : = 8)
                "2001:0:4136:e378:8000:63bf:3fff:fdd2:" to null,
                // Leading : (max number of : = 8)
                ":aaaa:bbbb:cccc:dddd:eeee:ffff:1111:2222" to null,
                // Invalid character
                "1234:2345:3456:4567:5678:6789::X890" to null,
                // Trailing . in IPv4
                "::ffff:255.255.255.255." to null,
                // To many characters in IPv4
                "::ffff:0.0.1111.0" to null,
                // Test method, adjacent .
                "::ffff:0.0..0" to null,
                // Not enough IPv4 entries trailing .
                "::ffff:127.0.0." to null,
                // Invalid trailing IPv4 character
                "::ffff:127.0.0.a" to null,
                // Invalid leading IPv4 character
                "::ffff:a.0.0.1" to null,
                // Invalid middle IPv4 character
                "::ffff:127.a.0.1" to null,
                // Invalid middle IPv4 character
                "::ffff:127.0.a.1" to null,
                // Not enough IPv4 entries no trailing .
                "::ffff:1.2.4" to null,
                // Extra IPv4 entry
                "::ffff:192.168.0.1.255" to null,
                // Not enough IPv6 content
                ":ffff:192.168.0.1.255" to null,
                // Intermixed IPv4 and IPv6 symbols
                "::ffff:255.255:255.255." to null,
                // Invalid IPv4 mapped address - invalid ipv4 separator
                "0:0:0::0:0:00f.0.0.1" to null,
                // Invalid IPv4 mapped address - not enough f's
                "0:0:0:0:0:fff:1.0.0.1" to null,
                // Invalid IPv4 mapped address - not IPv4 mapped, not IPv4 compatible
                "0:0:0:0:0:ff00:1.0.0.1" to null,
                // Invalid IPv4 mapped address - not IPv4 mapped, not IPv4 compatible
                "0:0:0:0:0:ff:1.0.0.1" to null,
                // Invalid IPv4 mapped address - too many f's
                "0:0:0:0:0:fffff:1.0.0.1" to null,
                // Invalid IPv4 mapped address - too many bytes (too many 0's)
                "0:0:0:0:0:0:ffff:1.0.0.1" to null,
                // Invalid IPv4 mapped address - too many bytes (too many 0's)
                "::0:0:0:0:0:ffff:1.0.0.1" to null,
                // Invalid IPv4 mapped address - too many bytes (too many 0's)
                "0:0:0:0:0:0::1.0.0.1" to null,
                // Invalid IPv4 mapped address - too many bytes (too many 0's)
                "0:0:0:0:0:00000:1.0.0.1" to null,
                // Invalid IPv4 mapped address - too few bytes (not enough 0's)
                "0:0:0:0:ffff:1.0.0.1" to null,
                // Invalid IPv4 mapped address - too few bytes (not enough 0's)
                "ffff:192.168.0.1" to null,
                // Invalid IPv4 mapped address - 0's after the mapped ffff indicator
                "0:0:0:0:0:ffff::10.0.0.1" to null,
                // Invalid IPv4 mapped address - 0's after the mapped ffff indicator
                "0:0:0:0:ffff::10.0.0.1" to null,
                // Invalid IPv4 mapped address - 0's after the mapped ffff indicator
                "0:0:0:ffff::10.0.0.1" to null,
                // Invalid IPv4 mapped address - 0's after the mapped ffff indicator
                "0:0:ffff::10.0.0.1" to null,
                // Invalid IPv4 mapped address - 0's after the mapped ffff indicator
                "0:ffff::10.0.0.1" to null,
                // Invalid IPv4 mapped address - 0's after the mapped ffff indicator
                "ffff::10.0.0.1" to null,
                // Invalid IPv4 mapped address - not all 0's before the mapped separator
                "1:0:0:0:0:ffff:10.0.0.1" to null,
                // Address that is similar to IPv4 mapped, but is invalid
                "0:0:0:0:ffff:ffff:1.0.0.1" to null,
                // Valid number of separators, but invalid IPv4 format
                "::1:2:3:4:5:6.7.8.9" to null,
                // Too many digits
                "0:0:0:0:0:0:ffff:10.0.0.1" to null,
                // Invalid IPv4 format
                ":1.2.3.4" to null,
                // Invalid IPv4 format
                "::.2.3.4" to null,
                // Invalid IPv4 format
                "::ffff:0.1.2." to null)


        private val ipv6ToAddressStrings = mapOf(
                // From the RFC 5952 http://tools.ietf.org/html/rfc5952#section-4
                byteArrayOf(
                        32, 1, 13, -72,
                        0, 0, 0, 0,
                        0, 0, 0, 0,
                        0, 0, 0, 1
                ) to "2001:db8::1",

                byteArrayOf(
                        32, 1, 13, -72,
                        0, 0, 0, 0,
                        0, 0, 0, 0,
                        0, 2, 0, 1
                ) to "2001:db8::2:1",

                byteArrayOf(
                        32, 1, 13, -72,
                        0, 0, 0, 1,
                        0, 1, 0, 1,
                        0, 1, 0, 1
                ) to "2001:db8:0:1:1:1:1:1",

                // Other examples
                byteArrayOf(
                        32, 1, 13, -72,
                        0, 0, 0, 0,
                        0, 0, 0, 0,
                        0, 2, 0, 1
                ) to "2001:db8::2:1",

                byteArrayOf(
                        32, 1, 0, 0,
                        0, 0, 0, 1,
                        0, 0, 0, 0,
                        0, 0, 0, 1
                ) to "2001:0:0:1::1",

                byteArrayOf(
                        32, 1, 13, -72,
                        0, 0, 0, 0,
                        0, 1, 0, 0,
                        0, 0, 0, 1
                ) to "2001:db8::1:0:0:1",

                byteArrayOf(
                        32, 1, 13, -72,
                        0, 0, 0, 0,
                        0, 1, 0, 0,
                        0, 0, 0, 0
                ) to "2001:db8:0:0:1::",

                byteArrayOf(
                        32, 1, 13, -72,
                        0, 0, 0, 0,
                        0, 0, 0, 0,
                        0, 2, 0, 0
                ) to "2001:db8::2:0",

                byteArrayOf(
                        0, 0, 0, 0,
                        0, 0, 0, 0,
                        0, 0, 0, 0,
                        0, 0, 0, 1
                ) to "::1",

                byteArrayOf(
                        0, 0, 0, 0,
                        0, 0, 0, 1,
                        0, 0, 0, 0,
                        0, 0, 0, 1
                ) to "::1:0:0:0:1",

                byteArrayOf(
                        0, 0, 0, 0,
                        1, 0, 0, 1,
                        0, 0, 0, 0,
                        1, 0, 0, 0
                ) to "::100:1:0:0:100:0",

                byteArrayOf(
                        32, 1, 0, 0,
                        65, 54, -29, 120,
                        -128, 0, 99, -65,
                        63, -1, -3, -46
                ) to "2001:0:4136:e378:8000:63bf:3fff:fdd2",

                byteArrayOf(
                        -86, -86, -69, -69,
                        -52, -52, -35, -35,
                        -18, -18, -1, -1,
                        17, 17, 34, 34
                ) to "aaaa:bbbb:cccc:dddd:eeee:ffff:1111:2222",

                byteArrayOf(
                        0, 0, 0, 0,
                        0, 0, 0, 0,
                        0, 0, 0, 0,
                        0, 0, 0, 0
                ) to "::"
        )


        private val ipv4MappedToIPv6AddressStrings = mapOf(
                // IPv4 addresses
                "255.255.255.255" to "::ffff:255.255.255.255",
                "0.0.0.0" to "::ffff:0.0.0.0",
                "127.0.0.1" to "::ffff:127.0.0.1",
                "1.2.3.4" to "::ffff:1.2.3.4",
                "192.168.0.1" to "::ffff:192.168.0.1",
                // IPv4 compatible addresses are deprecated [1], so we don't support outputting them, but we do support
                // parsing them into IPv4 mapped addresses. These values are treated the same as a plain IPv4 address above.
                // [1] https://tools.ietf.org/html/rfc4291#section-2.5.5.1
                "0:0:0:0:0:0:255.254.253.252" to "::ffff:255.254.253.252",
                "0:0:0:0:0::1.2.3.4" to "::ffff:1.2.3.4",
                "0:0:0:0::1.2.3.4" to "::ffff:1.2.3.4",
                "::0:0:0:0:0:1.2.3.4" to "::ffff:1.2.3.4",
                "0::0:0:0:0:1.2.3.4" to "::ffff:1.2.3.4",
                "0:0::0:0:0:1.2.3.4" to "::ffff:1.2.3.4",
                "0:0:0::0:0:1.2.3.4" to "::ffff:1.2.3.4",
                "0:0:0:0::0:1.2.3.4" to "::ffff:1.2.3.4",
                "0:0:0:0:0::1.2.3.4" to "::ffff:1.2.3.4",
                "::0:0:0:0:1.2.3.4" to "::ffff:1.2.3.4",
                "0::0:0:0:1.2.3.4" to "::ffff:1.2.3.4",
                "0:0::0:0:1.2.3.4" to "::ffff:1.2.3.4",
                "0:0:0::0:1.2.3.4" to "::ffff:1.2.3.4",
                "0:0:0:0::1.2.3.4" to "::ffff:1.2.3.4",
                "::0:0:0:0:1.2.3.4" to "::ffff:1.2.3.4",
                "0::0:0:0:1.2.3.4" to "::ffff:1.2.3.4",
                "0:0::0:0:1.2.3.4" to "::ffff:1.2.3.4",
                "0:0:0::0:1.2.3.4" to "::ffff:1.2.3.4",
                "0:0:0:0::1.2.3.4" to "::ffff:1.2.3.4",
                "::0:0:0:1.2.3.4" to "::ffff:1.2.3.4",
                "0::0:0:1.2.3.4" to "::ffff:1.2.3.4",
                "0:0::0:1.2.3.4" to "::ffff:1.2.3.4",
                "0:0:0::1.2.3.4" to "::ffff:1.2.3.4",
                "::0:0:1.2.3.4" to "::ffff:1.2.3.4",
                "0::0:1.2.3.4" to "::ffff:1.2.3.4",
                "0:0::1.2.3.4" to "::ffff:1.2.3.4",
                "::0:1.2.3.4" to "::ffff:1.2.3.4",
                "::1.2.3.4" to "::ffff:1.2.3.4",
                // IPv4 mapped (fully specified)
                "0:0:0:0:0:ffff:1.2.3.4" to "::ffff:1.2.3.4",
                // IPv6 addresses
                // Fully specified
                "2001:0:4136:e378:8000:63bf:3fff:fdd2" to "2001:0:4136:e378:8000:63bf:3fff:fdd2",
                "aaaa:bbbb:cccc:dddd:eeee:ffff:1111:2222" to "aaaa:bbbb:cccc:dddd:eeee:ffff:1111:2222",
                "0:0:0:0:0:0:0:0" to "::",
                "0:0:0:0:0:0:0:1" to "::1",
                // Compressing at the beginning
                "::1:0:0:0:1" to "::1:0:0:0:1",
                "::1:ffff:ffff" to "::1:ffff:ffff",
                "::" to "::",
                "::1" to "::1",
                "::ffff" to "::ffff",
                "::ffff:0" to "::ffff:0",
                "::ffff:ffff" to "::ffff:ffff",
                "::0987:9876:8765" to "::987:9876:8765",
                "::0987:9876:8765:7654" to "::987:9876:8765:7654",
                "::0987:9876:8765:7654:6543" to "::987:9876:8765:7654:6543",
                "::0987:9876:8765:7654:6543:5432" to "::987:9876:8765:7654:6543:5432",
                // Note the compression is removed (rfc 5952 section 4.2.2)
                "::0987:9876:8765:7654:6543:5432:3210" to "0:987:9876:8765:7654:6543:5432:3210",
                // Compressing at the end
                // Note the compression is removed (rfc 5952 section 4.2.2)
                "2001:db8:abcd:bcde:cdef:def1:ef12::" to "2001:db8:abcd:bcde:cdef:def1:ef12:0",
                "2001:db8:abcd:bcde:cdef:def1::" to "2001:db8:abcd:bcde:cdef:def1::",
                "2001:db8:abcd:bcde:cdef::" to "2001:db8:abcd:bcde:cdef::",
                "2001:db8:abcd:bcde::" to "2001:db8:abcd:bcde::",
                "2001:db8:abcd::" to "2001:db8:abcd::",
                "2001:1234::" to "2001:1234::",
                "2001::" to "2001::",
                "0::" to "::",
                // Compressing in the middle
                "1234:2345::7890" to "1234:2345::7890",
                "1234::2345:7890" to "1234::2345:7890",
                "1234:2345:3456::7890" to "1234:2345:3456::7890",
                "1234:2345::3456:7890" to "1234:2345::3456:7890",
                "1234::2345:3456:7890" to "1234::2345:3456:7890",
                "1234:2345:3456:4567::7890" to "1234:2345:3456:4567::7890",
                "1234:2345:3456::4567:7890" to "1234:2345:3456::4567:7890",
                "1234:2345::3456:4567:7890" to "1234:2345::3456:4567:7890",
                "1234::2345:3456:4567:7890" to "1234::2345:3456:4567:7890",
                "1234:2345:3456:4567:5678::7890" to "1234:2345:3456:4567:5678::7890",
                "1234:2345:3456:4567::5678:7890" to "1234:2345:3456:4567::5678:7890",
                "1234:2345:3456::4567:5678:7890" to "1234:2345:3456::4567:5678:7890",
                "1234:2345::3456:4567:5678:7890" to "1234:2345::3456:4567:5678:7890",
                "1234::2345:3456:4567:5678:7890" to "1234::2345:3456:4567:5678:7890",
                // Note the compression is removed (rfc 5952 section 4.2.2)
                "1234:2345:3456:4567:5678:6789::7890" to "1234:2345:3456:4567:5678:6789:0:7890",
                // Note the compression is removed (rfc 5952 section 4.2.2)
                "1234:2345:3456:4567:5678::6789:7890" to "1234:2345:3456:4567:5678:0:6789:7890",
                // Note the compression is removed (rfc 5952 section 4.2.2)
                "1234:2345:3456:4567::5678:6789:7890" to "1234:2345:3456:4567:0:5678:6789:7890",
                // Note the compression is removed (rfc 5952 section 4.2.2)
                "1234:2345:3456::4567:5678:6789:7890" to "1234:2345:3456:0:4567:5678:6789:7890",
                // Note the compression is removed (rfc 5952 section 4.2.2)
                "1234:2345::3456:4567:5678:6789:7890" to "1234:2345:0:3456:4567:5678:6789:7890",
                // Note the compression is removed (rfc 5952 section 4.2.2)
                "1234::2345:3456:4567:5678:6789:7890" to "1234:0:2345:3456:4567:5678:6789:7890",
                // IPv4 mapped addresses
                "::ffff:255.255.255.255" to "::ffff:255.255.255.255",
                "::ffff:0.0.0.0" to "::ffff:0.0.0.0",
                "::ffff:127.0.0.1" to "::ffff:127.0.0.1",
                "::ffff:1.2.3.4" to "::ffff:1.2.3.4",
                "::ffff:192.168.0.1" to "::ffff:192.168.0.1")


        private fun assertHexDumpEquals(expected: String?, actual: ByteArray?, message: String) {
            assertEquals(message, expected, if (actual == null) null else hex(actual))
        }

        private fun hex(value: ByteArray): String {
            return Sys.bytesToHex(value).toLowerCase()
        }

        private fun unhex(value: String?): ByteArray? {
            return if (value != null) Sys.hexToBytes(value) else null
        }
    }

    @Test
    fun testLocalhost() {
        Assert.assertNotNull(IP.LOCALHOST)
    }

    @Test
    fun testLoopback() {
        Assert.assertNotNull(IP.LOOPBACK_IF)
    }

    @Test
    fun testIsValidIpV4Address() {
        for (host in validIpV4Hosts.keys) {
            Assert.assertTrue(host, IPv4.isValid(host))
        }
        for (host in invalidIpV4Hosts.keys) {
            Assert.assertFalse(host, IPv4.isValid(host))
        }
    }

    @Test
    fun testIsValidIpV6Address() {
        for (host in validIpV6Hosts.keys) {
            Assert.assertTrue(host, IPv6.isValid(host))
            if (host[0] != '[' && !host.contains("%")) {
                Assert.assertNotNull(host, IPv6.fromString(host, true))
                var hostMod = "[$host]"
                Assert.assertTrue(hostMod, IPv6.isValid(hostMod))
                hostMod = "$host%"
                Assert.assertTrue(hostMod, IPv6.isValid(hostMod))
                hostMod = "$host%eth1"
                Assert.assertTrue(hostMod, IPv6.isValid(hostMod))
                hostMod = "[$host%]"
                Assert.assertTrue(hostMod, IPv6.isValid(hostMod))
                hostMod = "[$host%1]"
                Assert.assertTrue(hostMod, IPv6.isValid(hostMod))
                hostMod = "[$host]%"
                Assert.assertFalse(hostMod, IPv6.isValid(hostMod))
                hostMod = "[$host]%1"
                Assert.assertFalse(hostMod, IPv6.isValid(hostMod))
            }
        }
        for (host in invalidIpV6Hosts.keys) {
            Assert.assertFalse(host, IPv6.isValid(host))
            Assert.assertNull(host, IPv6.fromString(host))
            var hostMod = "[$host]"
            Assert.assertFalse(hostMod, IPv6.isValid(hostMod))
            hostMod = "$host%"
            Assert.assertFalse(hostMod, IPv6.isValid(hostMod))
            hostMod = "$host%eth1"
            Assert.assertFalse(hostMod, IPv6.isValid(hostMod))
            hostMod = "[$host%]"
            Assert.assertFalse(hostMod, IPv6.isValid(hostMod))
            hostMod = "[$host%1]"
            Assert.assertFalse(hostMod, IPv6.isValid(hostMod))
            hostMod = "[$host]%"
            Assert.assertFalse(hostMod, IPv6.isValid(hostMod))
            hostMod = "[$host]%1"
            Assert.assertFalse(hostMod, IPv6.isValid(hostMod))
            hostMod = "$host]"
            Assert.assertFalse(hostMod, IPv6.isValid(hostMod))
            hostMod = "[$host"
            Assert.assertFalse(hostMod, IPv6.isValid(hostMod))
        }
    }

    @Test
    fun testCreateByteArrayFromIpAddressString() {
        for ((ip, value) in validIpV4Hosts) {
            assertHexDumpEquals(value, IPv4.toBytes(ip), ip)
        }
        for ((ip, value) in invalidIpV4Hosts) {
            assertHexDumpEquals(value, IPv4.toBytesOrNull(ip), ip)
        }
        for ((ip, value) in validIpV6Hosts) {
            assertHexDumpEquals(value, IPv6.toBytes(ip), ip)
        }
        for ((ip, value) in invalidIpV6Hosts) {
            assertHexDumpEquals(value, IPv6.toBytesOrNull(ip), ip)
        }
    }

    @Test
    @Throws(UnknownHostException::class)
    fun testBytesToIpAddress() {
        for ((key) in validIpV4Hosts) {
            assertEquals(key, IPv4.toString(IPv4.toBytes(key)))
            assertEquals(key, IPv4.toString(IPv4.toBytes(key)))
        }
        for ((key, value) in ipv6ToAddressStrings) {
            assertEquals(value, IPv6.toString(key))
        }
    }

    @Test
    @Throws(UnknownHostException::class)
    fun testIp6AddressToString() {
        for ((key, value) in ipv6ToAddressStrings) {
            assertEquals(value, IP.toString(InetAddress.getByAddress(key)))
        }
    }

    @Test
    @Throws(UnknownHostException::class)
    fun testIp4AddressToString() {
        for ((key, value) in validIpV4Hosts) {
            assertEquals(key, IP.toString(InetAddress.getByAddress(unhex(value))))
        }
    }

    @Test
    fun testIpv4MappedIp6GetByName() {
        for ((srcIp, dstIp) in ipv4MappedToIPv6AddressStrings) {
            val inet6Address: Inet6Address? = IPv6.fromString(srcIp, true)
            Assert.assertNotNull("$srcIp, $dstIp", inet6Address)
            assertEquals(srcIp, dstIp, IPv6.toString(inet6Address!!, true))
        }
    }

    @Test
    fun testInvalidIpv4MappedIp6GetByName() {
        for (host in invalidIpV4Hosts.keys) {
            Assert.assertNull(host, IPv4.fromString(host))
        }
        for (host in invalidIpV6Hosts.keys) {
            Assert.assertNull(host, IPv6.fromString(host, true))
        }
    }

    @Test
    @Throws(UnknownHostException::class)
    fun testIp6InetSocketAddressToString() {
        for ((key, value) in ipv6ToAddressStrings) {
            assertEquals("[$value]:9999", IP.toString(InetSocketAddress(InetAddress.getByAddress(key), 9999)))
        }
    }

    @Test
    @Throws(UnknownHostException::class)
    fun testIp4SocketAddressToString() {
        for ((key, value) in validIpV4Hosts) {
            assertEquals("$key:9999", IP.toString(InetSocketAddress(InetAddress.getByAddress(unhex(value)), 9999)))
        }
    }
//
//    @Test
//    fun testPing() {
//        println(Ping().run())
////        println(Executor().command("ping 1.1.1.1").readOutput().startAsShellBlocking().output.utf8())
//    }


    @Test
    fun testIntStringCycle() {
        for (host in validIpV4Hosts.keys) {
            println(host)
            val ip = IPv4.toIntUnsafe(host)
            val ipString = IPv4.toString(ip)
            Assert.assertEquals(host, ipString)
        }
    }

    @Test
    fun testMac() {
        assertEquals("12:12:12:12:12:12", Mac.toStringLowerCase(Mac.toLong("12:12:12:12:12:12")))
    }

    @Test
    fun testIsPrivate() {
        assertTrue(IPv4.isSiteLocal("192.168.23.102"))
        assertTrue(IPv4.isSiteLocal("10.10.234.102"))
        assertTrue(IPv4.isSiteLocal("172.16.45.13"))
        assertFalse(IPv4.isSiteLocal("72.66.83.240"))
    }

    @Test
    fun testIp4Range() {
        assertTrue(IPv4.isInRange("10.10.10.5", "10.10.10.10", 24))
        assertTrue(IPv4.isInRange("10.0.0.5",  "10.10.10.10", 8))
        assertFalse(IPv4.isInRange("11.0.0.5", "10.10.10.10", 8))
        assertTrue(IPv4.isInRange("11.0.0.5",  "10.10.10.10", 1))
        assertTrue(IPv4.isInRange("11.0.0.5",  "10.10.10.10", 0))
        assertFalse(IPv4.isInRange("11.0.0.5", "10.10.10.10", 32))
        assertTrue(IPv4.isInRange("10.10.10.10", "10.10.10.10", 32))
        assertTrue(IPv4.isInRange("10.10.10.10", "10.10.10.10", 31))
        assertTrue(IPv4.isInRange("10.10.10.10", "10.10.10.10", 30))
        assertTrue(IPv4.isInRange("192.168.42.14", "192.168.0.0", 16))
        assertTrue(IPv4.isInRange("192.168.0.0", "192.168.0.0", 16))
    }


    @Test
    fun testCidr() {
        assertTrue(IPv4.isValidCidr("10.10.10.5/0"))
        assertTrue(IPv4.isValidCidr("10.10.10.5/24"))
        assertTrue(IPv4.isValidCidr("10.10.10.5/32"))
        assertFalse(IPv4.isValidCidr("10.10.10.5/33"))
        assertFalse(IPv4.isValidCidr("10.10.10.5/-1"))
    }

    @Test
    fun testToString() {
        assertEquals("10.10.10.5", IPv4.toString(IPv4.toInt("10.10.10.5")))
    }
}
