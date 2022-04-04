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

import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

object Inet6 {
    /**
     * Gets the version number.
     */
    const val version = Common.version

    /**
     * Returns the [Inet6Address] representation of a [String] IP or host address.
     * <p>
     * The [ipv4Mapped] parameter specifies how IPv4 addresses should be treated. The "IPv4 mapped" format is
     * defined in <a href="http://tools.ietf.org/html/rfc4291#section-2.5.5">rfc 4291 section 2</a> is supported.
     *
     * @param ip [String] IP address to be converted to a [Inet6Address]
     * @param ipv4Mapped
     *
     *   * [true] To allow IPv4 mapped inputs to be translated into {@link Inet6Address}</li>
     *   * [false] Consider IPv4 mapped addresses as invalid.</li>
     *
     * @return [Inet6Address] representation of the [ip] or [null] if not a valid IP address.
     */
    fun toAddress(ip: String, ipv4Mapped: Boolean = true): Inet6Address? {
        return if (IPv6.isValid(ip)) {
            IPv6.toAddress(ip, ipv4Mapped)
        } else {
            // maybe this is a host name and not an IP address?
            try {
                InetAddress.getAllByName(ip).find { it is Inet6Address } as Inet6Address
            } catch (e: UnknownHostException) {
                null
            }
        }
    }
}
