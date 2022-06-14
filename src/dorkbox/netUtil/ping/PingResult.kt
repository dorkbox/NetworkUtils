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

import java.time.Duration

class PingResult {
    var host: String? = null
    var ip: String? = null
    val responses= arrayListOf<Response>()
    var transmittedPackets = 0
    var receivedPackets = 0
    var packetLoss = 0.0
    var time: Duration? = null
    var minRoundTripTime: Duration? = null
    var avgRoundTripTime: Duration? = null
    var maxRoundTripTime: Duration? = null
    var mdevRoundTripTime: Duration? = null

    data class Response(val bytes: Int,
                   val host: String,
                   val icmpSeq: Int,
                   val ttl: Int,
                   val time: Duration)

    override fun toString(): String {
        return "PingResult(host=$host, ip=$ip, responses=$responses, transmittedPackets=$transmittedPackets, receivedPackets=$receivedPackets, packetLoss=$packetLoss%, time=$time, minRoundTripTime=$minRoundTripTime, avgRoundTripTime=$avgRoundTripTime, maxRoundTripTime=$maxRoundTripTime, mdevRoundTripTime=$mdevRoundTripTime)"
    }
}
