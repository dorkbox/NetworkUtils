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

/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package dorkbox.netUtil

import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketPermission
import java.util.*

/**
 * By asserting that these operations are privileged, the operations can proceed even if some code in the calling chain lacks
 * the appropriate [SocketPermission].
 */
object SocketUtils {
    /**
     * Gets the version number.
     */
    const val version = Common.version

    private val EMPTY = Collections.enumeration(emptyList<Any>())

    @Suppress("UNCHECKED_CAST")
    private fun <T> empty(): Enumeration<T> {
        return EMPTY as Enumeration<T>
    }

    fun addressesFromNetworkInterface(intf: NetworkInterface): Enumeration<InetAddress> {
        // Android seems to sometimes return null even if this is not a valid return value by the api docs.
        // Just return an empty Enumeration in this case.
        // See https://github.com/netty/netty/issues/10045
        return intf.inetAddresses ?: empty()
    }
}
