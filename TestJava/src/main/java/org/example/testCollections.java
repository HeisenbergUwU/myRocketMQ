package org.example;

import java.util.Collections;
import java.util.List;

public class testCollections {
    public static void main(String[] args) {
        List<String> strings = Collections.singletonList("A");
        strings.add("B"); // UnsupportedOperation
        System.out.println(strings);
    }
}
