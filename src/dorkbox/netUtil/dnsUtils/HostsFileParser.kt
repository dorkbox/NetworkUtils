/*
 * Copyright 2015 The Netty Project
 * Copyright 2021 dorkbox, llc
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package dorkbox.netUtil.dnsUtils

import dorkbox.netUtil.Common.OS_WINDOWS
import dorkbox.netUtil.Common.logger
import dorkbox.netUtil.IPv4
import dorkbox.netUtil.IPv6
import java.io.*
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern

/**
 * A parser for hosts files.
 */
object HostsFileParser {
    private const val WINDOWS_DEFAULT_SYSTEM_ROOT = "C:\\Windows"
    private const val WINDOWS_HOSTS_FILE_RELATIVE_PATH = "\\system32\\drivers\\etc\\hosts"
    private const val X_PLATFORMS_HOSTS_FILE_PATH = "/etc/hosts"

    private val WHITESPACES = Pattern.compile("[ \t]+")

    private fun locateHostsFile(): File {
        var hostsFile: File

        if (OS_WINDOWS) {
            hostsFile = File(System.getenv("SystemRoot") + WINDOWS_HOSTS_FILE_RELATIVE_PATH)

            if (!hostsFile.exists()) {
                hostsFile = File(WINDOWS_DEFAULT_SYSTEM_ROOT + WINDOWS_HOSTS_FILE_RELATIVE_PATH)
            }
        } else {
            hostsFile = File(X_PLATFORMS_HOSTS_FILE_PATH)
        }

        return hostsFile
    }

    /**
     * Parse hosts file at standard OS location using the systems default [Charset] for decoding.
     *
     * @return a [HostsFileEntries]
     */
    fun parse(): HostsFileEntries {
        return parse(Charset.defaultCharset())
    }

    /**
     * Parse hosts file at standard OS location using the given [Charset]s one after each other until
     * we were able to parse something or none is left.
     *
     * @param charsets the [Charset]s to try as file encodings when parsing.
     *
     * @return a [HostsFileEntries]
     */
    fun parse(vararg charsets: Charset): HostsFileEntries {
        val hostsFile = locateHostsFile()
        return try {
            parse(hostsFile, *charsets)
        } catch (e: IOException) {
            logger.warn("Failed to load and parse hosts file at " + hostsFile.path, e)
            HostsFileEntries()
        }
    }

    /**
     * Parse a hosts file.
     *
     * @param file the file to be parsed
     * @param charsets the [Charset]s to try as file encodings when parsing.
     *
     * @return a [HostsFileEntries]
     */
    fun parse(file: File, vararg charsets: Charset): HostsFileEntries {
        val hostsFileEntries = HostsFileEntries()

        try {
            if (file.exists() && file.isFile) {
                for (charset in charsets) {
                    BufferedReader(InputStreamReader(FileInputStream(file), charset)).use { reader ->
                        val entries = parse(reader)
                        if (entries != hostsFileEntries) {
                            return entries
                        }
                    }
                }
            }
        } catch (e: IOException) {
            logger.warn("Failed to load and parse hosts file at " + file.path, e)
        }

        return hostsFileEntries
    }

    /**
     * Parse a reader of hosts file format.
     *
     * @param reader the file to be parsed
     *
     * @return a [HostsFileEntries]
     */
    fun parse(reader: Reader): HostsFileEntries {

        val ipv4Entries = mutableMapOf<String, Inet4Address>()
        val ipv6Entries = mutableMapOf<String, Inet6Address>()

        reader.useLines { lines ->
            lines.map { line ->
                // remove comments
                val commentPosition = line.indexOf('#')
                if (commentPosition != -1) {
                    line.substring(0, commentPosition)
                } else {
                    line
                }
            }.map { line ->
                // trim lines
                line.trim()
            }.filter { line ->
                // skip empty lines
                line.isNotEmpty()
            }.map { line ->
                // split
                val lineParts = mutableListOf<String>()
                for (s in WHITESPACES.split(line)) {
                    if (s.isNotEmpty()) {
                        lineParts.add(s)
                    }
                }
                lineParts
            }.filter { lineParts ->
                // a valid line should be [IP, hostname, alias*]
                // skip invalid lines!
                lineParts.size >= 2
            }.forEach { lineParts ->
                val ip = lineParts[0]
                val ipBytes = when {
                    IPv4.isValid(ip) -> IPv4.toBytes(ip)
                    IPv6.isValid(ip) -> IPv6.toBytes(ip)
                    else -> null
                }

                if (ipBytes != null) {
                    // loop over hostname and aliases, skip invalid IP
                    for (i in 1 until lineParts.size) {
                        val hostname = lineParts[i]
                        val hostnameLower = hostname.toLowerCase(Locale.ENGLISH)
                        val address = InetAddress.getByAddress(hostname, ipBytes)
                        if (address is Inet4Address) {
                            val previous = ipv4Entries.put(hostnameLower, address)
                            if (previous != null) {
                                // restore, we want to keep the first entry
                                ipv4Entries[hostnameLower] = previous
                            }
                        } else {
                            val previous = ipv6Entries.put(hostnameLower, address as Inet6Address)
                            if (previous != null) {
                                // restore, we want to keep the first entry
                                ipv6Entries[hostnameLower] = previous
                            }
                        }
                    }
                }
            }
        }

        return HostsFileEntries(ipv4Entries, ipv6Entries)
    }
}
