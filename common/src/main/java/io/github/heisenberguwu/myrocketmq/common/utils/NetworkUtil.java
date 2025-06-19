package io.github.heisenberguwu.myrocketmq.common.utils;

import io.github.heisenberguwu.myrocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;

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

    /**
     * SelectionKey 选择器 ** Selector ** 的获取方法
     * Selector - 监听事件 - SelectionKey - 绑定Channel - Channel - 封装 - Socket
     * @return
     * @throws IOException
     */
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
                try {
                    // 获取 provider() 静态方法，这里是无参数的。
                    // 举个例子，如果我想要第一个参数是String 第二个参数是int的provider方法
                    // providerClazz.getMethod("provider",String.class,int.class)
                    final Method method = providerClazz.getMethod("provider");
                    // 调用静态方法，obj参数为null 是因为调用的是静态方法。

                    final SelectorProvider selectorProvider = (SelectorProvider) method.invoke(null);
                    // 通过提供者返回，AbstractSelector的实例，如果执行到这里肯定是EpollSelectorxxxxx
                    if (selectorProvider != null) {
                        result = selectorProvider.openSelector();
                    }
                } catch (final Exception e) {
                    // final 的意图就是额外保证在未来的异常传递中不要被修改了，这只是一种风格。
                    log.warn("Open ePoll Selector for linux platform exception", e);
                }
            } catch (final Exception e) {
                // ignore
            }
        }

        // 如果上面一顿骚操作失败了，那么就老老实实创建一个 selector
        if (result == null) {
            result = Selector.open();
        }

        return result;
    }

    public static List<InetAddress getLocalInetAddressList() throws SocketException
    {

    }
}