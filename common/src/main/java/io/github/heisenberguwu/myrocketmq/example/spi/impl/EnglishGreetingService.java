package io.github.heisenberguwu.myrocketmq.example.spi.impl;

import io.github.heisenberguwu.myrocketmq.example.spi.GreetingService;

public class EnglishGreetingService implements GreetingService {
    @Override
    public void greet(String name) {
        System.out.println("Hello " + name + " !");
    }
}
