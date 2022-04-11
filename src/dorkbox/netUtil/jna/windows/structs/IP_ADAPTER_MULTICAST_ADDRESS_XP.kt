/*
 * Copyright 2016 dorkbox, llc
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

package dorkbox.netUtil.jna.windows.structs

import com.sun.jna.Structure

open class IP_ADAPTER_MULTICAST_ADDRESS_XP : Structure() {
    class ByReference : IP_ADAPTER_MULTICAST_ADDRESS_XP(), Structure.ByReference

    @JvmField var Length = 0
    @JvmField var Reserved = 0
    @JvmField var Next: IP_ADAPTER_DNS_SERVER_ADDRESS_XP.ByReference? = null
    @JvmField var Address: SOCKET_ADDRESS? = null

    override fun getFieldOrder(): List<String> {
        return listOf("Length", "Reserved", "Next", "Address")
    }
}
