package dorkbox.netUtil.jna.windows.structs

import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import com.sun.jna.platform.win32.Guid

open class IP_ADAPTER_ADDRESSES_LH : Structure {
    constructor(p: Pointer) : super(p) {
        read()
    }

    constructor() {}

    class ByReference : IP_ADAPTER_ADDRESSES_LH(), Structure.ByReference

    @JvmField var Length = 0
    @JvmField var IfIndex = 0
    @JvmField var Next: ByReference? = null
    @JvmField var AdapterName: String? = null

    @JvmField var FirstUnicastAddress: IP_ADAPTER_UNICAST_ADDRESS_LH.ByReference? = null
    @JvmField var FirstAnycastAddress: IP_ADAPTER_ANYCAST_ADDRESS_XP.ByReference? = null

    @JvmField var FirstMulticastAddress: IP_ADAPTER_MULTICAST_ADDRESS_XP.ByReference? = null
    @JvmField var FirstDnsServerAddress: IP_ADAPTER_DNS_SERVER_ADDRESS_XP.ByReference? = null

    @JvmField var DnsSuffix: WString? = null
    @JvmField var Description: WString? = null
    @JvmField var FriendlyName: WString? = null
    @JvmField var PhysicalAddress = ByteArray(8)
    @JvmField var PhysicalAddressLength = 0

    @JvmField var Flags = 0
    @JvmField var Mtu = 0
    @JvmField var IfType = 0
    @JvmField var OperStatus = 0
    @JvmField var Ipv6IfIndex = 0

    @JvmField var ZoneIndices = IntArray(16)

    @JvmField var FirstPrefix: Pointer? = null
    @JvmField var TransmitLinkSpeed: Long = 0
    @JvmField var ReceiveLinkSpeed: Long = 0

    @JvmField var FirstWinsServerAddress: Pointer? = null
    @JvmField var FirstGatewayAddress: Pointer? = null

    @JvmField var Ipv4Metric = 0
    @JvmField var Ipv6Metric = 0

    @JvmField var Luid: LUID? = null
    @JvmField var Dhcpv4Server: SOCKET_ADDRESS? = null
    @JvmField var CompartmentId = 0
    @JvmField var NetworkGuid: Guid.GUID? = null
    @JvmField var ConnectionType = 0
    @JvmField var TunnelType = 0

    @JvmField var Dhcpv6Server: SOCKET_ADDRESS? = null
    @JvmField var Dhcpv6ClientDuid = ByteArray(130)
    @JvmField var Dhcpv6ClientDuidLength = 0
    @JvmField var Dhcpv6Iaid = 0

    @JvmField var FirstDnsSuffix: IP_ADAPTER_DNS_SUFFIX.ByReference? = null

    override fun getFieldOrder(): List<String> {
        return listOf(
            "Length",
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
            "FirstDnsSuffix"
        )
    }
}
