package dorkbox.netUtil

import dorkbox.executor.Executor
import java.util.*

/**
 *
 */
object Iface {
    /**
     * Gets the version number.
     */
    const val version = "2.5"

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
                    return possibleAddr!!
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
