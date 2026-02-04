/*
 * Copyright 2026 dorkbox, llc
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

import de.undercouch.gradle.tasks.download.Download

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS   // always show the stacktrace!
gradle.startParameter.warningMode = WarningMode.All


plugins {
    id("com.dorkbox.GradleUtils") version "4.8"
    id("com.dorkbox.Licensing") version "3.1"
    id("com.dorkbox.VersionUpdate") version "3.2"
    id("com.dorkbox.GradlePublish") version "2.2"

    id("de.undercouch.download") version "5.4.0"

    kotlin("jvm") version "2.3.0"
}

GradleUtils.load {
    group = "com.dorkbox"
    id = "NetworkUtils"

    description = "Utilities for managing network configurations, IP/MAC address conversion, and ping"
    name = "NetworkUtils"
    version = "2.24"

    vendor = "Dorkbox LLC"
    vendorUrl = "https://dorkbox.com/"

    url = "https://git.dorkbox.com/dorkbox/NetworkUtils"

    issueManagement {
        url = "${url}/issues"
        nickname = "Gitea Issues"
    }

    developer {
        id = "dorkbox"
        name = vendor
        email = "email@dorkbox.com"
    }
}
GradleUtils.defaults()
GradleUtils.compileConfiguration(JavaVersion.VERSION_25)


licensing {
    license(License.APACHE_2) {
        description(Extras.description)
        url(Extras.url)
        author(Extras.vendor)

        extra("Netty", License.APACHE_2) {
            copyright(2014)
            author("The Netty Project")
            url("https://netty.io")
            note("This product contains a modified portion of Netty Network Utils")
        }

        extra("Apache Harmony", License.APACHE_2) {
            copyright(2010)
            author("The Apache Software Foundation")
            url("https://archive.apache.org/dist/harmony/")
            note("This product contains a modified portion of 'Apache Harmony', an open source Java SE")
        }
        extra("Mozilla Public Suffix List", License.MOZILLA_2) {
            copyright(2010)
            author("The Apache Software Foundation")
            url("https://publicsuffix.org/list/public_suffix_list.dat")
        }
        extra("Apache HTTP Utils", License.APACHE_2) {
            copyright(2010)
            author("The Apache Software Foundation")
            url("https://svn.apache.org/repos/asf/httpcomponents/httpclient/trunk/httpclient5/src/main/java/org/apache/hc/client5/http/psl/")
            note("This product contains a modified portion of 'PublicSuffixDomainFilter.java'")
        }
        extra("UrlRewriteFilter", License.BSD_3) {
            description("UrlRewriteFilter is a Java Web Filter for any J2EE compliant web application server")
            url("https://github.com/paultuckey/urlrewritefilter")
            copyright(2022)
            author("Paul Tuckey")
        }
    }
}

tasks.register<Download>("updateTldList") {
    src("https://publicsuffix.org/list/public_suffix_list.dat")
    dest(file("resources/public_suffix_list.dat")) // Destination file path
    overwrite(true)

    tasks.jar.get().finalizedBy(this)
}


dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")

    implementation("org.slf4j:slf4j-api:2.0.7")

    api("com.dorkbox:Executor:3.13")
    api("com.dorkbox:Updates:1.1")

    val jnaVersion = "5.13.0"
    api("net.java.dev.jna:jna-jpms:$jnaVersion")
    api("net.java.dev.jna:jna-platform-jpms:$jnaVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("ch.qos.logback:logback-classic:1.4.4")
}
