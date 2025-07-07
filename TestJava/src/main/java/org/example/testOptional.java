package org.example;

import java.util.Optional;

public class testOptional {
    public static void main(String[] args) {
        String a = "hello";
        a = null;
        Optional<String> hello = Optional.ofNullable(a);
        System.out.println(hello.isPresent());
    }
}
