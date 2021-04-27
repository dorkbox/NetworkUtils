package dorkbox.netUtil

import dorkbox.executor.Executor
import java.io.File

/**
 *
 */
object NameSpace {
    /**
     * Gets the version number.
     */
    const val version = "2.6"

    object Route {
        fun flush(nameSpace: String) {
            if (Common.OS_LINUX) {
                run(nameSpace, "/sbin/ip", "route", "flush", "cache")
            }
            else {
                throw RuntimeException("NOT IMPL.")
            }
        }
    }

    object Dhcp {
        object Dhcp {
            fun start(nameSpace: String, id: String, interfaceName: String) {
                if (Common.OS_LINUX) {
                    stop(nameSpace, id, interfaceName)
                    val dhcpPidFile = "/var/run/dhclient-$id.pid"
                    run(nameSpace, "/sbin/dhclient", "-pf", dhcpPidFile, interfaceName)
                } else {
                    throw RuntimeException("NOT IMPL.")
                }
            }

            fun stop(nameSpace: String, id: String, interfaceName: String) {
                if (Common.OS_LINUX) {
                    val dhcpPidFile = "/var/run/dhclient-$id.pid"

                    // close the dhclient if it was already running (based on pid file), and delete the pid file
                    run(nameSpace, "/sbin/dhclient", "-r -pf", dhcpPidFile, interfaceName)

                    // short break
                    try {
                        Thread.sleep(500)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    File(dhcpPidFile).delete()
                } else {
                    throw RuntimeException("NOT IMPL.")
                }
            }
        }
    }

    private val nameSpaceToIifToIp: MutableMap<String, MutableMap<String, String>> = HashMap()

    fun add(nameSpace: String) {
        if (Common.OS_LINUX) {
             Executor.run("/sbin/ip", "netns", "add", nameSpace)
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    fun delete(nameSpace: String) {
        if (Common.OS_LINUX) {
            Executor.run("/sbin/ip", "netns", "del", nameSpace);
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    fun dhcpStart(nameSpace: String, id: String, interfaceName: String) {
        if (Common.OS_LINUX) {
            dhcpStop(nameSpace, id, interfaceName)
            val dhcpPidFile = "/var/run/dhclient-$id.pid"
            run(nameSpace, "/sbin/dhclient", "-pf", dhcpPidFile, interfaceName)
        } else {
            throw RuntimeException("NOT IMPL.")
        }

    }

    fun dhcpStop(nameSpace: String, id: String, interfaceName: String) {
        if (Common.OS_LINUX) {
            val dhcpPidFile = "/var/run/dhclient-$id.pid"

            // close the dhclient if it was already running (based on pid file), and delete the pid file
            run(nameSpace, "/sbin/dhclient", "-r -pf", dhcpPidFile, interfaceName)

            // short break
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            File(dhcpPidFile).delete()
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    fun run(nameSpace: String, vararg args: String): String {
        if (Common.OS_LINUX) {
            val command = mutableListOf<String>()
            command.add("/sbin/ip")
            command.add("netns")
            command.add("exec")
            command.add(nameSpace)
            command.addAll(listOf(*args))

            return Executor.run(command)
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }

    /**
     * On client disconnect, it will get the IP address for the interface from the cache, instead of from `ifconfig` (since the
     * interface
     * is down at this point)
     */
    fun getIpFromIf(nameSpace: String, interfaceName: String, isOnClientConnect: Boolean): String {
        if (Common.OS_LINUX) {
            if (isOnClientConnect) {
                val ifaceInfo = run(nameSpace, "/sbin/ifconfig", interfaceName)
                val str = "inet addr:"
                var index = ifaceInfo.indexOf(str)
                if (index > -1) {
                    index += str.length
                    val possibleAddr = ifaceInfo.substring(index, ifaceInfo.indexOf(" ", index))
                    Common.logger.debug("Found on '{}' possible addr '{}' : ADD", interfaceName, possibleAddr)
                    synchronized(nameSpaceToIifToIp) {
                        var ifToIp = nameSpaceToIifToIp[nameSpace]
                        if (ifToIp == null) {
                            ifToIp = HashMap()
                            nameSpaceToIifToIp[nameSpace] = ifToIp
                        }
                        ifToIp.put(interfaceName, possibleAddr)
                    }
                    return possibleAddr
                }
            } else {
                var possibleAddr: String? = ""
                synchronized(nameSpaceToIifToIp) {
                    val ifToIp = nameSpaceToIifToIp[nameSpace]
                    if (ifToIp != null) {
                        possibleAddr = ifToIp.remove(interfaceName)
                    }
                }
                Common.logger.debug("Found on '{}' possible addr '{}' : REMOVE", interfaceName, possibleAddr)
                if (possibleAddr != null) {
                    return possibleAddr!!
                }
            }
            return ""
        } else {
            throw RuntimeException("NOT IMPL.")
        }

    }

    fun addLoopback(nameSpace: String) {
        if (Common.OS_LINUX) {
            run(nameSpace, "/sbin/ip link set dev lo up")
            run(nameSpace, "/sbin/ip addr add 127.0.0.1 dev lo")
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }
}
