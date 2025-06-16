package io.github.heisenberguwu.myrocketmq;

import io.github.heisenberguwu.myrocketmq.example.spi.GreetingService;

import java.util.ServiceLoader;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        testSPI();
    }

    static void testSPI(){
        ServiceLoader<GreetingService> loader = ServiceLoader.load(GreetingService.class);

        for (GreetingService service : loader) {
            service.greet("ChatGPT");
        }
    }

}
