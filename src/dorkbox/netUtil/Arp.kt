package dorkbox.netUtil

import dorkbox.executor.Executor
import java.io.File

object Arp {
    /**
     * Gets the version number.
     */
    const val version = "2.2"

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
