package dorkbox.netUtil.jna.windows.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public
class SOCKADDR_IN extends Structure {
    public short sin_family;
    public short sin_port;
    public byte[] sin_addr = new byte[4];
    public byte[] sin_zero = new byte[8];

    public
    SOCKADDR_IN(Pointer p) {
        super(p);
        read();
    }

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("sin_family", "sin_port", "sin_addr", "sin_zero");
    }
}
