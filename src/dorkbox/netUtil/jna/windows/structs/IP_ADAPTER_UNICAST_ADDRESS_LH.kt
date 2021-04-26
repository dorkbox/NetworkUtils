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
