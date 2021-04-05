package dorkbox.netUtil.jna.windows.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

public
class IP_ADAPTER_DNS_SUFFIX extends Structure {
    public static
    class ByReference extends IP_ADAPTER_DNS_SUFFIX implements Structure.ByReference {}


    public IP_ADAPTER_DNS_SUFFIX.ByReference Next;
    public char[] _String = new char[256];

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("Next", "_String");
    }
}
