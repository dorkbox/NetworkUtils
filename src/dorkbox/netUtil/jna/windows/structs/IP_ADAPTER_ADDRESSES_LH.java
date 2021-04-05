package dorkbox.netUtil.jna.windows.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid;

public
class IP_ADAPTER_ADDRESSES_LH extends Structure {
    public static
    class ByReference extends IP_ADAPTER_ADDRESSES_LH implements Structure.ByReference {}


    public int Length;
    public int IfIndex;

    public IP_ADAPTER_ADDRESSES_LH.ByReference Next;
    public String AdapterName;

    public IP_ADAPTER_UNICAST_ADDRESS_LH.ByReference FirstUnicastAddress;
    public IP_ADAPTER_ANYCAST_ADDRESS_XP.ByReference FirstAnycastAddress;
    public IP_ADAPTER_MULTICAST_ADDRESS_XP.ByReference FirstMulticastAddress;
    public IP_ADAPTER_DNS_SERVER_ADDRESS_XP.ByReference FirstDnsServerAddress;

    public WString DnsSuffix;
    public WString Description;
    public WString FriendlyName;

    public byte[] PhysicalAddress = new byte[8];
    public int PhysicalAddressLength;
    public int Flags;
    public int Mtu;
    public int IfType;
    public int OperStatus;
    public int Ipv6IfIndex;

    public int[] ZoneIndices = new int[16];
    public Pointer FirstPrefix;

    public long TransmitLinkSpeed;
    public long ReceiveLinkSpeed;

    public Pointer FirstWinsServerAddress;
    public Pointer FirstGatewayAddress;

    public int Ipv4Metric;
    public int Ipv6Metric;

    public LUID Luid;
    public SOCKET_ADDRESS Dhcpv4Server;
    public int CompartmentId;
    public Guid.GUID NetworkGuid;

    public int ConnectionType;
    public int TunnelType;
    public SOCKET_ADDRESS Dhcpv6Server;

    public byte[] Dhcpv6ClientDuid = new byte[130];
    public int Dhcpv6ClientDuidLength;
    public int Dhcpv6Iaid;

    public IP_ADAPTER_DNS_SUFFIX.ByReference FirstDnsSuffix;

    public
    IP_ADAPTER_ADDRESSES_LH(Pointer p) {
        super(p);
        read();
    }

    public
    IP_ADAPTER_ADDRESSES_LH() {}

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("Length",
                             "IfIndex",
                             "Next",
                             "AdapterName",
                             "FirstUnicastAddress",
                             "FirstAnycastAddress",
                             "FirstMulticastAddress",
                             "FirstDnsServerAddress",
                             "DnsSuffix",
                             "Description",
                             "FriendlyName",
                             "PhysicalAddress",
                             "PhysicalAddressLength",
                             "Flags",
                             "Mtu",
                             "IfType",
                             "OperStatus",
                             "Ipv6IfIndex",
                             "ZoneIndices",
                             "FirstPrefix",
                             "TransmitLinkSpeed",
                             "ReceiveLinkSpeed",
                             "FirstWinsServerAddress",
                             "FirstGatewayAddress",
                             "Ipv4Metric",
                             "Ipv6Metric",
                             "Luid",
                             "Dhcpv4Server",
                             "CompartmentId",
                             "NetworkGuid",
                             "ConnectionType",
                             "TunnelType",
                             "Dhcpv6Server",
                             "Dhcpv6ClientDuid",
                             "Dhcpv6ClientDuidLength",
                             "Dhcpv6Iaid",
                             "FirstDnsSuffix");
    }
}


