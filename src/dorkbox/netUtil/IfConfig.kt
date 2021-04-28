package dorkbox.netUtil

import dorkbox.executor.Executor

/**
 *
 */
object IfConfig {
    /**
     * Gets the version number.
     */
    const val version = "2.7"

    fun assignMac(interfaceName: String, interfaceMac: String) {
        if (Common.OS_LINUX) {
             Executor.run("/sbin/ifconfig", interfaceName, "hw", "ether", interfaceMac)
        } else {
            throw RuntimeException("NOT IMPL.")
        }

    }

    fun up(interfaceName: String, interfaceCIDR: String) {
        if (Common.OS_LINUX) {
            up(interfaceName, "0.0.0.0", interfaceCIDR)
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    fun up(interfaceName: String, interfaceIP: String, interfaceCIDR: String) {
        if (Common.OS_LINUX) {
             Executor.run("/sbin/ifconfig", interfaceName, "$interfaceIP/$interfaceCIDR", "up");
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }
}
