package dorkbox.netUtil.jna.windows.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

public
class IP_ADAPTER_DNS_SERVER_ADDRESS_XP extends Structure {
    public static
    class ByReference extends IP_ADAPTER_DNS_SERVER_ADDRESS_XP implements Structure.ByReference {}


    public int Length;
    public int Reserved;

    public IP_ADAPTER_DNS_SERVER_ADDRESS_XP.ByReference Next;
    public SOCKET_ADDRESS Address;

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("Length", "Reserved", "Next", "Address");
    }
}
