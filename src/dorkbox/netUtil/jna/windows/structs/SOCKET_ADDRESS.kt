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
