package io.github.heisenberguwu.myrocketmq;

import io.github.heisenberguwu.myrocketmq.example.spi.GreetingService;

import java.util.*;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
//        testSPI();

        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterator<String> it = list.iterator();  // ✅ 可以生成 Iterator

        while (it.hasNext()) {
            String s = it.next();               // 通过 Iterator 遍历 List
            if(s.equals("a"))
            {
                it.remove();
            }
            System.out.println(s);
        }



        System.out.println(list);
    }

    static void testSPI() {
        ServiceLoader<GreetingService> loader = ServiceLoader.load(GreetingService.class);

        for (GreetingService service : loader) {
            service.greet("ChatGPT");
        }
    }

}
