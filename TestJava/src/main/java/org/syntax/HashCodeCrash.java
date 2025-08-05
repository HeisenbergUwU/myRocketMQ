package org.syntax;

public class HashCodeCrash {
    public static void main(String[] args) {
        String str1 = "FB";
        String str2 = "Ea";

        // 这两个字符串是知名的 hashCode 碰撞对（在 Java String 的 hashCode 实现下）
        System.out.println("str1 = " + str1 + ", hashCode = " + str1.hashCode());
        System.out.println("str2 = " + str2 + ", hashCode = " + str2.hashCode());
        System.out.println("equals? " + str1.equals(str2));
    }
}
