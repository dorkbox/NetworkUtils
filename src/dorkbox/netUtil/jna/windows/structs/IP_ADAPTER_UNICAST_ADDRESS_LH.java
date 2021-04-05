package dorkbox.netUtil.jna.windows.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

public
class IP_ADAPTER_UNICAST_ADDRESS_LH extends Structure {
    public static
    class ByReference extends IP_ADAPTER_UNICAST_ADDRESS_LH implements Structure.ByReference {}


    public int Length;
    public int IfIndex;

    public IP_ADAPTER_UNICAST_ADDRESS_LH.ByReference Next;
    public SOCKET_ADDRESS Address;

    public int PrefixOrigin;
    public int SuffixOrigin;
    public int DadState;

    public int ValidLifetime;
    public int PreferredLifetime;
    public int LeaseLifetime;
    public byte OnLinkPrefixLength;

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("Length",
                             "IfIndex",
                             "Next",
                             "Address",
                             "PrefixOrigin",
                             "SuffixOrigin",
                             "DadState",
                             "ValidLifetime",
                             "PreferredLifetime",
                             "LeaseLifetime",
                             "OnLinkPrefixLength");
    }
}
