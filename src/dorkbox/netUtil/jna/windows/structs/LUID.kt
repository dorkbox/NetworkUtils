package dorkbox.netUtil.jna.windows.structs

import com.sun.jna.Structure

class LUID : Structure() {
    @JvmField var LowPart = 0
    @JvmField var HighPart = 0

    override fun getFieldOrder(): List<String> {
        return listOf("LowPart", "HighPart")
    }
}
