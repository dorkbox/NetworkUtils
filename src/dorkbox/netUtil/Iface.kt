/*
 * Copyright 2026 dorkbox, llc
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

/**
 *
 */
object Iface {
    /**
     * Gets the version number.
     */
    const val version = Common.version

    private val ifToIp: MutableMap<String, String> = HashMap()

    /**
     * On disconnect, it will get the IP address for the interface from the cache, instead of from `ifconfig` (since the interface
     * is down at this point)
     */
    fun getIpFromIf(interfaceName: String, isOnClientConnect: Boolean): String {
        if (Common.OS_LINUX) {
            if (isOnClientConnect) {
                val ifaceInfo = Executor.run("/sbin/ifconfig", interfaceName)

                val str = "inet addr:"
                var index = ifaceInfo.indexOf(str)
                if (index > -1) {
                    index += str.length
                    val possibleAddr = ifaceInfo.substring(index, ifaceInfo.indexOf(" ", index));

                    Common.logger.debug("Found on '{}' possible addr '{}' : ADD", interfaceName, possibleAddr);
                    synchronized(ifToIp) {
                        ifToIp.put(interfaceName, possibleAddr);
                    }
                    return possibleAddr;
                }
            }
            else {
                var possibleAddr: String?
                synchronized(ifToIp) { possibleAddr = ifToIp.remove(interfaceName) }

                Common.logger.debug("Found on '{}' possible addr '{}' : REMOVE", interfaceName, possibleAddr)
                if (possibleAddr != null) {
                    return possibleAddr
                }
            }
        }
        else {
            throw RuntimeException("NOT IMPL.")
        }

        return ""
    }

    fun down(interfaceName: String) {
        if (Common.OS_LINUX) {
            Executor.run("/sbin/ip", "link", "set", "dev", interfaceName, "down")
        }
        else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    fun up(interfaceName: String) {
        if (Common.OS_LINUX) {
            Executor.run("/sbin/ip", "link", "set", "dev", interfaceName, "up")
        }
        else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    fun setMac(interfaceName: String, interfaceMac: String) {
        if (Common.OS_LINUX) {
            Executor.run("/sbin/ip", "link", "set", "dev", interfaceName, "address", interfaceMac)
        }
        else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    fun assignCIDR(interfaceName: String?, cidr: Int) {
        if (Common.OS_LINUX) {
            Executor.run("/sbin/ifconfig", "$interfaceName 0.0.0.0/$cidr up")
        }
        else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    fun addLoopback() {
        if (Common.OS_LINUX) {
            Executor.Companion.run("/sbin/ip", "link", "set", "dev", "lo", "up")
            Executor.Companion.run("/sbin/ip", "addr", "add", "127.0.0.1", "dev", "lo")
        }
        else {
            throw RuntimeException("NOT IMPL.")
        }
    }
}
