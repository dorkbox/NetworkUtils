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
