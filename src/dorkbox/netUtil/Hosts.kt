package dorkbox.netUtil

import dorkbox.executor.Executor
import java.net.NetworkInterface



/**
 * partially from: https://stackoverflow.com/questions/7348711/recommended-way-to-get-hostname-in-java/7353473#7353473
 */
object Hosts {
    /**
     * Gets the version number.
     */
    const val version = Common.version

    /**
     * Gets the hostname of the machine, using a variety of different methods.
     */
    val name: String by lazy {
        var hostName: String?

        hostName = Executor.run("hostname")

        if (hostName.isEmpty() || hostName == "localhost") {
            try {
                // note: this doesn't always work!
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val nic: NetworkInterface = interfaces.nextElement()
                    val addresses = nic.inetAddresses
                    while (hostName == null && addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress) {
                            hostName = address.canonicalHostName
                        }
                    }
                }
            }catch (ignored: Exception) {
            }
        }

        if (hostName.isNullOrEmpty() || hostName == "localhost") {
            hostName = System.getenv("COMPUTERNAME")
        }

        if (hostName.isNullOrEmpty() || hostName == "localhost") {
            hostName = System.getenv("HOSTNAME")
        }

        if (hostName.isNullOrEmpty()) {
            hostName = "localhost"
        }

        hostName
    }
}
