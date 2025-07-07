package org.apache.rocketmq.common.utils;

import org.apache.commons.codec.binary.Hex;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BinaryUtil {
    public static byte[] calculateMd5(byte[] binaryData) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found.");
        }
        /**
         * 用于累积数据。MessageDigest 是有状态的：你可以多次调用 update(byte[])，它会将多次传入的字节“拼接”在一起，直到调用 digest() 结束：
         * 常见用法是在处理大型数据时，分块读取并多次 update(...)，效率更好，避免一次性全量载入内存。越大的块，一次 update(...) 越高效。
         */
        messageDigest.update(binaryData);
        return messageDigest.digest();
    }

    public static String generateMd5(String bodyStr) {
        byte[] bytes = calculateMd5(bodyStr.getBytes(Charset.forName("UTF-8")));
        return Hex.encodeHexString(bytes, false);
    }

    public static String generateMd5(byte[] content) {
        byte[] bytes = calculateMd5(content);
        return Hex.encodeHexString(bytes, false);
    }


    /**
     * Returns true if subject contains only bytes that are spec-compliant ASCII characters.
     *
     * @param subject
     * @return
     */
    public static boolean isAscii(byte[] subject) {
        if (subject == null) {
            return false;
        }
        for (byte b : subject) {
            if (b < 32 || b > 126) {
                return false;
            }
        }
        return true;
    }
}