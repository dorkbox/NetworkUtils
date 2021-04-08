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

class DnsTest {
    @Test
    fun lanAddress() {
        Assert.assertNotNull(IP.lanAddress())
    }

    @Test
    fun publicViaHttpAddress() {
        val publicIpViaHttp = IP.publicIpViaHttp()
        Assert.assertNotNull(publicIpViaHttp)
//        println(IP.fromString(publicIpViaHttp!!))
    }

    @Test
    fun dnsNameServers() {
        Assert.assertTrue(Dns.defaultNameServers.isNotEmpty())
    }

    @Test
    fun dnsNdots() {
        Assert.assertTrue(Dns.numberDots >= 1)
    }

    @Test
    fun localhost() {
        Assert.assertNotNull(IP.LOCALHOST)
    }
}
