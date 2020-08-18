package dorkbox.netUtil

import dorkbox.executor.Executor
import java.io.File

/**
 *
 */
object Dhcp {
    fun start(id: String, interfaceName: String) {
        if (Common.OS_LINUX) {
            stop(id, interfaceName)
            val dhcpPidFile = "/var/run/dhclient-$id.pid"
            Executor().command("/sbin/dhclient", "-pf", dhcpPidFile, interfaceName).startBlocking()
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    fun stop(id: String, interfaceName: String) {
        if (Common.OS_LINUX) {
            val dhcpPidFile = "/var/run/dhclient-$id.pid"

            // close the dhclient if it was already running (based on pid file), and delete the pid file
             Executor().command("/sbin/dhclient", "-r -pf", dhcpPidFile, interfaceName).startBlocking()

            // short break
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            File(dhcpPidFile).delete()
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }
}
