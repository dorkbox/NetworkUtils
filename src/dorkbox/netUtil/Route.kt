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

/**
 *
 */
object Route {
    /**
     * Gets the version number.
     */
    const val version = Common.version

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
