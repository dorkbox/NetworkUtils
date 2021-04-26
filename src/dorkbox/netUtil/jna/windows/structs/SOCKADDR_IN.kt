package dorkbox.netUtil.jna.windows.structs

import com.sun.jna.Pointer
import com.sun.jna.Structure

class SOCKADDR_IN(p: Pointer) : Structure(p) {
    @JvmField var sin_family: Short = 0
    @JvmField var sin_port: Short = 0

    @JvmField var sin_addr = ByteArray(4)
    @JvmField var sin_zero = ByteArray(8)

    init {
        read()
    }

    override fun getFieldOrder(): List<String> {
        return listOf("sin_family", "sin_port", "sin_addr", "sin_zero")
    }
}
