package org.apache.rocketmq.common.utils;

import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.UtilAll;
import org.apache.commons.collections.CollectionUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 将一批 “检查点” 数据写入文件，并在读取时进行完整性校验。适用于定期保存进度、恢复状态或容错机制中。
 * Entry Checkpoint file util
 * Format:
 * <li>First line:  Entries size
 * <li>Second line: Entries crc32
 * <li>Next: Entry data per line
 * <p>
 * Example:
 * <li>2 (size)
 * <li>773307083 (crc32)
 * <li>7-7000 (entry data)
 * <li>8-8000 (entry data)
 * <p>
 * 第一行：条目数量（entries size）。
 * <p>
 * 第二行：Crc32 校验值（整个内容的完整性）。
 * <p>
 * 后续每一行：通过 CheckpointSerializer<T> 将每个 entry 序列化为文本行。
 */
public class CheckpointFile<T> {
    // 如果不用CRC32检测则为0
    private static final int NOT_CHECK_CRC_MAGIC_CODE = 0;
    private final String filePath;
    private final CheckpointSerializer<T> serializer;

    public interface CheckpointSerializer<T> {
        /**
         * Serialize entry to line
         */
        String toLine(final T entry);


        /**
         * DeSerialize line to entry
         */
        T fromLine(final String Line);
    }

    public CheckpointFile(final String filePath, final CheckpointSerializer<T> serializer) {
        this.filePath = filePath;
        this.serializer = serializer;
    }

    public String getBackFilePath() {
        return this.filePath + ".bak";
    }

    // write entries to file.
    public void write(final List<T> entries) throws IOException {
        if (entries.isEmpty()) {
            return;
        }

        synchronized (this) {
            StringBuilder entryContent = new StringBuilder();
            for (T entry : entries) {
                final String line = this.serializer.toLine(entry);
                if (line != null && !line.isEmpty()) {
                    entryContent.append(line);
                    entryContent.append(System.lineSeparator());
                }
            }
            int crc32 = UtilAll.crc32(entryContent.toString().getBytes(StandardCharsets.UTF_8));

            String content = entries.size() + System.lineSeparator() +
                    crc32 + System.lineSeparator() + entryContent;

            MixAll.string2File(content, this.filePath);
        }
    }

    private List<T> read(String filePath) throws IOException {
        final ArrayList<T> result = new ArrayList<>();
        synchronized (this) {
            final File file = new File(filePath);
            if (!file.exists()) {
                return result;
            }
            try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
                // 阅读的长度
                int expectedLines = Integer.parseInt(reader.readLine());
                // crc 参数
                int expectedCrc32 = Integer.parseInt(reader.readLine());

                // Read entries
                StringBuilder sb = new StringBuilder();
                String line = reader.readLine();
                while (line != null) {
                    sb.append(line).append(System.lineSeparator());
                    final T entry = this.serializer.fromLine(line);
                    if (entry != null) {
                        result.add(entry);
                    }
                    line = reader.readLine();
                }
                // crc32 验证一下
                int truthCrc32 = UtilAll.crc32(sb.toString().getBytes(StandardCharsets.UTF_8));

                if (result.size() != expectedLines) {
                    final String err = String.format(
                            "Expect %d entries, only found %d entries", expectedLines, result.size());
                    throw new IOException(err);
                }

                if (NOT_CHECK_CRC_MAGIC_CODE != expectedCrc32 && truthCrc32 != expectedCrc32) {
                    final String err = String.format(
                            "Entries crc32 not match, file=%s, truth=%s", expectedCrc32, truthCrc32);
                    throw new IOException(err);
                }
                return result;
            }
        }
    }

    /**
     * Read entries from file
     */
    public List<T> read() throws IOException {
        try {
            List<T> result = this.read(this.filePath);
            if (CollectionUtils.isEmpty(result)) {
                result = this.read(this.getBackFilePath());
            }
            return result;
        } catch (IOException e) {
            return this.read(this.getBackFilePath());
        }
    }

}