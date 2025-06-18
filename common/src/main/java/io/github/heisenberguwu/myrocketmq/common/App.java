package io.github.heisenberguwu.myrocketmq.common;

import io.github.heisenberguwu.myrocketmq.common.example.spi.GreetingService;

import java.util.*;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        System.out.println();

    }

    static void testSPI() {
        ServiceLoader<GreetingService> loader = ServiceLoader.load(GreetingService.class);

        for (GreetingService service : loader) {
            service.greet("ChatGPT");
        }
    }

    enum A {
        A, B, C;
    }

}
