/*
 * Copyright 2021 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dorkbox.netUtil

import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException

object Inet4 {
    /**
     * Gets the version number.
     */
    const val version = "2.8"

    /**
     * Returns the [Inet4Address] representation of a [String] IP or host address.
     *
     * @param ip [String] IP address to be converted to a [Inet4Address]
     * @return [Inet4Address] representation of the `ip` or `null` if not a valid IP address.
     */
    fun toAddress(ip: String): Inet4Address? {
        return if (IPv4.isValid(ip)) {
            IPv4.toAddressUnsafe(ip)
        } else {
            // maybe this is a host name and not an IP address?
            try {
                InetAddress.getAllByName(ip).find { it is Inet4Address } as Inet4Address
            } catch (e: UnknownHostException) {
                null
            }
        }
    }
}
