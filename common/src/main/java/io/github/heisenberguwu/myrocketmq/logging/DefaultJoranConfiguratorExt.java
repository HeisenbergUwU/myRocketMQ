package io.github.heisenberguwu.myrocketmq.logging;

import org.apache.rocketmq.logging.ch.qos.logback.classic.util.DefaultJoranConfigurator;

import java.util.ArrayList;
import java.util.List;

public class DefaultJoranConfiguratorExt extends DefaultJoranConfigurator {

    final public static String TEST_AUTOCONFIG_FILE = "rmq.logback-test.xml";
    final public static String AUTOCONFIG_FILE = "rmq.logback.xml";

    final public static String PROXY_AUTOCONFIG_FILE = "rmq.proxy.logback.xml";
    final public static String BROKER_AUTOCONFIG_FILE = "rmq.broker.logback.xml";

    final public static String NAMESRV_AUTOCONFIG_FILE = "rmq.namesrv.logback.xml";
    final public static String CONTROLLER_AUTOCONFIG_FILE = "rmq.controller.logback.xml";
    final public static String TOOLS_AUTOCONFIG_FILE = "rmq.tools.logback.xml";

    final public static String CLIENT_AUTOCONFIG_FILE = "rmq.client.logback.xml";

    private final List<String> configFiles;

    public DefaultJoranConfiguratorExt() {
        this.configFiles = new ArrayList<>();
        configFiles.add(TEST_AUTOCONFIG_FILE);
        configFiles.add(AUTOCONFIG_FILE);
        configFiles.add(PROXY_AUTOCONFIG_FILE);
        configFiles.add(BROKER_AUTOCONFIG_FILE);
        configFiles.add(NAMESRV_AUTOCONFIG_FILE);
        configFiles.add(CONTROLLER_AUTOCONFIG_FILE);
        configFiles.add(TOOLS_AUTOCONFIG_FILE);
        configFiles.add(CLIENT_AUTOCONFIG_FILE);
    }
}
