package org.apache.rocketmq.common.example.spi.impl;

import org.apache.rocketmq.common.example.spi.GreetingService;

public class ChineseGreetingService implements GreetingService {
    @Override
    public void greet(String name) {
        System.out.println("你好，" + name + "！");
    }
}