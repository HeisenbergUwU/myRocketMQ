package org.syntax;

import java.util.zip.CRC32;

public class testCRC32 {
    public static void main(String[] args) {
        CRC32 crc = new CRC32();
        byte[] data = "Hello".getBytes();
        crc.update(data, 0, data.length);
        long checksum = crc.getValue();  // CRC‑32 的值（0～0xFFFFFFFF）
        System.out.println(checksum);
    }
}
