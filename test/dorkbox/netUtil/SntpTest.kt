/*
 * Copyright 2012 The Netty Project
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

import org.junit.Assert
import org.junit.Test

class SntpTest {
    @Test
    fun testToString() {
        val ntp = Sntp.update("time.mit.edu")

        // simple test that we got something
        Assert.assertTrue(ntp.roundTripDelay > 0)

        // Display response
        val now = System.currentTimeMillis()
        val cor = now + ntp.localClockOffset
        val cor2 = ntp.now

        System.out.printf ("Originate time:    %1\$ta, %1\$td %1\$tb %1\$tY, %1\$tI:%1\$tm:%1\$tS.%1\$tL %1\$tp %1\$tZ%n", ntp.originateTimestamp);
        System.out.printf ("Receive time:      %1\$ta, %1\$td %1\$tb %1\$tY, %1\$tI:%1\$tm:%1\$tS.%1\$tL %1\$tp %1\$tZ%n", ntp.receiveTimestamp)
        System.out.printf ("Transmit time:     %1\$ta, %1\$td %1\$tb %1\$tY, %1\$tI:%1\$tm:%1\$tS.%1\$tL %1\$tp %1\$tZ%n", ntp.transmitTimestamp)
        System.out.printf ("Destination time:  %1\$ta, %1\$td %1\$tb %1\$tY, %1\$tI:%1\$tm:%1\$tS.%1\$tL %1\$tp %1\$tZ%n", ntp.destinationTimestamp)

        System.out.println("RoundTripDelay :  ${ntp.roundTripDelay}")
        System.out.println("ClockOffset    :  ${ntp.localClockOffset}")

        System.out.printf ("Local time:      %1\$ta, %1\$td %1\$tb %1\$tY, %1\$tI:%1\$tm:%1\$tS.%1\$tL %1\$tp %1\$tZ%n", now)
        System.out.printf ("Corrected time:  %1\$ta, %1\$td %1\$tb %1\$tY, %1\$tI:%1\$tm:%1\$tS.%1\$tL %1\$tp %1\$tZ%n", cor)
        System.out.printf ("Corrected time2: %1\$ta, %1\$td %1\$tb %1\$tY, %1\$tI:%1\$tm:%1\$tS.%1\$tL %1\$tp %1\$tZ%n", cor2)

        Assert.assertEquals(cor, cor2)
    }
}
