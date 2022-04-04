/*
 * Copyright 2015 The Netty Project
 * Copyright 2021 dorkbox, llc
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package dorkbox.netUtil.dnsUtils

import dorkbox.netUtil.Common.OS_WINDOWS
import dorkbox.netUtil.dnsUtils.HostsFileParser.parse
import java.net.InetAddress
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Will resolve host file entries only once, from the `hosts` file.
 */
object DefaultHostsFileResolver {
    private val parseEntries: HostsFileEntries by lazy {
        if (OS_WINDOWS) {
            // Only windows there seems to be no standard for the encoding used for the hosts file, so let us
            // try multiple until we either were able to parse it or there is none left and so we return an
            // empty instance.
            parse(
                Charset.defaultCharset(),
                StandardCharsets.UTF_16,
                StandardCharsets.UTF_8
            )
        } else {
            parse(Charset.defaultCharset())
        }
    }

    fun address(inetHost: String, resolvedAddressTypes: ResolvedAddressTypes): InetAddress? {
        val normalized = normalize(inetHost)

        return when (resolvedAddressTypes) {
            ResolvedAddressTypes.IPV4_ONLY -> parseEntries.ipv4Entries[normalized]
            ResolvedAddressTypes.IPV6_ONLY -> parseEntries.ipv6Entries[normalized]
            ResolvedAddressTypes.IPV4_PREFERRED -> {
                parseEntries.ipv4Entries[normalized]  ?: parseEntries.ipv6Entries[normalized]
            }
            ResolvedAddressTypes.IPV6_PREFERRED -> {
                parseEntries.ipv6Entries[normalized] ?: parseEntries.ipv4Entries[normalized]
            }
        }
    }

    private fun normalize(inetHost: String): String {
        return inetHost.lowercase(Locale.ENGLISH)
    }
}
