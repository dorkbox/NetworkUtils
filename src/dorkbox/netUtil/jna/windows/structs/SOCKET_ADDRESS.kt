package dorkbox.netUtil.jna.windows.structs;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public
class SOCKET_ADDRESS extends Structure {
    public static final int AF_INET = 2;
    public static final int AF_INET6 = 23;

    public Pointer lpSockaddr;
    public int iSockaddrLength;

    public
    InetAddress toAddress() throws UnknownHostException {
        switch (((int) lpSockaddr.getShort(0))) {
            case AF_INET:
                SOCKADDR_IN in4 = new SOCKADDR_IN(lpSockaddr);
                return InetAddress.getByAddress(in4.sin_addr);
            case AF_INET6:
                SOCKADDR_IN6 in6 = new SOCKADDR_IN6(lpSockaddr);
                return Inet6Address.getByAddress("", in6.sin6_addr, in6.sin6_scope_id);
        }

        return null;
    }

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("lpSockaddr", "iSockaddrLength");
    }
}
