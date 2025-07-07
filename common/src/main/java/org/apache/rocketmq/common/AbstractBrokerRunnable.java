package org.apache.rocketmq.common;

import org.apache.rocketmq.logging.org.slf4j.MDC;

import java.io.File;

/**
 * 该类型主要是用来封装 Runnable 啥设置日志上下文，在线程中打印更多详细信息
 */
public abstract class AbstractBrokerRunnable implements Runnable {
    protected final BrokerIdentity brokerIdentity;

    public AbstractBrokerRunnable(BrokerIdentity brokerIdentity) {
        this.brokerIdentity = brokerIdentity;
    }
    /*
    MDC (Mapped Diagnostic Context) 是 日志框架中用于保存线程上线文信息 的一个类
    --- 例如 ---
    MDC.put("requestId", "abc-123-xyz");
    MDC.put("userId", "user-456");

    LOGGER.info("Received new request");
    // 日志输出：2025-06-18 22:30:00 [thread-1] INFO  requestId=abc-123-xyz userId=user-456 Received new request

    MDC.clear(); // 清理，防止线程复用污染
     */
    private static final String MDC_BROKER_CONTAINER_LOG_DIR = "brokerContainerLogDir";

    public abstract void run0();

    @Override
    public void run() {
        try {
            if (brokerIdentity.isInBrokerContainer()) {
                MDC.put(MDC_BROKER_CONTAINER_LOG_DIR, File.separator + brokerIdentity.getCanonicalName());
            }
            run0();
        } finally {
            MDC.clear();
        }
    }
}