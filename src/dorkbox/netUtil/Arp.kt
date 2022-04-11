/*
 * Copyright 2020 dorkbox, llc
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

import dorkbox.executor.Executor
import java.io.File

object Arp {
    /**
     * Gets the version number.
     */
    const val version = Common.version

    // Now setup ARP Proxy for this interface (so ARP requests are answered correctly)
    fun proxyAdd(interfaceName: String) {
        if (Common.OS_LINUX) {
            val file = File("/proc/sys/net/ipv4/conf/$interfaceName/proxy_arp")
            if (!file.readText().contains("1")) {
                file.appendText("1")
            }
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    fun proxyDel(interfaceName: String) {
        if (Common.OS_LINUX) {
            val file = File("/proc/sys/net/ipv4/conf/$interfaceName/proxy_arp")
            file.delete()
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    fun add(interfaceName: String, targetIpAddress: String, targetMacAddress: String) {
        if (Common.OS_LINUX) {
             // have to make sure that the host interface will answer ARP for the "target" ip (normally, it will not for veth interfaces)
             Executor()
                 .command("/usr/sbin/arp", "-i", interfaceName, "-s", targetIpAddress, targetMacAddress)
                 .startBlocking()
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }
}
