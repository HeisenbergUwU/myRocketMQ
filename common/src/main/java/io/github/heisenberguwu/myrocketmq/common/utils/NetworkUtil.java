package io.github.heisenberguwu.myrocketmq.common.utils;

import io.github.heisenberguwu.myrocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Selector;

public class NetworkUtil {
    public static final String OS_NAME = System.getProperty("os.name"); // 通过JVM参数获取操作系统

    private static final Logger log = LoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);
    private static boolean isLinuxPlatform = false;
    private static boolean isWindowsPlatform = false;

    // 初始化时候执行
    // 判断系统类型
    static {
        if (OS_NAME != null && OS_NAME.toLowerCase().contains("linux")) {
            isLinuxPlatform = true;
        }

        if (OS_NAME != null && OS_NAME.toLowerCase().contains("windows")) {
            isWindowsPlatform = true;
        }
    }

    public static boolean isWindowsPlatform() {
        return isWindowsPlatform;
    }

    public static boolean isLinuxPlatform() {
        return isLinuxPlatform;
    }

    public static Selector openSelector() throws IOException {
        Selector result = null;
        if (isLinuxPlatform()) {
            try {
                /**
                 * 代码里不能直接硬编码用EPoll类，否则在非Linux环境跑就会出错。
                 *
                 * 用反射可以“试探性地加载”：Linux系统存在才加载，不存在就回退使用普通NIO Selector。
                 *
                 * 你调用 Selector.open()，它会调用默认的 SelectorProvider.provider() 来获取平台默认的 SelectorProvider。
                 *
                 * JDK HotSpot 在 Linux 下，默认就是用 EPollSelectorProvider，所以一般来说你不用特殊处理，也会走 epoll。
                 *
                 * 但是 JDK 其他版本的不一定使用的是 Epoll
                 */
                final Class<?> providerClazz = Class.forName("sun.nio.ch.EPollSelectorProvider");

            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

    }
}