/*
 * Copyright 2022 dorkbox, llc
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

package dorkbox.netUtil.ping

import dorkbox.executor.Executor
import dorkbox.netUtil.Common
import dorkbox.netUtil.IP
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetAddress
import java.time.Duration

/**
 *
 */
object Ping {
    private val logger = LoggerFactory.getLogger(Ping::class.java.simpleName)

    /**
     * Gets the version number.
     */
    const val version = Common.version

    private var host = "1.1.1.1"
    private var count = 4
    private var waitTime = Duration.ofSeconds(4)
    private var deadline: Int? = null
    private var ttl: Short? = null

    fun host(host: String): Ping {
        this.host = host
        return this
    }

    fun host(ipAddress: InetAddress): Ping {
        this.host = IP.toString(ipAddress)
        return this
    }

    fun host(ipAddressBytes: ByteArray): Ping {
        this.host = IP.toString(ipAddressBytes)
        return this
    }

    fun count(count: Int): Ping {
        this.count = count
        return this
    }

    fun waitTime(seconds: Int): Ping {
        this.waitTime = Duration.ofSeconds(seconds.toLong())
        return this
    }

    fun deadline(seconds: Int): Ping {
        this.deadline = seconds
        return this
    }

    fun ttl(ttl: Int): Ping {
        this.ttl = ttl.toShort()
        return this
    }


    @Throws(IOException::class)
    fun run(host: String = "1.1.1.1"): PingResult {
        val ping = Executor()
            .command("ping")

        if (Common.OS_WINDOWS) {
            ping.addArg("-n $count")
        }
        else {
            ping.addArg("-c $count")
        }

        if (waitTime != null) {
            when {
                Common.OS_MAC -> {
                    ping.addArg("-W " + waitTime.toMillis())
                }
                Common.OS_WINDOWS -> {
                    ping.addArg("-w " + waitTime.toMillis())
                }
                else -> {
                    ping.addArg("-W " + waitTime.seconds)
                }
            }
        }

        if (deadline != null) {
            when {
                Common.OS_MAC -> {
                    ping.addArg("-t $deadline")
                }
                Common.OS_WINDOWS -> {
                    logger.info("Deadline is not supported on Windows")
                }
                else -> {
                    ping.addArg("-w $deadline")
                }
            }
        }

        if (ttl != null) {
            when {
                Common.OS_MAC -> {
                    ping.addArg("-m $ttl")
                }
                Common.OS_WINDOWS -> {
                    ping.addArg("-i $ttl")
                }
                else -> {
                    ping.addArg("-t $ttl")
                }
            }
        }

        ping.addArg(host)

        // wait for it to finish running
        val output: String = ping.enableRead().startAsShellBlocking().output.utf8()
        return PingResultBuilder.fromOutput(output)
    }
}
