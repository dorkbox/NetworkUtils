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
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

class SOCKET_ADDRESS : Structure() {
    companion object {
        const val AF_INET = 2
        const val AF_INET6 = 23
    }

    @JvmField var lpSockaddr: Pointer? = null
    @JvmField var iSockaddrLength = 0


    @Throws(UnknownHostException::class)
    fun toAddress(): InetAddress? {
        when (lpSockaddr!!.getShort(0).toInt()) {
            AF_INET -> {
                val in4 = SOCKADDR_IN(lpSockaddr!!)
                return InetAddress.getByAddress(in4.sin_addr)
            }
            AF_INET6 -> {
                val in6 = SOCKADDR_IN6(lpSockaddr!!)
                return Inet6Address.getByAddress("", in6.sin6_addr, in6.sin6_scope_id)
            }
        }

        return null
    }

    override fun getFieldOrder(): List<String> {
        return listOf("lpSockaddr", "iSockaddrLength")
    }
}
