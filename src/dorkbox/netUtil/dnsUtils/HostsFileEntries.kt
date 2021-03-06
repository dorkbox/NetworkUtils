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

import java.net.Inet4Address
import java.net.Inet6Address

/**
 * A container of hosts file entries
 */
data class HostsFileEntries(val ipv4Entries: Map<String, Inet4Address> = emptyMap(),
                            val ipv6Entries: Map<String, Inet6Address> = emptyMap()) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HostsFileEntries

        if (ipv4Entries != other.ipv4Entries) return false
        if (ipv6Entries != other.ipv6Entries) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ipv4Entries.hashCode()
        result = 31 * result + ipv6Entries.hashCode()
        return result
    }
}
