package dorkbox.netUtil

/**
 *
 */
object VirtualEth {
    fun add(host: String?, guest: String?) {
        // ShellExecutor.Companion.run("/sbin/ip", "link add name " + host + " type veth peer name " + guest);
        throw RuntimeException("NOT IMPL.")
    }

    fun delete(host: String?) {
        // ShellExecutor.Companion.run("/sbin/ip", "link del " + host);
        throw RuntimeException("NOT IMPL.")
    }

    fun assignNameSpace(nameSpace: String?, guest: String?) {
        // ShellExecutor.Companion.run("/sbin/ip", "link set " + guest + " netns " + nameSpace);
        throw RuntimeException("NOT IMPL.")
    }
}
