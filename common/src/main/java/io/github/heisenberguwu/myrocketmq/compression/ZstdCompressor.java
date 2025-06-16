package io.github.heisenberguwu.myrocketmq.compression;

import io.github.heisenberguwu.myrocketmq.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

import java.io.IOException;

public class ZstdCompressor implements Compressor{

    private static final Logger log = LoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);
    @Override
    public byte[] compress(byte[] src, int level) throws IOException {
        return new byte[0];
    }

    @Override
    public byte[] decompress(byte[] src) throws IOException {
        return new byte[0];
    }
}
