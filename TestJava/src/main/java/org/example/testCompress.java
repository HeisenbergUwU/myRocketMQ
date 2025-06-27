package org.example;

import org.example.compress.ZstdCompressor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class testCompress {
    /*
    添加 Zstd 压缩算法包装对象
     */
    public static void main(String[] args) throws IOException {
        ZstdCompressor compressor = new ZstdCompressor();

        byte[] in = "Hello world.".getBytes();

        byte[] compress = compressor.compress(in, 5);

        System.out.println(in.length);
        System.out.println(compress.length); // 压缩之后反而更大了
//        System.out.println(Arrays.toString(compress));
        System.out.println(new String(compress, StandardCharsets.UTF_8));

        byte[] decompress = compressor.decompress(compress);
        System.out.println(new String(decompress, StandardCharsets.UTF_8));
    }
}
