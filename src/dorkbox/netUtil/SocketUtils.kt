/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package dorkbox.netUtil

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketException
import java.net.SocketPermission
import java.net.UnknownHostException
import java.nio.channels.DatagramChannel
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.security.AccessController
import java.security.PrivilegedAction
import java.security.PrivilegedActionException
import java.security.PrivilegedExceptionAction
import java.util.*

/**
 * Provides socket operations with privileges enabled. This is necessary for applications that use the
 * [SecurityManager] to restrict [SocketPermission] to their application.
 *
 * By asserting that these operations are privileged, the operations can proceed even if some code in the calling chain lacks
 * the appropriate [SocketPermission].
 */
object SocketUtils {
    /**
     * Gets the version number.
     */
    const val version = Common.version

    private val EMPTY = Collections.enumeration(emptyList<Any>())

    @Suppress("UNCHECKED_CAST")
    private fun <T> empty(): Enumeration<T> {
        return EMPTY as Enumeration<T>
    }

    @Throws(IOException::class)
    fun connect(socket: Socket, remoteAddress: SocketAddress?, timeout: Int) {
        try {
            AccessController.doPrivileged<Void>(PrivilegedExceptionAction<Void?> {
                socket.connect(remoteAddress, timeout)
                null
            })
        } catch (e: PrivilegedActionException) {
            throw (e.cause as IOException?)!!
        }
    }

    @Throws(IOException::class)
    fun bind(socket: Socket, bindpoint: SocketAddress?) {
        try {
            AccessController.doPrivileged<Void>(PrivilegedExceptionAction<Void?> {
                socket.bind(bindpoint)
                null
            })
        } catch (e: PrivilegedActionException) {
            throw (e.cause as IOException?)!!
        }
    }

    @Throws(IOException::class)
    fun connect(socketChannel: SocketChannel, remoteAddress: SocketAddress?): Boolean {
        return try {
            AccessController.doPrivileged(PrivilegedExceptionAction { socketChannel.connect(remoteAddress) })
        } catch (e: PrivilegedActionException) {
            throw (e.cause as IOException?)!!
        }
    }

    @Throws(IOException::class)
    fun bind(socketChannel: SocketChannel, address: SocketAddress?) {
        try {
            AccessController.doPrivileged<Void>(PrivilegedExceptionAction<Void?> {
                socketChannel.bind(address)
                null
            })
        } catch (e: PrivilegedActionException) {
            throw (e.cause as IOException?)!!
        }
    }

    @Throws(IOException::class)
    fun accept(serverSocketChannel: ServerSocketChannel): SocketChannel {
        return try {
            AccessController.doPrivileged(PrivilegedExceptionAction { serverSocketChannel.accept() })
        } catch (e: PrivilegedActionException) {
            throw (e.cause as IOException?)!!
        }
    }

    @Throws(IOException::class)
    fun bind(networkChannel: DatagramChannel, address: SocketAddress?) {
        try {
            AccessController.doPrivileged<Void>(PrivilegedExceptionAction<Void?> {
                networkChannel.bind(address)
                null
            })
        } catch (e: PrivilegedActionException) {
            throw (e.cause as IOException?)!!
        }
    }

    fun localSocketAddress(socket: ServerSocket): SocketAddress {
        return AccessController.doPrivileged(PrivilegedAction { socket.localSocketAddress })
    }

    @Throws(UnknownHostException::class)
    fun addressByName(hostname: String?): InetAddress {
        return try {
            AccessController.doPrivileged(PrivilegedExceptionAction { InetAddress.getByName(hostname) })
        } catch (e: PrivilegedActionException) {
            throw (e.cause as UnknownHostException?)!!
        }
    }

    @Throws(UnknownHostException::class)
    fun allAddressesByName(hostname: String?): Array<InetAddress> {
        return try {
            AccessController.doPrivileged(PrivilegedExceptionAction { InetAddress.getAllByName(hostname) })
        } catch (e: PrivilegedActionException) {
            throw (e.cause as UnknownHostException?)!!
        }
    }

    fun socketAddress(hostname: String?, port: Int): InetSocketAddress {
        return AccessController.doPrivileged(PrivilegedAction { InetSocketAddress(hostname, port) })
    }

    fun addressesFromNetworkInterface(intf: NetworkInterface): Enumeration<InetAddress> {
        // Android seems to sometimes return null even if this is not a valid return value by the api docs.
        // Just return an empty Enumeration in this case.
        // See https://github.com/netty/netty/issues/10045
        return AccessController.doPrivileged(PrivilegedAction { intf.inetAddresses })
                ?: return empty()
    }

    fun loopbackAddress(): InetAddress {
        return AccessController.doPrivileged(PrivilegedAction {
            return@PrivilegedAction InetAddress.getLoopbackAddress()
        })
    }

    @Throws(SocketException::class)
    fun hardwareAddressFromNetworkInterface(intf: NetworkInterface): ByteArray {
        return try {
            AccessController.doPrivileged(PrivilegedExceptionAction { intf.hardwareAddress })
        } catch (e: PrivilegedActionException) {
            throw (e.cause as SocketException?)!!
        }
    }
}
