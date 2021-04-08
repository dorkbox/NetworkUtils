package dorkbox.netUtil

import dorkbox.netUtil.Common.logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.*
import java.util.regex.Pattern

/**
 * A class that holds a number of network-related constants, also from:
 * (Netty, apache 2.0 license)
 * https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/NetUtil.java
 *
 * This class borrowed some of its methods from a modified fork of the
 * [Inet6Util class]
 * (http://svn.apache.org/repos/asf/harmony/enhanced/java/branches/java6/classlib/modules/luni/src/main/java/org/apache/harmony/luni/util/Inet6Util.java) which was part of Apache Harmony.
 */
object IP {

    /**
     * Gets the version number.
     */
    const val version = "2.2"

    /**
     * The [InetAddress] that represents the loopback address. If IPv6 stack is available, it will refer to
     * [.LOCALHOST6].  Otherwise, [.LOCALHOST4].
     */
    val LOCALHOST: InetAddress

    /**
     * The loopback [NetworkInterface] of the current machine
     */
    val LOOPBACK_IF: NetworkInterface

    init {
        logger.trace {
            "-Djava.net.preferIPv4Stack: ${IPv4.isPreferred}"
        }
        logger.trace {
            "-Djava.net.preferIPv6Addresses: ${IPv6.isPreferred}"
        }

        // Retrieve the list of available network interfaces.
        val netInterfaces = mutableListOf<NetworkInterface>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    val iface: NetworkInterface = interfaces.nextElement()
                    // Use the interface with proper INET addresses only.
                    if (SocketUtils.addressesFromNetworkInterface(iface).hasMoreElements()) {
                        netInterfaces.add(iface)
                    }
                }
            }
        } catch (e: SocketException) {
            logger.warn("Failed to retrieve the list of available network interfaces", e)
        }


        // Find the first loopback interface available from its INET address (127.0.0.1 or ::1)
        // Note that we do not use NetworkInterface.isLoopback() in the first place because it takes long time
        // on a certain environment. (e.g. Windows with -Djava.net.preferIPv4Stack=true)

        var loopbackIface: NetworkInterface? = null
        var loopbackAddr: InetAddress? = null

        loop@ for (iface in netInterfaces) {
            val i = SocketUtils.addressesFromNetworkInterface(iface)
            while (i.hasMoreElements()) {
                val addr: InetAddress = i.nextElement()
                if (addr.isLoopbackAddress) {
                    // Found
                    loopbackIface = iface
                    loopbackAddr = addr
                    break@loop
                }
            }
        }

        // If failed to find the loopback interface from its INET address, fall back to isLoopback().
        if (loopbackIface == null) {
            try {
                for (iface in netInterfaces) {
                    if (iface.isLoopback) {
                        val i = SocketUtils.addressesFromNetworkInterface(iface)
                        if (i.hasMoreElements()) {
                            // Found the one with INET address.
                            loopbackIface = iface
                            loopbackAddr = i.nextElement()
                            break
                        }
                    }
                }
                if (loopbackIface == null) {
                    logger.warn("Failed to find the loopback interface")
                }
            } catch (e: SocketException) {
                logger.warn("Failed to find the loopback interface", e)
            }
        }

        if (loopbackIface != null) {
            // Found the loopback interface with an INET address.
            logger.trace {
                "Loopback interface: ${loopbackIface.name} (${loopbackIface.displayName}, ${loopbackAddr!!.hostAddress})"
            }
        } else {
            // Could not find the loopback interface, but we can't leave LOCALHOST as null.
            // Use LOCALHOST6 or LOCALHOST4, preferably the IPv6 one.
            if (loopbackAddr == null) {
                try {
                    if (NetworkInterface.getByInetAddress(IPv6.LOCALHOST) != null) {
                        logger.debug {
                            "Using hard-coded IPv6 localhost address: ${IPv6.LOCALHOST}"
                        }

                        loopbackAddr = IPv6.LOCALHOST
                    }
                } catch (ignore: Exception) {

                }
                if (loopbackAddr == null) {
                    logger.debug {
                        "Using hard-coded IPv4 localhost address: ${IPv4.LOCALHOST}"
                    }
                    loopbackAddr = IPv4.LOCALHOST
                }
            }
        }

        LOOPBACK_IF = loopbackIface!!
        LOCALHOST = loopbackAddr!!
    }

    /**
     * The LAN address of the machine, to the best of our ability
     */
    fun lanAddress(): InetAddress {
        /**
         * Returns an `InetAddress` object encapsulating what is most likely the machine's LAN IP address.
         *
         *
         * This method is intended for use as a replacement of JDK method `InetAddress.getLocalHost`, because
         * that method is ambiguous on Linux systems. Linux systems enumerate the loopback network interface the same
         * way as regular LAN network interfaces, but the JDK `InetAddress.getLocalHost` method does not
         * specify the algorithm used to select the address returned under such circumstances, and will often return the
         * loopback address, which is not valid for network communication. Details
         * [here](http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037).
         *
         *
         * This method will scan all IP addresses on all network interfaces on the host machine to determine the IP address
         * most likely to be the machine's LAN address. If the machine has multiple IP addresses, this method will prefer
         * a site-local IP address (e.g. 192.168.x.x or 10.10.x.x, usually IPv4) if the machine has one (and will return the
         * first site-local address if the machine has more than one), but if the machine does not hold a site-local
         * address, this method will return simply the first non-loopback address found (IPv4 or IPv6).
         *
         *
         * If this method cannot find a non-loopback address using this selection algorithm, it will fall back to
         * calling and returning the result of JDK method `InetAddress.getLocalHost`.
         *
         *
         * @throws UnknownHostException If the LAN address of the machine cannot be found.
         *
         * From: https://issues.apache.org/jira/browse/JCS-40
         */
        val likelyAddress = mutableListOf<InetAddress>()
        val candidates = mutableListOf<InetAddress>()

        try {
            // Iterate all NICs (network interface cards)...
            val ifaces = NetworkInterface.getNetworkInterfaces()
            while (ifaces.hasMoreElements()) {
                val iface = ifaces.nextElement()
                // Iterate all IP addresses assigned to each card...
                val inetAddrs = iface.inetAddresses
                while (inetAddrs.hasMoreElements()) {
                    val inetAddr = inetAddrs.nextElement()
                    if (!inetAddr.isLoopbackAddress) {
                        if (inetAddr.isSiteLocalAddress) {
                            // Found non-loopback site-local address. Return it immediately...
                            likelyAddress.add(inetAddr)
                        } else {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidates.add(inetAddr)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // ignored
        }

        if (likelyAddress.size == 1) {
            return likelyAddress.first()
        }

        // we have MORE than 1 likely address, but we DO NOT know which one is used for 0.0.0.0 traffic.
        // we **COULD** parse out the gateway information from the routes for each interface... but that is a huge amount of work.
        // it's MUCH easier to see if open a connection to 1.1.1.1 and get the interface this traffic was on, and use that interface IP address
        runCatching {
            Socket().use {
                it.connect(InetSocketAddress("1.1.1.1", 80))
                return it.localAddress
            }
        }.onFailure {
            Common.logger.error("Unable to determine outbound traffic local address. Using alternate logic instead.", it)
        }

        // there was an error doing this! (it's possible that outbound traffic is not allowed
        if (IPv6.isPreferred) {
            val ipv6 = likelyAddress.filterIsInstance<Inet6Address>()
            if (ipv6.isNotEmpty()) {
                return ipv6.first()
            }
        } else if (IPv4.isPreferred) {
            val ipv4 = likelyAddress.filterIsInstance<Inet4Address>()
            if (ipv4.isNotEmpty()) {
                return ipv4.first()
            }
        }

        // we STILL don't have something. Possibly that we have no likely addresses, but we
        // found some other non-loopback address.
        // The machine might have a non-site-local address assigned to its NIC (or it might be running
        // IPv6 which deprecates the "site-local" concept).
        // Return this non-loopback candidate address...
        if (IPv6.isPreferred) {
            val ipv6 = candidates.filterIsInstance<Inet6Address>()
            if (ipv6.isNotEmpty()) {
                return ipv6.first()
            }
        } else if (IPv4.isPreferred) {
            val ipv4 = candidates.filterIsInstance<Inet4Address>()
            if (ipv4.isNotEmpty()) {
                return ipv4.first()
            }
        }

        // At this point, we did not find a non-loopback address.
        // Fall back to returning whatever InetAddress.getLocalHost() returns...
        return InetAddress.getLocalHost()
            ?: throw UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.")
    }

    /**
     * This will retrieve your IP address via an HTTP server.
     *
     * **NOTE: Can optionally use DnsClient.getPublicIp() instead. It's much faster and more reliable as it uses DNS.**
     *
     * @return the public IP address if found, or null if it didn't find it
     */
    fun publicIpViaHttp(): String? {
        // method 1: use DNS servers
        // dig +short myip.opendns.com @resolver1.opendns.com

        // method 2: use public http servers
        // @formatter:off
        val  websites = arrayOf(
            "http://ip.dorkbox.com/",
            "http://checkip.dyndns.com/",
            "http://checkip.dyn.com/",
            "http://curlmyip.com/",
            "http://ipecho.net/plain",
            "http://icanhazip.com/")
        // @formatter:on

        // loop, since they won't always work.
        for (i in websites.indices) {
            try {
                val autoIP = URL(websites[i])
                val `in` = BufferedReader(InputStreamReader(autoIP.openStream()))
                val response = `in`.readLine()
                    .trim { it <= ' ' }
                `in`.close()
                val pattern = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b")
                val matcher = pattern.matcher(response)
                if (matcher.find()) {
                    return matcher.group()
                        .trim { it <= ' ' }
                }
            } catch (ignored: java.lang.Exception) {
            }
        }
        return null
    }

    /**
     * Creates an byte[] based on an ipAddressString. No error handling is performed here.
     */
    fun toBytes(ipAddressString: String): ByteArray {
        if (IPv4.isValid(ipAddressString)) {
            return IPv4.toBytes(ipAddressString)
        }

        return IPv6.toBytes(ipAddressString)
    }

    /**
     * Converts 4-byte or 16-byte data into an IPv4 or IPv6 string respectively.
     *
     * @throws IllegalArgumentException
     * if `length` is not `4` nor `16`
     */
    fun toString(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): String {
        return when (length) {
            4 -> {
                StringBuilder(15)
                        .append(bytes[offset].toInt())
                        .append('.')
                        .append(bytes[offset + 1].toInt())
                        .append('.')
                        .append(bytes[offset + 2].toInt())
                        .append('.')
                        .append(bytes[offset + 3].toInt()).toString()
            }
            16 -> IPv6.toString(bytes, offset)
            else -> throw IllegalArgumentException("length: $length (expected: 4 or 16)")
        }
    }

    /**
     * Returns the [String] representation of an [InetAddress].
     *
     *  * Inet4Address results are identical to [InetAddress.getHostAddress]
     *  * Inet6Address results adhere to
     * [rfc 5952 section 4](http://tools.ietf.org/html/rfc5952#section-4) if
     * `ipv4Mapped` is false.  If `ipv4Mapped` is true then "IPv4 mapped" format
     * from [rfc 4291 section 2](http://tools.ietf.org/html/rfc4291#section-2.5.5) will be supported.
     * The compressed result will always obey the compression rules defined in
     * [rfc 5952 section 4](http://tools.ietf.org/html/rfc5952#section-4)
     *
     *
     *
     * The output does not include Scope ID.
     *
     * @param ip [InetAddress] to be converted to an address string
     * @param ipv4Mapped
     *
     *  * `true` to stray from strict rfc 5952 and support the "IPv4 mapped" format
     * defined in [rfc 4291 section 2](http://tools.ietf.org/html/rfc4291#section-2.5.5) while still
     * following the updated guidelines in
     * [rfc 5952 section 4](http://tools.ietf.org/html/rfc5952#section-4)
     *
     *  * `false` to strictly follow rfc 5952
     *
     * @return `String` containing the text-formatted IP address
     */
    fun toString(ip: InetAddress, ipv4Mapped: Boolean = false): String {
        if (ip is Inet4Address) {
            return IPv4.toString(ip)
        }

        require(ip is Inet6Address) { "Unhandled type: $ip" }

        return IPv6.toString(ip, ipv4Mapped)
    }

    /**
     * Returns the [String] representation of an [InetSocketAddress].
     *
     * The output does not include Scope ID.
     * @param addr [InetSocketAddress] to be converted to an address string
     * @return `String` containing the text-formatted IP address
     */
    fun toString(addr: InetSocketAddress): String {
        val port = addr.port.toString()
        val sb: StringBuilder

        sb = if (addr.isUnresolved) {
            val hostname = addr.hostString
            newSocketAddressStringBuilder(hostname, port, !IPv6.isValid(hostname))
        } else {
            val address = addr.address
            val hostString = toString(address)
            newSocketAddressStringBuilder(hostString, port, address is Inet4Address)
        }
        return sb.append(':').append(port).toString()
    }

    /**
     * Returns the [String] representation of a host port combo.
     */
    fun toString(host: String, port: Int): String {
        val portStr = port.toString()
        return newSocketAddressStringBuilder(host, portStr, !IPv6.isValid(host)).append(':').append(portStr).toString()
    }

    private fun newSocketAddressStringBuilder(host: String, port: String, ipv4: Boolean): StringBuilder {
        val hostLen = host.length
        if (ipv4) {
            // Need to include enough space for hostString:port.
            return StringBuilder(hostLen + 1 + port.length).append(host)
        }

        // Need to include enough space for [hostString]:port.
        val stringBuilder = StringBuilder(hostLen + 3 + port.length)
        return if (hostLen > 1 && host[0] == '[' && host[hostLen - 1] == ']') {
            stringBuilder.append(host)
        } else {
            stringBuilder.append('[').append(host).append(']')
        }
    }

    /**
     * Returns the [InetAddress] representation of a [CharSequence] IP address.
     *
     * This method will treat all IPv4 type addresses as "IPv4 mapped" (see [.getByName])
     *
     * @param ip [CharSequence] IP address to be converted to a [InetAddress]
     * @return [InetAddress] representation of the `ip` or `null` if not a valid IP address.
     */
    fun fromString(ip: String): InetAddress? {
        return if (IPv4.isValid(ip)) {
            IPv4.fromStringUnsafe(ip)
        } else {
            IPv6.fromString(ip)
        }
    }
}
