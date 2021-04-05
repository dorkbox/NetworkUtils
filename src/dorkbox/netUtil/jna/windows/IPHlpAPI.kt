package dorkbox.netUtil.jna.windows

import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import dorkbox.netUtil.jna.JnaHelper.register

/**
 * https://docs.microsoft.com/en-us/windows/win32/api/iphlpapi/
 */
object IPHlpAPI {
    init {
        register("IPHlpAPI", IPHlpAPI::class.java)
    }

    const val AF_UNSPEC = 0
    const val AF_INET = 2
    const val AF_INET6 = 23
    const val GAA_FLAG_SKIP_UNICAST = 0x0001
    const val GAA_FLAG_SKIP_ANYCAST = 0x0002
    const val GAA_FLAG_SKIP_MULTICAST = 0x0004
    const val GAA_FLAG_SKIP_DNS_SERVER = 0x0008
    const val GAA_FLAG_INCLUDE_PREFIX = 0x0010
    const val GAA_FLAG_SKIP_FRIENDLY_NAME = 0x0020
    const val GAA_FLAG_INCLUDE_WINS_INFO = 0x0040
    const val GAA_FLAG_INCLUDE_GATEWAYS = 0x0080
    const val GAA_FLAG_INCLUDE_ALL_INTERFACES = 0x0100
    const val GAA_FLAG_INCLUDE_ALL_COMPARTMENTS = 0x0200
    const val GAA_FLAG_INCLUDE_TUNNEL_BINDINGORDER = 0x0400

    external fun GetAdaptersAddresses(
        family: Int,
        flags: Int,
        reserved: Pointer?,
        adapterAddresses: Pointer?,
        sizePointer: IntByReference?
    ): Int
}
