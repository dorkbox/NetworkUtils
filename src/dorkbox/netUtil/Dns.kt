/*
 * Copyright 2023 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dorkbox.netUtil

import dorkbox.executor.Executor
import dorkbox.netUtil.dnsUtils.DefaultHostsFileResolver
import dorkbox.netUtil.dnsUtils.ResolveConf
import dorkbox.netUtil.dnsUtils.ResolvedAddressTypes
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Domain types differentiated by Mozilla Public Suffix List.
 *
 * @since 4.5
 */
enum class DomainType {
    UNKNOWN, ICANN, PRIVATE
}


data class PublicSuffixList(val type: DomainType, val rules: Set<String>, val exceptions: Set<String>, val wildcards: Set<String>) {
    constructor(rules: Set<String>, exceptions: Set<String>, wildcards: Set<String>) : this(DomainType.UNKNOWN, rules, exceptions, wildcards)
}

object Dns {

    /**
     * Gets the version number.
     */
    const val version = Common.version

    const val DEFAULT_SEARCH_DOMAIN = ""

    private var listTypes: MutableList<PublicSuffixList>

    /**
     * @throws IOException if the DNS resolve.conf file cannot be read
     */
    fun setDNSServers(dnsServersString: String) {
        if (Common.OS_LINUX) {
            val dnsServers = dnsServersString.split(",")
            val dnsFile = File("/etc/resolvconf/resolv.conf.d/head")

            if (!dnsFile.canRead()) {
                throw IOException("Unable to initialize dns server file. Something is SERIOUSLY wrong")
            }

            BufferedWriter(FileWriter(dnsFile)).use {
                it.write("# File location: /etc/resolvconf/resolv.conf.d/head\n")
                it.write("# Dynamic resolv.conf(5) file for glibc resolver(3) generated by resolvconf(8)\n")

                dnsServers.forEach { dns ->
                    it.write("nameserver $dns\n")
                }

                it.flush();
            }

            Executor().command("resolvconf", "-u").startBlocking()
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    /**
     * Resolve the address of a hostname against the entries in a hosts file, depending on some address types.
     *
     * @param inetHost the hostname to resolve
     * @param resolvedAddressTypes the address types to resolve
     *
     * @return the first matching address or null
     */
    fun resolveFromHosts(inetHost: String, resolvedAddressTypes: ResolvedAddressTypes = ResolvedAddressTypes.IPV4_PREFERRED): InetAddress? {
        return DefaultHostsFileResolver.address(inetHost, resolvedAddressTypes)
    }

    /** Returns all name servers, including the default ones. */
    val nameServers: Map<String, List<InetSocketAddress>> by lazy {
        ResolveConf.getUnsortedNameServers()
    }

    /** Returns all default name servers. */
    val defaultNameServers: List<InetSocketAddress> by lazy {
        val defaultServers = nameServers[DEFAULT_SEARCH_DOMAIN]!!
        if (IPv6.isPreferred) {
            // prefer IPv6: return IPv6 first, then IPv4 (each in the order added)
            defaultServers.filter { it.address is Inet6Address } + defaultServers.filter { it.address is Inet4Address }
        } else if (IPv4.isPreferred) {
            // skip IPv6 addresses
            defaultServers.filter { it.address is Inet4Address }
        } else {
            // neither is specified, return in the order added
            defaultServers
        }
    }

    /**
     * Gets the threshold for the number of dots which must appear in a name before it is considered
     * absolute. The default is 1.
     */
    val numberDots: Int by lazy {
        if (Common.OS_WINDOWS) {
            1
        } else {
            // first try the default unix config path
            var tryParse = ResolveConf.tryParseResolvConfNDots("/etc/resolv.conf")
            if (!tryParse.first) {
                // then fallback to netware
                tryParse = ResolveConf.tryParseResolvConfNDots("sys:/etc/resolv.cfg")
            }
            tryParse.second
        }
    }



    init {
        /**
         * And the effective_tld_names.dat is from mozilla (the following are all the same data)
         *
         *
         * https://mxr.mozilla.org/mozilla-central/source/netwerk/dns/effective_tld_names.dat?raw=1
         * which is...
         * https://publicsuffix.org/list/public_suffix_list.dat
         *
         *
         * Parses the list from publicsuffix.org
         * new one at:
         * http://svn.apache.org/repos/asf/httpcomponents/httpclient/trunk/httpclient5/src/main/java/org/apache/hc/client5/http/impl/cookie/PublicSuffixDomainFilter.java
         * and
         * http://svn.apache.org/repos/asf/httpcomponents/httpclient/trunk/httpclient5/src/main/java/org/apache/hc/client5/http/psl/
         */

        this.listTypes = mutableListOf()
        var domainType: DomainType? = null

        var exceptions: MutableSet<String> = mutableSetOf()
        var rules: MutableSet<String> = mutableSetOf()
        var wildcards: MutableSet<String> = mutableSetOf()

        // now load this file into memory, so it's faster to process.
        val tldResource = Dns.javaClass.getResourceAsStream("/public_suffix_list.dat")
        tldResource?.bufferedReader()?.useLines { lines ->
            lines.forEach { line ->
                if (line.isEmpty()) {
                    return@forEach
                }

                if (line.startsWith("//")) {
                    if (domainType == null) {
                        if (line.contains("===BEGIN ICANN DOMAINS===")) {
                            domainType = DomainType.ICANN
                        } else if (line.contains("===BEGIN PRIVATE DOMAINS===")) {
                            domainType = DomainType.PRIVATE
                        }
                    } else {
                        if (line.contains("===END ICANN DOMAINS===") || line.contains("===END PRIVATE DOMAINS===")) {
                            listTypes.add(PublicSuffixList(domainType!!, rules, exceptions, wildcards))

                            domainType = null
                            rules = mutableSetOf()
                            exceptions = mutableSetOf()
                            wildcards = mutableSetOf()
                        }
                    }

                    //entire lines can also be commented using //
                    return@forEach
                }


                if (domainType == null) {
                    // only parse data from well known sections
                    return@forEach
                }

                @Suppress("NAME_SHADOWING")
                var line = line
                if (line.startsWith(".")) {
                    line = line.substring(1) // A leading dot is optional
                }

                // An exclamation mark (!) at the start of a rule marks an exception to a previous wildcard rule
                if (line.startsWith("!")) {
                    // *.kawasaki.jp
                    //!city.kawasaki.jp
                    line = line.substring(1)
                    exceptions.add(line)
                } else if (line.startsWith("*")) {
                    // *.kawasaki.jp
                    // motors.kawasaki.jp IS A TLD
                    // kawasaki.jp IS NOT a TLD
                    // city.kawasaki.jp IS NOT a TLD  (!city.kawasaki.jp is a rule)
                    line = line.substring(2)
                    wildcards.add(line)
                } else {
                    // this is a normal rule
                    rules.add(line)
                }
            }
        }
    }


    /**
     * Extracts the second level domain, from a fully qualified domain (ie: www.aa.com, or www.amazon.co.uk).
     *
     *
     * This algorithm works from left to right parsing the domain string parameter
     *
     * @param domain a fully qualified domain (ie: www.aa.com, or www.amazon.co.uk)
     *
     * @return null (if there is no second level domain) or the SLD www.aa.com -> aa.com , or www.amazon.co.uk -> amazon.co.uk
     */
    @Suppress("NAME_SHADOWING")
    fun extractSLD(domain: String): String? {
        var domain = domain
        var last = domain
        var anySLD = false

        do {
            if (isTLD(domain)) {
                return if (anySLD) {
                    last
                }
                else {
                    null
                }
            }

            anySLD = true
            last = domain

            val nextDot = domain.indexOf(".")
            if (nextDot == -1) {
                return null
            }

            domain = domain.substring(nextDot + 1)
        } while (domain.isNotEmpty())

        return null
    }

    /**
     * Returns a domain that is without its TLD at the end.
     *
     * @param domain  domain a fully qualified domain or not, (ie: www.aa.com, or amazon.co.uk).
     *
     * @return a domain that is without it's TLD, ie: www.aa.com -> www.aa, or google.com -> google
     */
    fun withoutTLD(domain: String): String {
        var index = 0
        while (index != -1) {
            index = domain.indexOf('.', index)

            if (index != -1) {
                if (isTLD(domain.substring(index))) {
                    return domain.substring(0, index)
                }
                index++
            }
            else {
                return ""
            }
        }

        return ""
    }

    /**
     * Checks if the domain is a TLD.
     */
    @Suppress("NAME_SHADOWING")
    fun isTLD(domain: String): Boolean {
        var domain = domain
        if (domain.startsWith(".")) {
            domain = domain.substring(1)
        }

        // An exception rule takes priority over any other matching rule.
        // Exceptions are ones that are not a TLD, but would match a pattern rule
        // e.g. bl.uk is not a TLD, but the rule *.uk means it is. Hence there is an exception rule
        // stating that bl.uk is not a TLD.
        listTypes.forEach { list ->
            // exceptions always take priority
            if (list.exceptions.contains(domain)) {
                // exceptions list means that this is NOT a TLD, even though it looks like one
                return false
            }

            if (list.rules.contains(domain)) {
                // we have an explicit rule for this domain
                return true
            }

            // Try patterns....
            // *.kawasaki.jp
            // motors.kawasaki.jp IS A TLD
            // kawasaki.jp IS NOT a TLD
            // city.kawasaki.jp IS NOT a TLD  (!city.kawasaki.jp is a rule)
            val nextdot = domain.indexOf('.')
            if (nextdot == -1) {
                // there is no wildcard possibility
                return false
            }

            return list.wildcards.contains("*" + domain.substring(nextdot))
        }

        return false
    }

    /**
     * Checks if the domain is a TLD.
     */
    @Suppress("NAME_SHADOWING")
    fun getDomainType(domain: String): DomainType {
        var domain = domain
        if (domain.startsWith(".")) {
            domain = domain.substring(1)
        }

        // An exception rule takes priority over any other matching rule.
        // Exceptions are ones that are not a TLD, but would match a pattern rule
        // e.g. bl.uk is not a TLD, but the rule *.uk means it is. Hence there is an exception rule
        // stating that bl.uk is not a TLD.
        listTypes.forEach { list ->
            // exceptions always take priority
            if (list.exceptions.contains(domain)) {
                // exceptions list means that this is NOT a TLD, even though it looks like one
                return list.type  // false (This is not a TLD, and it's defined in the public suffixed list)
            }

            if (list.rules.contains(domain)) {
                // This is a TLD, and it's defined in the public suffixed list
                return list.type
            }

            // Try patterns....
            // *.kawasaki.jp
            // motors.kawasaki.jp IS A TLD
            // kawasaki.jp IS NOT a TLD
            // city.kawasaki.jp IS NOT a TLD  (!city.kawasaki.jp is a rule)
            val nextdot = domain.indexOf('.')
            if (nextdot == -1) {
                // not a tld, because it's not formatted correctly
                return DomainType.UNKNOWN
            }

            val domain = "*" + domain.substring(nextdot)
            if (list.wildcards.contains(domain)) {
                return list.type
            }
        }

        return DomainType.UNKNOWN
    }
}
