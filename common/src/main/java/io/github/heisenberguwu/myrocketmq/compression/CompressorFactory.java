package io.github.heisenberguwu.myrocketmq.compression;

import java.util.EnumMap;

public class CompressorFactory {
    private static final EnumMap<CompressionType, Compressor> COMPRESSORS;

    static {
        // EnumMap 内部使用数组保存，只能使用枚举对象作为Key。
        // 查询时候是 o(1)
        COMPRESSORS = new EnumMap<>(CompressionType.class);
        COMPRESSORS.put(CompressionType.LZ4, new Lz4Compressor());
        COMPRESSORS.put(CompressionType.ZSTD, new ZstdCompressor());
        COMPRESSORS.put(CompressionType.ZLIB, new ZlibCompressor());
    }

    public static Compressor getCompressor(CompressionType type) {
        return COMPRESSORS.get(type);
    }
}