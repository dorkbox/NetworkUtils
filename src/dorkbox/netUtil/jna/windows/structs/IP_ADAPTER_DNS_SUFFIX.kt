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

open class IP_ADAPTER_DNS_SUFFIX : Structure() {
    class ByReference : IP_ADAPTER_DNS_SUFFIX(), Structure.ByReference

    @JvmField var Next: ByReference? = null
    @JvmField var _String = CharArray(256)

    override fun getFieldOrder(): List<String> {
        return listOf("Next", "_String")
    }
}
