package org.example;

import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

public class TestLog {
    private static final Logger log = LoggerFactory.getLogger("TEST");

    public static void main(String[] args) {
        log.error(("HELLO WORLD."));
    }
}
