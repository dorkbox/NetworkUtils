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

object VirtualEth {
    /**
     * Gets the version number.
     */
    const val version = Common.version

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
            Executor().command("/sbin/ip", "link", "set", guest, "netns", nameSpace).startBlocking()
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }
}
