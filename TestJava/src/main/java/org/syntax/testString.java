package org.syntax;

public class testString {
    public static void main(String[] args) {
        String hi = "Hello world.";
        int o = hi.indexOf("o");

        String substring = hi.substring(0,o);
        String s = hi.substring(o);

        System.out.println(substring);
        System.out.println(s);
        int i = hi.codePointAt(o);
        System.out.println(i);
    }
}
