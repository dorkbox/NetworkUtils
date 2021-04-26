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
