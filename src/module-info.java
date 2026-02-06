module dorkbox.netutil {
    exports dorkbox.netUtil;
    exports dorkbox.netUtil.dnsUtils;
    exports dorkbox.netUtil.jna;
    exports dorkbox.netUtil.ping;
    exports dorkbox.netUtil.web;

    requires transitive dorkbox.executor;
    requires transitive dorkbox.updates;

    requires transitive kotlin.stdlib;
    requires transitive kotlinx.coroutines.core;

    requires static com.sun.jna;
    requires static com.sun.jna.platform;

    requires java.naming;

    requires static org.slf4j;
}
