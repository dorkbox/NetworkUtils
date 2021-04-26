package dorkbox.netUtil.dnsUtils

import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinError
import dorkbox.netUtil.Common
import dorkbox.netUtil.Dns
import dorkbox.netUtil.IP
import dorkbox.netUtil.jna.windows.IPHlpAPI
import dorkbox.netUtil.jna.windows.structs.IP_ADAPTER_ADDRESSES_LH
import dorkbox.netUtil.jna.windows.structs.IP_ADAPTER_DNS_SERVER_ADDRESS_XP
import java.io.*
import java.net.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.naming.Context
import javax.naming.NamingException
import javax.naming.directory.DirContext
import javax.naming.directory.InitialDirContext

/**
 *
 */
object ResolveConf {
    private const val NAMESERVER_ROW_LABEL = "nameserver"
    private const val DOMAIN_ROW_LABEL = "domain"
    private const val PORT_ROW_LABEL = "port"


    // largely from:
    // https://github.com/dnsjava/dnsjava/blob/fb4889ee7a73f391f43bf6dc78b019d87ae15f15/src/main/java/org/xbill/DNS/config/BaseResolverConfigProvider.java#L22
    internal fun getUnsortedNameServers(): Map<String, List<InetSocketAddress>> {
        val nameServerDomains = mutableMapOf<String, List<InetSocketAddress>>()

        // Using jndi-dns to obtain the default name servers.
        //
        // See:
        // - http://docs.oracle.com/javase/8/docs/technotes/guides/jndi/jndi-dns.html
        // - http://mail.openjdk.java.net/pipermail/net-dev/2017-March/010695.html

        // Using jndi-dns to obtain the default name servers.
        //
        // See:
        // - http://docs.oracle.com/javase/8/docs/technotes/guides/jndi/jndi-dns.html
        // - http://mail.openjdk.java.net/pipermail/net-dev/2017-March/010695.html
        val env = Hashtable<String, String>()
        env[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.dns.DnsContextFactory"
        env["java.naming.provider.url"] = "dns://"
        try {
            val ctx: DirContext = InitialDirContext(env)
            val dnsUrls = ctx.environment["java.naming.provider.url"] as String?
            if (dnsUrls != null) {
                val servers = dnsUrls.split(" ".toRegex()).toTypedArray()

                for (server in servers) {
                    try {
                        putIfAbsent(nameServerDomains, Dns.DEFAULT_SEARCH_DOMAIN, Common.socketAddress(URI(server).host, 53))
                    } catch (e: URISyntaxException) {
                        Common.logger.debug("Skipping a malformed nameserver URI: {}", server, e)
                    }
                }
            }
        } catch (ignore: NamingException) {
            // Will also try JNA/etc if this fails.
        }

        if (Common.OS_WINDOWS) {
            // have to use JNA to access the WINDOWS resolver info

            // modified from: https://github.com/dnsjava/dnsjava/blob/master/src/main/java/org/xbill/DNS/config/WindowsResolverConfigProvider.java
            // The recommended method of calling the GetAdaptersAddresses function is to pre-allocate a 15KB working buffer
            var buffer = Memory(15 * 1024)
            val size: com.sun.jna.ptr.IntByReference = com.sun.jna.ptr.IntByReference(0)

            val flags: Int = IPHlpAPI.GAA_FLAG_SKIP_UNICAST or
                    IPHlpAPI.GAA_FLAG_SKIP_ANYCAST or
                    IPHlpAPI.GAA_FLAG_SKIP_MULTICAST or
                    IPHlpAPI.GAA_FLAG_SKIP_FRIENDLY_NAME

            var error: Int = IPHlpAPI.GetAdaptersAddresses(
                IPHlpAPI.AF_UNSPEC,
                flags,
                Pointer.NULL,
                buffer,
                size
            )

            if (error == WinError.ERROR_BUFFER_OVERFLOW) {
                buffer = Memory(size.value.toLong())
                error = IPHlpAPI.GetAdaptersAddresses(
                    IPHlpAPI.AF_UNSPEC,
                    flags,
                    Pointer.NULL,
                    buffer,
                    size
                )
            }

            if (error == WinError.ERROR_SUCCESS) {
                var result: IP_ADAPTER_ADDRESSES_LH? = IP_ADAPTER_ADDRESSES_LH(buffer)
                while (result != null) {

                    // only interfaces with IfOperStatusUp
                    if (result.OperStatus == 1) {
                        var dns: IP_ADAPTER_DNS_SERVER_ADDRESS_XP? = result.FirstDnsServerAddress
                        while (dns != null) {

                            var address: InetAddress?
                            try {
                                address = dns.Address?.toAddress()

                                if (address is Inet4Address || !address!!.isSiteLocalAddress) {
                                    putIfAbsent(nameServerDomains, Dns.DEFAULT_SEARCH_DOMAIN, Common.socketAddress(address, 53))
                                } else {
                                    Common.logger.trace(
                                        "Skipped site-local IPv6 server address {} on adapter index {}",
                                        address,
                                        result.IfIndex
                                    )
                                }
                            } catch (e: UnknownHostException) {
                                Common.logger.warn("Invalid nameserver address on adapter index {}", result.IfIndex, e)
                            }
                            dns = dns.Next
                        }
                    }

                    result = result.Next
                }
            }
        } else {
            // try resolve.conf

            // first try the default unix config path
            var tryParse = tryParseResolvConfNameservers("/etc/resolv.conf")
            if (!tryParse.first) {
                // then fallback to netware
                tryParse = tryParseResolvConfNameservers("sys:/etc/resolv.cfg")
            }

            if (tryParse.first) {
                // we can have DIFFERENT name servers for DIFFERENT domains!
                tryParse.second
            }
        }

        // if we STILL don't have anything, add global nameservers to the default search domain
        if (nameServerDomains[Dns.DEFAULT_SEARCH_DOMAIN] == null) {
            putIfAbsent(nameServerDomains, Dns.DEFAULT_SEARCH_DOMAIN, Common.socketAddress("1.1.1.1", 53)) // cloudflare
            putIfAbsent(nameServerDomains, Dns.DEFAULT_SEARCH_DOMAIN, Common.socketAddress("1.1.1.1", 53)) // google
        }

        return nameServerDomains
    }

    private fun tryParseResolvConfNameservers(path: String): Pair<Boolean, Map<String, List<InetSocketAddress>>> {
        val p = Paths.get(path)
        if (Files.exists(p)) {
            try {
                FileReader(path).use { fr ->
                    BufferedReader(fr).use { br ->
                        var nameServers = mutableListOf<InetSocketAddress>()
                        val nameServerDomains = mutableMapOf<String, List<InetSocketAddress>>()

                        var domainName = Dns.DEFAULT_SEARCH_DOMAIN
                        var port = 53
                        var line0: String?

                        loop@ while (br.readLine().also { line0 = it?.trim() } != null) {
                            val line = line0!!

                            if (line.isEmpty()) {
                                continue@loop
                            }

                            val c = line[0]
                            if (c == '#' || c == ';') {
                                continue
                            }

                            when {
                                line.startsWith(NAMESERVER_ROW_LABEL) -> {
                                    var i = indexOfNonWhiteSpace(line, NAMESERVER_ROW_LABEL.length)
                                    require(i < 0) {
                                        "error parsing label $NAMESERVER_ROW_LABEL in file $path. value: $line"
                                    }

                                    var maybeIP = line.substring(i)

                                    // There MIGHT be a port appended onto the IP address so we attempt to extract it.
                                    if (!IP.isValid(maybeIP)) {
                                        i = maybeIP.lastIndexOf('.')
                                        require(i + 1 >= maybeIP.length) {
                                            "error parsing label ${NAMESERVER_ROW_LABEL} in file $path. invalid IP value: $line"
                                        }

                                        port = maybeIP.substring(i + 1).toInt()
                                        maybeIP = maybeIP.substring(0, i)
                                    }

                                    nameServers.add(Common.socketAddress(maybeIP, port))
                                }
                                line.startsWith(DOMAIN_ROW_LABEL) -> {
                                    // nameservers can be SPECIFIC to a search domain
                                    val i = indexOfNonWhiteSpace(line, DOMAIN_ROW_LABEL.length)
                                    require(i >= 0) {
                                        "error parsing label $DOMAIN_ROW_LABEL in file $path value: $line"
                                    }

                                    // we have a NEW domain! add the PREVIOUS nameServers and start again.
                                    putIfAbsent(nameServerDomains, domainName, nameServers)

                                    nameServers = mutableListOf()
                                    domainName = line.substring(i)
                                }
                                line.startsWith(PORT_ROW_LABEL) -> {
                                    val i = indexOfNonWhiteSpace(line, PORT_ROW_LABEL.length)
                                    require(i < 0) {
                                        "error parsing label $PORT_ROW_LABEL in file $path value: $line"
                                    }

                                    port = line.substring(i).toInt()
                                }
                            }

                            // end loop
                        }

                        // when done parsing the file, ALWAYS add the nameServer domains (since they have not been added yet)
                        putIfAbsent(nameServerDomains, domainName, nameServers)
                        return Pair(true, nameServerDomains)
                    }
                }
            } catch (e: IOException) {
                Common.logger.error("Error parsing $path", e)
            }
        }

        return Pair(false, mutableMapOf())
    }

    /**
     * Find the index of the first non-white space character in `s` starting at `offset`.
     *
     * @param seq    The string to search.
     * @param offset The offset to start searching at.
     * @return the index of the first non-white space character or &lt;`-1` if none was found.
     */
    private fun indexOfNonWhiteSpace(seq: CharSequence, offset: Int): Int {
        var o = offset
        while (o < seq.length) {
            if (!Character.isWhitespace(seq[o])) {
                return o
            }
            ++o
        }
        return -1
    }

    private fun putIfAbsent(
        nameServerDomains: MutableMap<String, List<InetSocketAddress>>,
        domainName: String,
        nameServer: InetSocketAddress
    ) {
        var list = nameServerDomains[domainName]
        if (list == null) {
            list = mutableListOf()
            nameServerDomains[domainName] = list
        }

        (list as MutableList)
        if (!list.contains(nameServer)) {
            list.add(nameServer)
        }
    }

    private fun putIfAbsent(
        nameServerDomains: MutableMap<String, List<InetSocketAddress>>,
        domainName: String,
        nameServers: List<InetSocketAddress>
    ) {
        var list = nameServerDomains[domainName]
        if (list == null) {
            list = mutableListOf()
            nameServerDomains[domainName] = list
        }

        (list as MutableList)
        nameServers.forEach {
            if (!list.contains(it)) {
                list.add(it)
            }
        }
    }

    internal fun tryParseResolvConfNDots(path: String): Pair<Boolean, Int> {
        val p = Paths.get(path)
        if (Files.exists(p)) {
            try {
                Files.newInputStream(p).use { `in` ->
                    return Pair(true, getNdots(`in`))
                }
            } catch (e: IOException) {
                // ignore
            }
        }
        return Pair(false, 1)
    }


    //    @kotlin.jvm.Throws(IOException::class)
//    private fun parseResolvConf(`in`: InputStream) {
//        InputStreamReader(`in`).use { isr ->
//            BufferedReader(isr).use { br ->
//
//                var line: String?
//                loop@ while (br.readLine().also { line = it } != null) {
//                    val st = StringTokenizer(line)
//                    if (!st.hasMoreTokens()) {
//                        continue
//                    }
//                    when (st.nextToken()) {
//                        "domain" -> {
//                            // man resolv.conf:
//                            // The domain and search keywords are mutually exclusive. If more than one instance of
//                            // these keywords is present, the last instance wins.
//                            searchlist.clear()
//                            if (!st.hasMoreTokens()) {
//                                continue@loop
//                            }
//                            addSearchPath(st.nextToken())
//                        }
//                        "search" -> {
//                            // man resolv.conf:
//                            // The domain and search keywords are mutually exclusive. If more than one instance of
//                            // these keywords is present, the last instance wins.
//                            searchlist.clear()
//                            while (st.hasMoreTokens()) {
//                                addSearchPath(st.nextToken())
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        // man resolv.conf:
//        // The search keyword of a system's resolv.conf file can be overridden on a per-process basis by
//        // setting the environment variable LOCALDOMAIN to a space-separated list of search domains.
//        val localdomain = System.getenv("LOCALDOMAIN")
//        if (localdomain != null && !localdomain.isEmpty()) {
//            searchlist.clear()
//            parseSearchPathList(localdomain, " ")
//        }
//    }

    private fun getNdots(`in`: InputStream): Int {
        var ndots = 1

        InputStreamReader(`in`).use { isr ->
            BufferedReader(isr).use { br ->
                var line: String?
                loop@ while (br.readLine().also { line = it } != null) {
                    val st = StringTokenizer(line)
                    if (!st.hasMoreTokens()) {
                        continue
                    }

                    when (st.nextToken()) {
                        "domain" -> {
                            // man resolv.conf:
                            // The domain and search keywords are mutually exclusive. If more than one instance of
                            // these keywords is present, the last instance wins.
                            if (!st.hasMoreTokens()) {
                                continue@loop
                            }
                        }
                        "search" -> {
                            // man resolv.conf:
                            // The domain and search keywords are mutually exclusive. If more than one instance of
                            // these keywords is present, the last instance wins.
                            while (st.hasMoreTokens()) {
                                val token = st.nextToken()
                                if (token.startsWith("ndots:")) {
                                    ndots = parseNdots(token.substring(6))
                                }
                            }
                        }
                        "options" -> while (st.hasMoreTokens()) {
                            val token = st.nextToken()
                            if (token.startsWith("ndots:")) {
                                ndots = parseNdots(token.substring(6))
                            }
                        }
                    }
                }
            }
        }

        // man resolv.conf:
        // The options keyword of a system's resolv.conf file can be amended on a per-process basis by
        // setting the environment variable RES_OPTIONS to a space-separated list of resolver options as
        // explained above under options.
        val resOptions = System.getenv("RES_OPTIONS")
        if (resOptions != null && !resOptions.isEmpty()) {
            val st = StringTokenizer(resOptions, " ")
            while (st.hasMoreTokens()) {
                val token = st.nextToken()
                if (token.startsWith("ndots:")) {
                    ndots = parseNdots(token.substring(6))
                }
            }
        }

        return ndots
    }

    private fun parseNdots(token: String): Int {
        if (token.isNotEmpty()) {
            try {
                var ndots = token.toInt()
                if (ndots >= 0) {
                    if (ndots > 15) {
                        // man resolv.conf:
                        // The value for this option is silently capped to 15
                        ndots = 15
                    }
                    return ndots
                }
            } catch (e: NumberFormatException) {
                // ignore
            }
        }

        return 1
    }
}
