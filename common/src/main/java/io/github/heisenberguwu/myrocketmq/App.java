package io.github.heisenberguwu.myrocketmq;

import io.github.heisenberguwu.myrocketmq.attribute.AttributeUtil;
import io.github.heisenberguwu.myrocketmq.constant.GrpcConstants;
import io.github.heisenberguwu.myrocketmq.example.spi.GreetingService;
import io.grpc.Metadata;

import java.util.*;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        AttributeUtil attributeUtil = new AttributeUtil();
        System.out.println(attributeUtil);

    }

    static void testSPI() {
        ServiceLoader<GreetingService> loader = ServiceLoader.load(GreetingService.class);

        for (GreetingService service : loader) {
            service.greet("ChatGPT");
        }
    }

}
