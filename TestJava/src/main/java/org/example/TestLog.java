package org.example;

import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

public class TestLog {
    private static final Logger log = LoggerFactory.getLogger("TEST");

    public static void main(String[] args) {
        log.error(("HELLO WORLD."));

        Integer i = new Integer(128);
        Integer j = new Integer(128);
        System.out.println(i == j);

        Integer a = new Integer(1);
        Integer b = new Integer(1);
        System.out.println(a == b);
    }
}
