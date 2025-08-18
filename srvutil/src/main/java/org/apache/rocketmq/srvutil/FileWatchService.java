package org.apache.rocketmq.srvutil;

import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.rocketmq.common.LifecycleAwareServiceThread;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

public class FileWatchService extends LifecycleAwareServiceThread {
    private static final Logger log = LoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);

    private final Map<String, String> currentHash = new HashMap<>();
    private final Listener listener;
    private static final int WATCH_INTERVAL = 500;
    private final MessageDigest md = MessageDigest.getInstance("MD5");

    public FileWatchService(final String[] watchFiles, final Listener listener) throws Exception {
        this.listener = listener;
        for (String file : watchFiles) {
            if (!Strings.isNullOrEmpty(file) && new File(file).exists()) {
                currentHash.put(file, md5Digest(file));
            }
        }
    }

    @Override
    public void run0() {
        log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            try {
                this.waitForRunning(WATCH_INTERVAL);
                for (Map.Entry<String, String> entry : currentHash.entrySet()) {
                    String newHash = md5Digest(entry.getKey());
                    if (!newHash.equals(entry.getValue())) {
                        entry.setValue(newHash);
                        listener.onChanged(entry.getKey());
                    }
                }
            } catch (Exception e) {
                log.warn(this.getServiceName() + " service raised an unexpected exception.", e);
            }
        }
        log.info(this.getServiceName() + " service end");
    }

    @Override
    public String getServiceName() {
        return "FileWatchService";
    }


    private String md5Digest(String filePath) {
        Path path = Paths.get(filePath);
        if (!path.toFile().exists()) // 如果不存在这个文件
        {
            return currentHash.getOrDefault(filePath, "");
        }
        byte[] raw;
        try {
            raw = Files.readAllBytes(path);
        } catch (IOException e) {
            log.info("Failed to read content of {}", filePath);
            // Reuse previous hash result
            return currentHash.getOrDefault(filePath, "");
        }
        md.update(raw);
        byte[] hash = md.digest();
        return UtilAll.bytes2string(hash);
    }

    public interface Listener {
        /**
         * Will be called when the target files are changed
         *
         * @param path the changed file path
         */
        void onChanged(String path);
    }


    /**
     * 更快的文件拷贝工作
     *
     * import java.io.*;
     * import java.nio.channels.FileChannel;
     * import java.nio.file.*;
     *
     * public class FileCopy {
     *     public static void main(String[] args) throws IOException {
     *         Path sourcePath = Paths.get("source.txt");
     *         Path destPath = Paths.get("destination.txt");
     *
     *         try (FileChannel sourceChannel = FileChannel.open(sourcePath, StandardOpenOption.READ);
     *              FileChannel destChannel = FileChannel.open(destPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
     *             sourceChannel.transferTo(0, sourceChannel.size(), destChannel);
     *         }
     *     }
     * }
     */
}