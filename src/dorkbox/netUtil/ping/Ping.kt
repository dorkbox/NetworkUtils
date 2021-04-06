package dorkbox.netUtil.ping

import dorkbox.executor.Executor
import dorkbox.netUtil.Common
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration

/**
 *
 */
class Ping {
    companion object {
        private val logger = LoggerFactory.getLogger(Ping::class.java.simpleName)

        /**
         * Gets the version number.
         */
        const val version = "2.1"
    }

    private val count = 4
    private val host = "1.1.1.1"
    private val waitTime = Duration.ofSeconds(4)
    private val deadline: Duration? = null
    private val ttl: Short? = null

    @Throws(IOException::class)
    fun run(): PingResult {
        val ping = Executor()
            .command("ping")

        if (Common.OS_WINDOWS) {
            ping.addArg("-n")
            ping.addArg("$count")
        }
        else {
            ping.addArg("-q")
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
                    ping.addArg("-t " + deadline.seconds)
                }
                Common.OS_WINDOWS -> {
                    logger.info("Deadline is not supported on Windows")
                }
                else -> {
                    ping.addArg("-w " + deadline.seconds)
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
//        ping.command("ping $host")

        // wait for it to finish running
        val output: String = ping.enableRead().startAsShellBlocking().output.utf8()
        return PingResultBuilder.fromOutput(output)
    }
}
