package org.example.compress;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ZstdCompressor {
    public byte[] compress(byte[] src, int level) throws IOException {
        byte[] result = src;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(src.length);
        ZstdOutputStream outputStream = new ZstdOutputStream(byteArrayOutputStream, level);
        try {
            outputStream.write(src);
            outputStream.flush();
            outputStream.close();
            result = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            try {
                byteArrayOutputStream.close();
            } catch (IOException ignored) {
            }
        }
        return result;
    }

    public byte[] decompress(byte[] src) throws IOException {
        byte[] result = src;
        byte[] uncompressData = new byte[src.length];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(src); // 输入流
        ZstdInputStream zstdInputStream = new ZstdInputStream(byteArrayInputStream); // 输入压缩工具流
        ByteArrayOutputStream resultOutputStream = new ByteArrayOutputStream(src.length); // 输出工具流
        try {
            while (true) {
                int len = zstdInputStream.read(uncompressData, 0, uncompressData.length);
                if (len <= 0) {
                    break;
                }
                resultOutputStream.write(uncompressData, 0, len);
            }
            resultOutputStream.flush();
            resultOutputStream.close();
            result = resultOutputStream.toByteArray();
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                zstdInputStream.close();
                byteArrayInputStream.close();
            } catch (IOException e) {
            }
        }

        return result;
    }
}