package io.github.heisenberguwu.myrocketmq.example.spi.impl;

import io.github.heisenberguwu.myrocketmq.example.spi.GreetingService;

public class ChineseGreetingService implements GreetingService {
    @Override
    public void greet(String name) {
        System.out.println("你好，" + name + "！");
    }
}