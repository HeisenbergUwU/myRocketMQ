package org.syntax.log;

import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

public class testLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerName.ROCKETMQ_REMOTING_NAME);
    public static void main(String[] args) {
        LOGGER.error("TEST");
    }
}
