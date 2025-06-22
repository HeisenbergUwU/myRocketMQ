package io.github.heisenberguwu.myrocketmq.common.config;

import io.github.heisenberguwu.myrocketmq.common.ThreadFactoryImpl;
import io.github.heisenberguwu.myrocketmq.common.constant.LoggerName;
import io.github.heisenberguwu.myrocketmq.common.utils.ThreadUtils;
import io.netty.buffer.PooledByteBufAllocator;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.rocksdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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

    private final ScheduledExecutorService reloadScheduler = ThreadUtils.newScheduledThreadPool(1, new ThreadFactoryImpl("RocksDBStorageReloadService_"));

    private final ThreadPoolExecutor manualCompactionThread = (ThreadPoolExecutor) ThreadUtils.newThreadPoolExecutor(
            1, 1, 1000 * 60, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1), new ThreadFactoryImpl("RocksDBManualCompactionService_")

    );

    static {
        // 在类加载时自动执行一次 JNI 库加载操作，为 RocksDB 提供底层 C++ 支持
        RocksDB.loadLibrary(); // 加载动态链接库，如果不加载那么调用所有的API都报错
    }

    public AbstractRocksDBStorage(String dbPath) {
        this.dbPath = dbPath;
    }

    protected void initOptions() {
        initWriteOptions();
        initAbleWalWriteOptions();
        initReadOptions();
        initTotalOrderReadOptions();
        initCompactRangeOptions();
        initCompactionOptions();
    }

    /**
     * WAL 关闭
     */
    protected void initWriteOptions()
    {
        this.writeOptions = new WriteOptions();
        this.writeOptions.setSync(false);// 异步写入
        this.writeOptions.setDisableWAL(true); // 不禁用 预写日志
        this.writeOptions.setNoSlowdown(false); // 阻塞等待 RocksDB 释放资源再继续写入（默认行为）。
    }

    /**
     * WAL 开启
     * | 项目        | WAL 关闭 (`setDisableWAL(true)`) | WAL 开启 (`setDisableWAL(false)`) |
     * | --------- | ------------------------------ | ------------------------------- |
     * | **写入性能**  | 更快（少一次写磁盘）                     | 略慢（需写 WAL）                      |
     * | **数据安全性** | 崩溃可能丢失数据（即使写入成功）               | 崩溃后可通过 WAL 恢复                   |
     * | **使用场景**  | 临时数据、缓存、可丢数据的业务                | 需要强一致性、不能丢数据的业务                 |
     */
    protected void initAbleWalWriteOptions()
    {
        this.ableWalWriteOptions = new WriteOptions();
        this.ableWalWriteOptions.setSync(false);
        this.ableWalWriteOptions.setDisableWAL(false);
        // https://github.com/facebook/rocksdb/wiki/Write-Stalls
        this.ableWalWriteOptions.setNoSlowdown(false);
    }

    protected void initReadOptions()
    {
        this.readOptions = new ReadOptions();
        this.readOptions.setPrefixSameAsStart(true); // 前缀匹配优化 iterator.seek("user:123"); 只会在user: 范围内查找
        this.readOptions.setTotalOrderSeek(false); // 不允许跨SST文件、跨memtable的全表扫描，只能在当前 prefix 范围或 current SST/memtable 范围内扫描
        /**
         * Tailing Iterator 用于实时消费新增数据，比如：
         * - 从 RocksDB 的最新位置不断读取新增写入的 key
         * - 类似“流式读取”或“变更订阅”
         */
        this.readOptions.setTailing(false); // 是否监听日志
    }
}