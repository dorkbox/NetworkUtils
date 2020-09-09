/*
 * Copyright 2020 dorkbox, llc
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

import dorkbox.gradle.kotlin
import java.time.Instant

///////////////////////////////
//////    PUBLISH TO SONATYPE / MAVEN CENTRAL
////// TESTING : (to local maven repo) <'publish and release' - 'publishToMavenLocal'>
////// RELEASE : (to sonatype/maven central), <'publish and release' - 'publishToSonatypeAndRelease'>
///////////////////////////////

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS   // always show the stacktrace!
gradle.startParameter.warningMode = WarningMode.All


plugins {
    java

    id("com.dorkbox.GradleUtils") version "1.10"
    id("com.dorkbox.Licensing") version "2.2"
    id("com.dorkbox.VersionUpdate") version "2.0"
    id("com.dorkbox.GradlePublish") version "1.6"
    id("com.dorkbox.GradleModuleInfo") version "1.0"

    kotlin("jvm") version "1.4.0"
}

object Extras {
    // set for the project
    const val description = "Utilities for managing network configurations, IP/MAC address conversion, and ping (via OS native commands)"
    const val group = "com.dorkbox"
    const val version = "1.5"

    // set as project.ext
    const val name = "NetworkUtils"
    const val id = "NetworkUtils"
    const val vendor = "Dorkbox LLC"
    const val vendorUrl = "https://dorkbox.com/"
    const val url = "https://git.dorkbox.com/dorkbox/NetworkUtils"

    val buildDate = Instant.now().toString()
}

///////////////////////////////
/////  assign 'Extras'
///////////////////////////////
GradleUtils.load("$projectDir/../../gradle.properties", Extras)
GradleUtils.fixIntellijPaths()
GradleUtils.defaultResolutionStrategy()
GradleUtils.compileConfiguration(JavaVersion.VERSION_11)


licensing {
    license(License.APACHE_2) {
        description(Extras.description)
        url(Extras.url)
        author(Extras.vendor)
        extra("Netty", License.APACHE_2) {
            it.copyright(2014)
            it.author("The Netty Project")
            it.url("https://netty.io/")
            it.note("This product contains a modified portion of Netty Network Utils")
        }
        extra("Apache Harmony", License.APACHE_2) {
            it.copyright(2010)
            it.author("The Apache Software Foundation")
            it.url("http://archive.apache.org/dist/harmony/")
            it.note("This product contains a modified portion of 'Apache Harmony', an open source Java SE")
        }
    }
}


sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))

            // want to include java files for the source. 'setSrcDirs' resets includes...
            include("**/*.java")
        }

        kotlin {
            setSrcDirs(listOf("src"))

            // want to include java files for the source. 'setSrcDirs' resets includes...
            include("**/*.java", "**/*.kt")
        }
    }

    test {
        java {
            setSrcDirs(listOf("test"))

            // want to include java files for the source. 'setSrcDirs' resets includes...
            include("**/*.java")
        }

        kotlin {
            setSrcDirs(listOf("test"))

            // want to include java files for the source. 'setSrcDirs' resets includes...
            include("**/*.java", "**/*.kt")
        }
    }
}

repositories {
    mavenLocal() // this must be first!
    jcenter()
}


tasks.jar.get().apply {
    manifest {
        // https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html
        attributes["Name"] = Extras.name

        attributes["Specification-Title"] = Extras.name
        attributes["Specification-Version"] = Extras.version
        attributes["Specification-Vendor"] = Extras.vendor

        attributes["Implementation-Title"] = "${Extras.group}.${Extras.id}"
        attributes["Implementation-Version"] = Extras.buildDate
        attributes["Implementation-Vendor"] = Extras.vendor

        attributes["Automatic-Module-Name"] = Extras.id
    }
}

dependencies {
    // https://github.com/MicroUtils/kotlin-logging
    implementation("io.github.microutils:kotlin-logging:1.8.3")  // slick kotlin wrapper for slf4j
    implementation("org.slf4j:slf4j-api:1.7.30")
    
    implementation("com.dorkbox:Executor:1.1")

    testImplementation("junit:junit:4.13")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("com.dorkbox:Utilities:1.6")
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
