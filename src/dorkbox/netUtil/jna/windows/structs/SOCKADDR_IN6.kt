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
