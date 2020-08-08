package dorkbox.netUtil.ping

import dorkbox.netUtil.Common
import dorkbox.netUtil.IPv4
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.regex.Matcher

internal object PingResultBuilder {

    private val roundTripTimeParser: (PingResult, Matcher) -> PingResult = { result, matcher ->
        val minRTT = matcher.group(1).toDouble()
        val avgRTT = matcher.group(2).toDouble()
        val maxRTT = matcher.group(3).toDouble()
        val mdevRTT = matcher.group(4).toDouble()

        result.minRoundTripTime = Duration.ofNanos((1000 * 1000 * minRTT).toLong())
        result.avgRoundTripTime = Duration.ofNanos((1000 * 1000 * avgRTT).toLong())
        result.maxRoundTripTime = Duration.ofNanos((1000 * 1000 * maxRTT).toLong())
        result.mdevRoundTripTime = Duration.ofNanos((1000 * 1000 * mdevRTT).toLong())

        result
    }


    private val resultParsers =
            when {
                Common.OS_MAC -> {
                    listOf(
                        /* BSD Ping (MacOS) */
                        ResultParser.of("(.*) packets transmitted, (.*) packets received, (.*)% packet loss") { result, matcher ->
                            val transmittedPackets: Int = matcher.group(1).toInt()
                            result.transmittedPackets = transmittedPackets

                            val receivedPackets: Int = matcher.group(2).toInt()
                            result.receivedPackets = receivedPackets

                            val packetLoss: Double = 0.01 * matcher.group(3).toDouble()
                            result.packetLoss = packetLoss

                            result
                        },
                        ResultParser.of("round-trip min\\/avg\\/max\\/stddev = (.*)\\/(.*)\\/(.*)\\/(.*) ms", roundTripTimeParser),
                        ResultParser.of("(.*) bytes from (.*): icmp_seq=(.*) ttl=(.*) time=(.*) ms") { result, matcher ->
                            val bytes: Int = matcher.group(1).toInt()
                            val host: String = matcher.group(2)
                            val icmpSeq: Int = matcher.group(3).toInt()
                            val ttl: Int = matcher.group(4).toInt()
                            val time = Duration.ofNanos((1000 * 1000 * matcher.group(5).toDouble()).toLong())
                            val response = PingResult.Response(bytes, host, icmpSeq, ttl, time)

                            result.responses.add(response)
                            result
                        }
                    )
                }
                Common.OS_WINDOWS -> {
                    listOf(
                        /* Windows */
                        ResultParser.of("Pinging (.*) with") { result, matcher ->
                            result.host = IPv4.WILDCARD // note: this is REALLY the host used for default traffic
                            result.ip = matcher.group(1)
                            result
                        },
                        ResultParser.of("(.*) packets transmitted, (.*) packets received, (.*)% packet loss") { result, matcher ->
                            val transmittedPackets: Int = matcher.group(1).toInt()
                            result.transmittedPackets = transmittedPackets

                            val receivedPackets: Int = matcher.group(2).toInt()
                            result.receivedPackets = receivedPackets

                            val packetLoss: Double = 0.01 * matcher.group(3).toDouble()
                            result.packetLoss = packetLoss

                            result
                        },
                        ResultParser.of("round-trip min\\/avg\\/max\\/stddev = (.*)\\/(.*)\\/(.*)\\/(.*) ms", roundTripTimeParser),
                        ResultParser.of("(.*) bytes from (.*): icmp_seq=(.*) ttl=(.*) time=(.*) ms") { result, matcher ->
                            val bytes: Int = matcher.group(1).toInt()
                            val host: String = matcher.group(2)
                            val icmpSeq: Int = matcher.group(3).toInt()
                            val ttl: Int = matcher.group(4).toInt()
                            val time = Duration.ofNanos((1000 * 1000 * matcher.group(5).toDouble()) as Long)
                            val response = PingResult.Response(bytes, host, icmpSeq, ttl, time)

                            result.responses.add(response)
                            result
                        }
                    )
                }
                else -> {
                    listOf(
                        /* GNU Ping (Debian, etc.) */
                        ResultParser.of("PING (.*) \\((.*?)\\)") { result, matcher ->
                            result.host = matcher.group(1)
                            result.ip = matcher.group(2)
                            result
                        },
                        ResultParser.of("(.*) packets transmitted, (.*) received, (.*)% packet loss, time (.*)ms") { result, matcher ->
                            val transmittedPackets: Int = matcher.group(1).toInt()
                            result.transmittedPackets = transmittedPackets

                            val receivedPackets: Int = matcher.group(2).toInt()
                            result.receivedPackets = receivedPackets

                            val packetLoss: Double = 0.01 * Integer.valueOf(matcher.group(3))
                            result.packetLoss = packetLoss

                            val time = Duration.of(matcher.group(4).toLong(), ChronoUnit.MILLIS)
                            result.time = time

                            result
                        },
                        ResultParser.of("rtt min\\/avg\\/max\\/mdev = (.*)\\/(.*)\\/(.*)\\/(.*) ms", roundTripTimeParser)
                    )
                }
            }


    fun fromOutput(output: String): PingResult {
        val pingResult = PingResult()

        resultParsers.forEach { rp: ResultParser ->
            rp.fill(pingResult, output)
        }

        return pingResult
    }
}
