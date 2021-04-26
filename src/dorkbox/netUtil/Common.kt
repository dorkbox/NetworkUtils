@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dorkbox.netUtil

import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.AccessController
import java.security.PrivilegedAction

/**
 * Network Utilities. MAC, IP, NameSpace, etc
 */
internal object Common {
    /**
     * Gets the version number.
     */
    const val version = "2.4"

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
        }.toLowerCase()

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
        value = value.trim().toLowerCase()

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

    private val HEX2B: IntArray = IntArray('f'.toInt()+1).apply {
        set('0'.toInt(), 0)
        set('1'.toInt(), 1)
        set('2'.toInt(), 2)
        set('3'.toInt(), 3)
        set('4'.toInt(), 4)
        set('5'.toInt(), 5)
        set('6'.toInt(), 6)
        set('7'.toInt(), 7)
        set('8'.toInt(), 8)
        set('9'.toInt(), 9)
        set('A'.toInt(), 10)
        set('B'.toInt(), 11)
        set('C'.toInt(), 12)
        set('D'.toInt(), 13)
        set('E'.toInt(), 14)
        set('F'.toInt(), 15)
        set('a'.toInt(), 10)
        set('b'.toInt(), 11)
        set('c'.toInt(), 12)
        set('d'.toInt(), 13)
        set('e'.toInt(), 14)
        set('f'.toInt(), 15)
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
        val index = c.toInt()

        return try {
            HEX2B[index]
        } catch (e: Exception) {
            -1
        }
    }

    fun socketAddress(hostname: String, port: Int): InetSocketAddress {
        return AccessController.doPrivileged<InetSocketAddress>(PrivilegedAction { InetSocketAddress(hostname, port) })
    }

    fun socketAddress(host: InetAddress, port: Int): InetSocketAddress {
        return AccessController.doPrivileged<InetSocketAddress>(PrivilegedAction { InetSocketAddress(host, port) })
    }
}
