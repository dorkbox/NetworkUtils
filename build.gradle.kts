/*
 * Copyright 2023 dorkbox, llc
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

///////////////////////////////
//////    PUBLISH TO SONATYPE / MAVEN CENTRAL
////// TESTING : (to local maven repo) <'publish and release' - 'publishToMavenLocal'>
////// RELEASE : (to sonatype/maven central), <'publish and release' - 'publishToSonatypeAndRelease'>
///////////////////////////////

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS   // always show the stacktrace!
gradle.startParameter.warningMode = WarningMode.All


plugins {
    id("com.dorkbox.GradleUtils") version "3.18"
    id("com.dorkbox.Licensing") version "2.25"
    id("com.dorkbox.VersionUpdate") version "2.8"
    id("com.dorkbox.GradlePublish") version "1.20"

    id("de.undercouch.download") version "5.4.0"

    kotlin("jvm") version "1.9.0"
}

object Extras {
    // set for the project
    const val description = "Utilities for managing network configurations, IP/MAC address conversion, and ping (via OS native commands)"
    const val group = "com.dorkbox"
    const val version = "2.24"

    // set as project.ext
    const val name = "NetworkUtils"
    const val id = "NetworkUtils"
    const val vendor = "Dorkbox LLC"
    const val vendorUrl = "https://dorkbox.com/"
    const val url = "https://git.dorkbox.com/dorkbox/NetworkUtils"
}

///////////////////////////////
/////  assign 'Extras'
///////////////////////////////
GradleUtils.load("$projectDir/../../gradle.properties", Extras)
GradleUtils.defaults()
GradleUtils.compileConfiguration(JavaVersion.VERSION_1_8)
GradleUtils.jpms(JavaVersion.VERSION_1_9)

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

tasks.jar.get().apply {
    manifest {
        // https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html
        attributes["Name"] = Extras.name

        attributes["Specification-Title"] = Extras.name
        attributes["Specification-Version"] = Extras.version
        attributes["Specification-Vendor"] = Extras.vendor

        attributes["Implementation-Title"] = "${Extras.group}.${Extras.id}"
        attributes["Implementation-Version"] = GradleUtils.now()
        attributes["Implementation-Vendor"] = Extras.vendor
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

publishToSonatype {
    groupId = Extras.group
    artifactId = Extras.id
    version = Extras.version

    name = Extras.name
    description = Extras.description
    url = Extras.url

    vendor = Extras.vendor
    vendorUrl = Extras.vendorUrl

    issueManagement {
        url = "${Extras.url}/issues"
        nickname = "Gitea Issues"
    }

    developer {
        id = "dorkbox"
        name = Extras.vendor
        email = "email@dorkbox.com"
    }
}
