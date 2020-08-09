package dorkbox.netUtil

import dorkbox.executor.Executor

/**
 *
 */
object Route {
    fun flush() {
        Executor.run("/sbin/ip", "route", "flush", "cache")
    }

    fun add(targetIpAndCidr: String, hostIP: String, hostInterface: String) {
        if (Common.OS_LINUX) {
            Executor.run("/sbin/ip", "route", "add", targetIpAndCidr, "via", hostIP, "dev", hostInterface);
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }
}
