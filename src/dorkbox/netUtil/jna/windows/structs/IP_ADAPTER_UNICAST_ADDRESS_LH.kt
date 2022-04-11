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

open class IP_ADAPTER_UNICAST_ADDRESS_LH : Structure() {
    class ByReference : IP_ADAPTER_UNICAST_ADDRESS_LH(), Structure.ByReference

    @JvmField var Length = 0
    @JvmField var IfIndex = 0
    @JvmField var Next: ByReference? = null
    @JvmField var Address: SOCKET_ADDRESS? = null
    @JvmField var PrefixOrigin = 0
    @JvmField var SuffixOrigin = 0
    @JvmField var DadState = 0
    @JvmField var ValidLifetime = 0
    @JvmField var PreferredLifetime = 0
    @JvmField var LeaseLifetime = 0
    @JvmField var OnLinkPrefixLength: Byte = 0

    override fun getFieldOrder(): List<String> {
        return listOf(
            "Length",
            "IfIndex",
            "Next",
            "Address",
            "PrefixOrigin",
            "SuffixOrigin",
            "DadState",
            "ValidLifetime",
            "PreferredLifetime",
            "LeaseLifetime",
            "OnLinkPrefixLength"
        )
    }
}
