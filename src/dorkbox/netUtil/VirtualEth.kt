package dorkbox.netUtil

import dorkbox.executor.Executor

/**
 *
 */
object VirtualEth {
    fun add(host: String, guest: String) {
        if (Common.OS_LINUX) {
            Executor().command("/sbin/ip", "link", "add", "name", host, "type", "veth", "peer", "name", guest).startBlocking()
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    fun delete(host: String) {
        if (Common.OS_LINUX) {
            Executor().command("/sbin/ip", "link", "del", host).startBlocking()
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    fun assignNameSpace(nameSpace: String, guest: String) {
        if (Common.OS_LINUX) {
            Executor().command("/sbin/ip", "link", "set", "guest", "netns", nameSpace).startBlocking()
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }
}
