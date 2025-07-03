package org.example;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class iterInFor {


    public static void main(String[] args) {

        List<String> paramValues = new ArrayList<>();
        paramValues.add("a");
        paramValues.add("b");
        paramValues.add("c");

        for (Iterator<String> iter = paramValues.iterator(); iter.hasNext(); ) {
            String next = iter.next();
            iter.remove();
        }

        System.out.println(paramValues);
    }
}
