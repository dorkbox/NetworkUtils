/*
 * Copyright 2020 dorkbox, llc
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

import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.AccessController
import java.security.PrivilegedAction
import java.util.*

/**
 * Network Utilities. MAC, IP, NameSpace, etc
 */
internal object Common {
    /**
     * Gets the version number.
     */
    const val version = "2.11"

    val OS_LINUX: Boolean
    val OS_WINDOWS: Boolean
    val OS_MAC: Boolean

    init {
        val osName: String = try {
            if (System.getSecurityManager() == null) {
                System.getProperty("os.name", "linux")
            }
            else {
                AccessController.doPrivileged<String>(PrivilegedAction { System.getProperty("os.name", "linux") })
            }
        } catch (ignored: java.lang.Exception) {
            "linux"
        }.lowercase(Locale.getDefault())

        if (osName.startsWith("mac") || osName.startsWith("darwin")) {
            OS_LINUX = false
            OS_WINDOWS = false
            OS_MAC = true
        } else if (osName.startsWith("win")) {
            OS_LINUX = false
            OS_WINDOWS = true
            OS_MAC = false
        } else {
            OS_LINUX = true
            OS_WINDOWS = false
            OS_MAC = false
        }

        // Add this project to the updates system, which verifies this class + UUID + version information
        dorkbox.updates.Updates.add(Common::class.java, "956b308f207944af95928ef6282a46d7", version)
    }


    internal val logger = LoggerFactory.getLogger("NetworkUtils")

    fun getBoolean(property: String, defaultValue: Boolean): Boolean {
        var value: String = System.getProperty(property, defaultValue.toString()) ?: return defaultValue
        value = value.trim().lowercase(Locale.getDefault())

        if (value.isEmpty()) {
            return defaultValue
        }

        if ("false" == value || "no" == value || "0" == value) {
            return false
        }
        return if ("true" == value || "yes" == value || "1" == value) {
            true
        }
        else defaultValue
    }

    private val HEX2B: IntArray = IntArray('f'.code + 1).apply {
        set('0'.code, 0)
        set('1'.code, 1)
        set('2'.code, 2)
        set('3'.code, 3)
        set('4'.code, 4)
        set('5'.code, 5)
        set('6'.code, 6)
        set('7'.code, 7)
        set('8'.code, 8)
        set('9'.code, 9)
        set('A'.code, 10)
        set('B'.code, 11)
        set('C'.code, 12)
        set('D'.code, 13)
        set('E'.code, 14)
        set('F'.code, 15)
        set('a'.code, 10)
        set('b'.code, 11)
        set('c'.code, 12)
        set('d'.code, 13)
        set('e'.code, 14)
        set('f'.code, 15)
    }

    /**
     * Helper to decode half of a hexadecimal number from a string.
     *
     * @param c The ASCII character of the hexadecimal number to decode.
     * Must be in the range `[0-9a-fA-F]`.
     *
     * @return The hexadecimal value represented in the ASCII character
     * given, or `-1` if the character is invalid.
     */
    fun decodeHexNibble(c: Char): Int {
        // Character.digit() is not used here, as it addresses a larger
        // set of characters (both ASCII and full-width latin letters).
        val index = c.code

        return try {
            HEX2B[index]
        } catch (e: Exception) {
            -1
        }
    }

    fun socketAddress(hostname: String, port: Int): InetSocketAddress {
        return AccessController.doPrivileged(PrivilegedAction { InetSocketAddress(hostname, port) })
    }

    fun socketAddress(host: InetAddress, port: Int): InetSocketAddress {
        return AccessController.doPrivileged(PrivilegedAction { InetSocketAddress(host, port) })
    }
}
