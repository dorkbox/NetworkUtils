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

import dorkbox.netUtil.Common
import dorkbox.netUtil.IP
import java.time.Duration
import java.util.regex.*

internal object PingResultBuilder {

    private val roundTripTimeParser: (PingResult, Matcher) -> PingResult = { result, matcher ->
        val minRTT = matcher.group(1).toDouble()
        val avgRTT = matcher.group(2).toDouble()
        val maxRTT = matcher.group(3).toDouble()
        val mdevRTT = matcher.group(4).toDouble()

        result.minRoundTripTime = Duration.ofMillis(minRTT.toLong())
        result.avgRoundTripTime = Duration.ofMillis(avgRTT.toLong())
        result.maxRoundTripTime = Duration.ofMillis(maxRTT.toLong())
        result.mdevRoundTripTime = Duration.ofMillis(mdevRTT.toLong())

        result
    }


    private val resultParsers =
            when {
                Common.OS_MAC -> {
                    listOf(
                        /* BSD Ping (MacOS) */
                        ResultParser.of("PING (.*) \\((.*?)\\): (.*) data bytes") { result, matcher ->
                            result.host = IP.lanAddress().hostAddress // note: this is REALLY the host used for lan traffic
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
                            result.host = IP.lanAddress().hostAddress // note: this is REALLY the host used for lan traffic
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
                            val time = Duration.ofMillis(matcher.group(5).toLong())
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

                            val time = Duration.ofMillis(matcher.group(4).toLong())
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
