package io.github.heisenberguwu.myrocketmq.common.utils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class IOTinyUtils {
    static public String toString(InputStream input, String encoding) throws IOException {
        return (null == encoding) ? toString(new InputStreamReader(input, StandardCharsets.UTF_8)) : toString(new InputStreamReader(
                input, encoding));
    }

    static public String toString(Reader reader) throws IOException {
        /**
         * Reader 是一个抽象类（public abstract class Reader implements Readable, Closeable
         *
         * InputStreamReader：将 byte 流转换为字符流
         *
         * FileReader：直接读取文件字符
         *
         * BufferedReader：缓冲字符读取，支持按行读
         *
         * CharArrayReader, StringReader, PipedReader, FilterReader 等其他专用子类
         */
        CharArrayWriter sw = new CharArrayWriter();
        copy(reader, sw);
        return sw.toString();
    }

    static public long copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[1 << 12]; // 4096
        long count = 0;
        for (int n = 0; (n = input.read(buffer)) >= 0; ) {
            // read 每次最多不会超过 buffer.length 个字节数
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

    static public void coyFile(String source, String target) throws IOException {
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
            tc = new FileOutputStream(tf).getChannel();
            sc = new FileInputStream(sf).getChannel();
            sc.transferTo(0, sc.size(), tc);
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
        // 这俩函数递归调用了，主要是为了删赶紧文件件下左右的内容
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