package dorkbox.netUtil.ping

import java.time.Duration
import java.util.*

class PingResult {
    var host: String? = null
    var ip: String? = null
    var responses: MutableList<Response> = LinkedList()
    var transmittedPackets = 0
    var receivedPackets = 0
    var packetLoss = 0.0
    var time: Duration? = null
    var minRoundTripTime: Duration? = null
    var avgRoundTripTime: Duration? = null
    var maxRoundTripTime: Duration? = null
    var mdevRoundTripTime: Duration? = null

    class Response(val bytes: Int,
                   val host: String,
                   val icmpSeq: Int,
                   val ttl: Int,
                   val time: Duration)

    override fun toString(): String {
        return "PingResult(host=$host, ip=$ip, responses=$responses, transmittedPackets=$transmittedPackets, receivedPackets=$receivedPackets, packetLoss=$packetLoss, time=$time, minRoundTripTime=$minRoundTripTime, avgRoundTripTime=$avgRoundTripTime, maxRoundTripTime=$maxRoundTripTime, mdevRoundTripTime=$mdevRoundTripTime)"
    }
}
