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

/**
 *
 */
object Dhcp {
    /**
     * Gets the version number.
     */
    const val version = Common.version

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
