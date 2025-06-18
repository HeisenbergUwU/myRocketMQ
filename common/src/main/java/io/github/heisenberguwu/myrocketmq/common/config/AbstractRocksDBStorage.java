package io.github.heisenberguwu.myrocketmq.common.config;

import io.github.heisenberguwu.myrocketmq.common.constant.LoggerName;
import io.netty.buffer.PooledByteBufAllocator;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.rocksdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class AbstractRocksDBStorage {

    /**
     * RocksDB k-v 存储的抽象类
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(LoggerName.ROCKSDB_LOGGER_NAME);

    /*
      Direct Jemalloc allocator
      jemalloc（Jason Evans malloc）是一个高性能、低碎片的用户态内存分配器
      Direct Jemalloc allocator 可能指的是系统直接使用 jemalloc（而非系统默认分配器）作为其内存分配器的一种配置或构建方式。
     */
    /*
      PooledByteBufAllocator 是 Netty 的一个高效 ByteBuf 分配器实现，用于缓冲区对象（ByteBuf）的池化和复用，特别适用于网络 I/O 中频繁创建/释放缓冲区的场景。其核心是减少 GC 压力、提升性能。
      - 核心竞争力：池化缓冲区。
     */
    public static final PooledByteBufAllocator POOLED_ALLOCATOR = new PooledByteBufAllocator(true); // 尽量使用直接内存。

    public static final byte CTRL_0 = '\u0000';
    public static final byte CTRL_1 = '\u0001';
    public static final byte CTRL_2 = '\u0002';

    private static final String SPACE = " | ";
    protected final String dbPath;
    protected boolean readOnly;

    protected RocksDB db; // 数据库操作实例
    protected DBOptions options; // 数据库配置项
    // 写配置
    protected WriteOptions writeOptions;
    protected WriteOptions ableWalWriteOptions;
    // 读配置
    protected ReadOptions readOptions;
    protected ReadOptions totalOrderReadOptions;
    // 压缩配置
    protected CompactionOptions compactionOptions;
    protected CompactRangeOptions compactRangeOptions;
    // 列族句柄，用来 引用和管理底层 C++ 中对应列族的指针
    protected ColumnFamilyHandle defaultCFHandle;
    protected final List<ColumnFamilyOptions> cfOptions = new ArrayList<>();
    protected final List<ColumnFamilyHandle> cfHandles = new ArrayList<>();

    protected volatile boolean loaded;
    protected CompressionType compressionType = CompressionType.LZ4_COMPRESSION;
    private volatile boolean closed;

    // 信号灯锁
    private final Semaphore reloadPermit = new Semaphore(1);
    ThreadUtils.
}

