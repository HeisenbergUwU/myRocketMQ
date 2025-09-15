
package org.apache.rocketmq.common.utils;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class IOTinyUtils {

    static public String toString(InputStream input, String encoding) throws IOException {
        return (null == encoding) ? toString(new InputStreamReader(input, StandardCharsets.UTF_8)) : toString(new InputStreamReader(
            input, encoding));
    }

    static public String toString(Reader reader) throws IOException {
        CharArrayWriter sw = new CharArrayWriter(); // StringWrite 封装了 StringBuffer，性能略低于 CAW
        copy(reader, sw);
        return sw.toString();
    }

    static public long copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[1 << 12];
        long count = 0;
        for (int n = 0; (n = input.read(buffer)) >= 0; ) { // 这里读取必须在for 循环中，如果4096放不下就进入一次循环
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    static public List<String> readLines(Reader input) throws IOException {
        BufferedReader reader = toBufferedReader(input);
        List<String> list = new ArrayList<>();
        String line;
        for (; ; ) {
            line = reader.readLine();
            if (null != line) {
                list.add(line);
            } else {
                break;
            }
        }
        return list;
    }

    static private BufferedReader toBufferedReader(Reader reader) {
        return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    }

    static public void copyFile(String source, String target) throws IOException {
        File sf = new File(source);
        if (!sf.exists()) {
            throw new IllegalArgumentException("source file does not exist.");
        }
        File tf = new File(target);
        tf.getParentFile().mkdirs();
        if (!tf.exists() && !tf.createNewFile()) {
            throw new RuntimeException("failed to create target file.");
        }

        FileChannel sc = null;
        FileChannel tc = null;
        try {
            // Java 1.4（2002）：引入了 java.nio（New I/O），包括 Buffer, Channel, Selector，支持非阻塞、零拷贝、内存映射、多路复用等高性能特性。
            tc = new FileOutputStream(tf).getChannel();
            sc = new FileInputStream(sf).getChannel();
            /**
             * byte[] buffer = new byte[8192];
             * int len;
             * while ((len = inputStream.read(buffer)) != -1) {
             *     outputStream.write(buffer, 0, len);
             * }
             * 数据拷贝路径（4 次）：
             * 磁盘 → 内核缓冲区（DMA）
             * 内核缓冲区 → 用户缓冲区（CPU 拷贝）
             * 用户缓冲区 → 内核缓冲区（CPU 拷贝）
             * 内核缓冲区 → 磁盘/网卡（DMA）
             * 👉 CPU 参与了 2 次数据拷贝，效率较低。
             */
            ///////////////////////////////////////
            /**
             *  数据拷贝路径（理想情况下 2 次）：
             * 磁盘 → 内核缓冲区（DMA）
             * 内核缓冲区 → 目标设备（磁盘/网卡）（DMA，由 sendfile / transfer 系统调用完成）
             */
            sc.transferTo(0, sc.size(), tc);
            // Files.copy(Paths.get("src"), Paths.get("dest"), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (null != sc) {
                sc.close();
            }
            if (null != tc) {
                tc.close();
            }
        }
    }

    public static void delete(File fileOrDir) throws IOException {
        if (fileOrDir == null) {
            return;
        }

        if (fileOrDir.isDirectory()) {
            cleanDirectory(fileOrDir);
        }

        fileOrDir.delete();
    }

    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) { // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (File file : files) {
            try {
                delete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

    public static void writeStringToFile(File file, String data, String encoding) throws IOException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(data.getBytes(encoding));
        } finally {
            if (null != os) {
                os.close();
            }
        }
    }
}
