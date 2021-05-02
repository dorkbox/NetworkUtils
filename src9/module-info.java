module dorkbox.netutil {
    exports dorkbox.netUtil;
    exports dorkbox.netUtil.dnsUtils;
    exports dorkbox.netUtil.jna;
    exports dorkbox.netUtil.ping;

    requires dorkbox.executor;
    requires dorkbox.updates;

    requires org.slf4j;

    requires com.sun.jna;
    requires com.sun.jna.platform;

    requires kotlin.stdlib;
    requires java.base;
}
