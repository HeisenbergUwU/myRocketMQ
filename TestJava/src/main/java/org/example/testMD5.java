package org.example;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class testMD5 {
    public static void main(String[] args) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        byte[] target = "Hello world".getBytes();

        System.out.println(Arrays.toString(md5.digest(target)));

    }
}
