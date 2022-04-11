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

import com.sun.jna.Pointer
import com.sun.jna.Structure

class SOCKADDR_IN6(p: Pointer) : Structure(p) {
    @JvmField var sin6_family: Short = 0
    @JvmField var sin6_port: Short = 0
    @JvmField var sin6_flowinfo = 0
    @JvmField var sin6_addr = ByteArray(16)
    @JvmField var sin6_scope_id = 0

    init {
        read()
    }

    override fun getFieldOrder(): List<String> {
        return listOf("sin6_family", "sin6_port", "sin6_flowinfo", "sin6_addr", "sin6_scope_id")
    }
}
