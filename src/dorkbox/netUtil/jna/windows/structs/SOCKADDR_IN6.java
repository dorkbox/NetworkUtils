package dorkbox.netUtil.jna.windows.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public
class SOCKADDR_IN6 extends Structure {
    public short sin6_family;
    public short sin6_port;
    public int sin6_flowinfo;

    public byte[] sin6_addr = new byte[16];
    public int sin6_scope_id;

    public
    SOCKADDR_IN6(Pointer p) {
        super(p);
        read();
    }

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("sin6_family", "sin6_port", "sin6_flowinfo", "sin6_addr", "sin6_scope_id");
    }
}
