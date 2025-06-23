package io.github.heisenberguwu.myrocketmq.common.config;

import com.google.common.collect.Maps;
import io.github.heisenberguwu.myrocketmq.common.ThreadFactoryImpl;
import io.github.heisenberguwu.myrocketmq.common.constant.LoggerName;
import io.github.heisenberguwu.myrocketmq.common.utils.ThreadUtils;
import io.netty.buffer.PooledByteBufAllocator;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.rocksdb.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static io.github.heisenberguwu.myrocketmq.common.constant.CommonConstants.SPACE;

public abstract class AbstractRocksDBStorage {

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

    // === 各种配置
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
    protected void initWriteOptions() {
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
    protected void initAbleWalWriteOptions() {
        this.ableWalWriteOptions = new WriteOptions();
        this.ableWalWriteOptions.setSync(false);
        this.ableWalWriteOptions.setDisableWAL(false);
        // https://github.com/facebook/rocksdb/wiki/Write-Stalls
        this.ableWalWriteOptions.setNoSlowdown(false);
    }

    protected void initReadOptions() {
        /**
         * 前缀扫描：启用了 prefix 模式，结合 Options.prefix_extractor，迭代会锁定在相同前缀范围内。
         * setPrefixSameAsStart(true)：一旦达到当前前缀尾部（如 prefix“user:”），后续 Next() 会直接停止，而不是跳到下一前缀键
         * TotalOrderSeek=false：关闭全局排序扫描，意味着 RocksDB 仅在当前前缀的 SST/memtable 中查找，无全表扫描
         * Tailing=false：不做实时监听，只做静态快照读取。
         */
        this.readOptions = new ReadOptions();
        this.readOptions.setPrefixSameAsStart(true); // 前缀匹配优化 iterator.seek("user:123"); 只会在user: 范围内查找
        this.readOptions.setTotalOrderSeek(false); // 不允许跨SST文件、跨memtable的全表扫描，只能在当前 prefix 范围或 current SST/memtable 范围内扫描
        this.readOptions.setTailing(false); // 是否监听日志
    }

    protected void initTotalOrderReadOptions() {
        /**
         * PrefixSameAsStart=false：不限制前缀。
         * TotalOrderSeek=true：启用全表扫描模式，无论是否配置 prefix_extractor，都会按照整个 key 空间进行 Seek 和遍历
         * Tailing=false：同样，仅一次性读取静态内容。
         */
        this.totalOrderReadOptions = new ReadOptions();
        this.totalOrderReadOptions.setPrefixSameAsStart(false);
        this.totalOrderReadOptions.setTotalOrderSeek(true); //
        this.totalOrderReadOptions.setTailing(false);
    }

    protected void initCompactRangeOptions() {
        this.compactRangeOptions = new CompactRangeOptions();
        /**
         * 底层（Bottommost Level）：RocksDB 的存储采用多层（level）结构，数据从 L0（最上层）逐渐流向 L1, L2, L3 等。底层数据较为难以压缩和合并，因为这些层的数据通常存放的是历史数据。
         * - 默认情况下，底层的压缩操作是懒加载的，不会主动进行（也不会频繁进行）。但通过 kForce 设置，可以强制进行压缩，避免仅仅把数据文件移动到底层，而不是进行重新合并。
         * - 适用场景：当你希望清理底层历史数据，减少空间碎片，提升读取效率时，可以开启这个选项。
         */
        this.compactRangeOptions.setBottommostLevelCompaction(CompactRangeOptions.BottommostLevelCompaction.kForce); //  设定底层进行Compact，KForce
        /**
         * 写操作停顿：当系统负载过重或者 compaction 操作进行时，写入请求可能会被延迟，直到 compaction 完成。这种情况下，Write Stall 会阻止写入，直到压缩完成。
         * - 设置为 true，RocksDB 在执行 compaction 操作时会暂时停止所有写操作。这个机制可防止写操作和 compaction 竞争资源，从而保证数据的完整性。
         * - 如果设置为 false，RocksDB 会尽量避免写停顿，即使写操作和 compaction 同时进行，也会通过调整压缩策略来避免停顿。
         */
        this.compactRangeOptions.setAllowWriteStall(true); // 允许在 compaction期间发生写操作停顿（stall）。
        /**
         * 排他性手动 compaction：当设置为 true 时，意味着当前的手动 compaction 会阻止任何其他手动或自动 compaction 任务的执行。这样可以确保 compaction 在某个特定时间内不会被打断。
         * - 设置为 false 时，允许并发的手动 compaction 和自动 compaction。多个线程可以同时执行 Compaction 操作，从而提高效率。
         * - 通常用于需要并发压缩的场景，避免手动压缩的单线程瓶颈。
         */
        this.compactRangeOptions.setExclusiveManualCompaction(false); // 不启用排他性手动 compaction。
        /**
         * 在默认情况下，RocksDB 执行 compaction 时会将数据文件合并并保持在原来的层级中。通过 setChangeLevel(true)，可以在 compaction 后把文件移至不同的层级。
         * 这个设置主要用于优化数据的分布和存储层次。当某些层级存储的数据量很大，且压缩完成后应该被移到更低层级时，启用该选项。
         */
        this.compactRangeOptions.setChangeLevel(true); // 允许在手动 compaction 中改变文件所处的层级。
        /**
         * 当 setTargetLevel(-1) 时，RocksDB 会自动选择一个层级，通常是从 L1 到 L6，适合当前数据的大小和频率。如果你希望文件被放置在某个特定的层级，可以手动指定目标层级（例如：setTargetLevel(2)）。
         * 设置为 -1 让 RocksDB 自行决定目标层级，有时会提高空间利用率。
         */
        this.compactRangeOptions.setTargetLevel(-1); // 自动选择最适等级放置 compaction 后文件。
        this.compactRangeOptions.setMaxSubcompactions(4); // 允许最多 4 个并发子 compaction 线程进行拆分压缩。
    }

    protected void initCompactionOptions() {
        this.compactionOptions = new CompactionOptions();
        this.compactionOptions.setCompression(compressionType); // 压缩算法 Lz4
        this.compactionOptions.setMaxSubcompactions(4); // 4个子任务
        this.compactionOptions.setOutputFileSizeLimit(4 * 1024 * 1024 * 1024L); // 限制每个SST文件大小不大于4GB
    }

    // === RocksDB 操作方法封装 CRUD

    /**
     * @param cfHandle
     * @param writeOptions
     */
    protected void put(ColumnFamilyHandle cfHandle, WriteOptions writeOptions,
                       final byte[] keyBytes, final int keyLen,
                       final byte[] valueBytes, final int valueLen) throws RocksDBException {
        /*
        列族（Column Family）是 RocksDB 中的一种逻辑数据结构，它提供了一种用于组织和存储数据的方式。在传统的关系型数据库中，
        我们通常会有多个表，而在 RocksDB 中，列族就类似于表的概念，但它的设计和实现更加优化了性能，特别是在高并发、高吞吐量的应用场景中。
        - 可以类比为 mysql 的一张表
         */
        if (!hold()) {
            throw new IllegalStateException("rocksDB:" + this + " is not ready");
        }
        try {
            this.db.put(cfHandle, writeOptions, keyBytes, 0, keyLen, valueBytes, 0, valueLen);
        } catch (RocksDBException e) {
            scheduleReloadRocksdb(e);
            LOGGER.error("put Failed. {}, {}", this.dbPath, getStatusError(e));
            throw e;
        } finally {
            release();
        }
    }

    /**
     * ByteBuffer 作为参数
     *
     * @param cfHandle
     * @param writeOptions
     * @param keyBB
     * @param valueBB
     * @throws RocksDBException
     */
    protected void put(ColumnFamilyHandle cfHandle, WriteOptions writeOptions,
                       final ByteBuffer keyBB, final ByteBuffer valueBB) throws RocksDBException {
        if (!hold()) {
            throw new IllegalStateException("rocksDB:" + this + " is not ready");
        }
        try {
            this.db.put(cfHandle, writeOptions, keyBB, valueBB);
        } catch (RocksDBException e) {
            scheduleReloadRocksdb(e);
            LOGGER.error("put Failed. {}, {}", this.dbPath, getStatusError(e));
            throw e;
        } finally {
            release();
        }
    }

    // 批量写入
    protected void batchPut(WriteOptions writeOptions, final WriteBatch batch) throws RocksDBException {
        try {
            this.db.write(writeOptions, batch);
        } catch (RocksDBException e) {
            scheduleReloadRocksdb(e);
            LOGGER.error("batchPut Failed. {}, {}", this.dbPath, getStatusError(e));
            throw e;
        } finally {
            batch.clear(); // 清理一下
        }
    }

    // byte[] get
    protected byte[] get(ColumnFamilyHandle cfHandle, ReadOptions readOptions, byte[] keyBytes) throws RocksDBException {
        if (!hold()) {
            throw new IllegalStateException("rocksDB:" + this + " is not ready");
        }
        try {
            return this.db.get(cfHandle, readOptions, keyBytes);
        } catch (RocksDBException e) {
            LOGGER.error("get Failed. {}, {}", this.dbPath, getStatusError(e));
            throw e;
        } finally {
            release();
        }
    }

    // byteBuffer get
    protected int get(ColumnFamilyHandle cfHandle, ReadOptions readOptions, final ByteBuffer keyBB,
                      final ByteBuffer valueBB) throws RocksDBException {
        if (!hold()) {
            throw new IllegalStateException("rocksDB:" + this + " is not ready");
        }
        try {
            return this.db.get(cfHandle, readOptions, keyBB, valueBB);
        } catch (RocksDBException e) {
            LOGGER.error("get Failed. {}, {}", this.dbPath, getStatusError(e));
            throw e;
        } finally {
            release();
        }
    }

    protected List<byte[]> multiGet(final ReadOptions readOptions,
                                    final List<ColumnFamilyHandle> columnFamilyHandleList,
                                    final List<byte[]> keys) throws RocksDBException {
        if (!hold()) {
            throw new IllegalStateException("rocksDB:" + this + " is not ready");
        }
        try {
            return this.db.multiGetAsList(readOptions, columnFamilyHandleList, keys);
        } catch (RocksDBException e) {
            LOGGER.error("multiGet Failed. {}, {}", this.dbPath, getStatusError(e));
            throw e;
        } finally {
            release();
        }
    }

    protected void delete(ColumnFamilyHandle cfHandle, WriteOptions writeOptions,
                          byte[] keyBytes) throws RocksDBException {
        if (!hold()) {
            throw new IllegalStateException("rocksDB:" + this + " is not ready");
        }
        try {
            this.db.delete(cfHandle, writeOptions, keyBytes);
        } catch (RocksDBException e) {
            LOGGER.error("delete Failed. {}, {}", this.dbPath, getStatusError(e));
            throw e;
        } finally {
            release();
        }
    }

    protected void delete(ColumnFamilyHandle cfHandle, WriteOptions writeOptions, ByteBuffer keyBB)
            throws RocksDBException {
        if (!hold()) {
            throw new IllegalStateException("rocksDB:" + this + " is not ready");
        }
        try {
            this.db.delete(cfHandle, writeOptions, keyBB);
        } catch (RocksDBException e) {
            LOGGER.error("delete Failed. {}, {}", this.dbPath, getStatusError(e));
            throw e;
        } finally {
            release();
        }
    }

    protected void rangeDelete(ColumnFamilyHandle cfHandle, WriteOptions writeOptions, final byte[] startKey,
                               final byte[] endKey) throws RocksDBException {
        if (!hold()) {
            throw new IllegalStateException("rocksDB:" + this + " is not ready");
        }
        try {
            this.db.deleteRange(cfHandle, writeOptions, startKey, endKey);
        } catch (RocksDBException e) {
            scheduleReloadRocksdb(e);
            LOGGER.error("rangeDelete Failed. {}, {}", this.dbPath, getStatusError(e));
            throw e;
        } finally {
            release();
        }
    }


    // === 各种数据库状态检查

    /**
     * ?? 手动压缩？
     * 🔁 为什么需要手动压缩？
     * 以下是常见的三种使用场景：
     * 1. ✅ 优化读取性能
     * 当一次性写入大量数据后，自动 compaction 未必赶得上，你可能在查询时遇到性能瓶颈。手动压缩可以及时将 L0/L1 等层的小文件整理到更高层，从而提升读取速度
     * 2. ⏳ 清理垃圾删除与过滤
     * 如果文件中存在 tombstone（删除标记），自动压缩未必会触发对应层级的迁移。手动 compaction 可以强制数据通过 compaction filter，将已删除数据清理掉
     * 3. 🔄 层级迁移或策略变更
     * 当你更改 compaction 策略、调整层级数量或文件大小参数时，旧有文件可能无法自发迁移。通过手动调用 CompactRange，可以将数据整理到正确的层级或按新的参数输出
     *
     * @param compactRangeOptions
     */
    protected void manualCompactionDefaultCfRange(CompactRangeOptions compactRangeOptions) {
        if (!hold()) {
            return;
        }
        long s1 = System.currentTimeMillis();
        boolean result = true;
        try {
            LOGGER.info("manualCompaction Start. {}", this.dbPath);
            this.db.compactRange(this.defaultCFHandle, null, null, compactRangeOptions); // 对整个 column 进行压缩。
        } catch (RocksDBException e) {
            result = false;
            scheduleReloadRocksdb(e);
            LOGGER.error("manualCompaction Failed. {}, {}", this.dbPath, getStatusError(e));
        } finally {
            release();
            LOGGER.info("manualCompaction End. {}, rt: {}(ms), result: {}", this.dbPath, System.currentTimeMillis() - s1, result);
        }
    }

    protected void manualCompaction(long minPhyOffset, final CompactRangeOptions compactRangeOptions) {
        this.manualCompactionThread.submit(new Runnable() {
            @Override
            public void run() {
                manualCompactionDefaultCfRange(compactRangeOptions);
            }
        });
    }

    /**
     * 打开 RocksDB 数据库实例，并根据是否只读的标志来决定是以只读还是读写模式打开数据库。
     *
     * @param cfDescriptors
     * @throws RocksDBException
     */
    protected void open(final List<ColumnFamilyDescriptor> cfDescriptors) throws RocksDBException {
        this.cfHandles.clear();
        if (this.readOnly) {
            // 读模式打开数据库
            this.db = RocksDB.openReadOnly(this.options, this.dbPath, cfDescriptors, cfHandles);
        } else {
            // rw
            this.db = RocksDB.open(this.options, this.dbPath, cfDescriptors, cfHandles);
        }
        // cf 描述符是否与句柄数量一致
        assert cfDescriptors.size() == cfHandles.size();

        if (this.db == null) {
            throw new RocksDBException("open rocksdb null");
        }
        try (Env env = this.db.getEnv()) {
            // 获取数据库环境对象，并且设置后台线程为8，优先级为低
            // 这些线程常用来作为后台的写入、压缩等任务。
            env.setBackgroundThreads(8, Priority.LOW);
        }
    }

    /**
     * 检查DB整体状态，验证数据库的当前状态，确保在进行操作前，数据库处于一个有效的状态。
     *
     * @return
     */
    public boolean hold() {
        if (!this.loaded || this.db == null || this.closed) {
            LOGGER.error("hold rocksdb Failed. {}", this.dbPath);
            return false;
        } else {
            return true;
        }
    }

    public void release() {
    }

    protected abstract boolean postLoad();

    // sync
    public synchronized boolean start() {
        if (this.loaded) {
            return true;
        }
        if (postLoad()) {
            this.loaded = true;
            LOGGER.info("RocksDB [{}] starts OK", this.dbPath);
            this.closed = false;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Close column family handles except the default column family
     * 在 RocksDB 中包含多个列族，关闭数据库之前需要彻底释放所有的句柄
     * 🔧 正确的关闭顺序
     * 关闭内存表（memtable）并生成 SST 文件：
     * flush(flushOptions);
     * 停止后台线程，防止并发访问问题：
     * <p>
     * db.cancelAllBackgroundWork(true);
     * db.pauseBackgroundWork();
     * 关闭所有列族句柄：
     * <p>
     * preShutdown()：用于关闭注册的自定义或非默认列族句柄。
     * <p>
     * this.defaultCFHandle.close()：专门关闭默认句柄。
     * <p>
     * 关闭各类 Options（ColumnFamilyOptions、WriteOptions、ReadOptions 等），防止资源泄漏。
     * <p>
     * 完成 DB 关闭，包括同步 WAL 和释放 db.closeE()。
     */
    protected abstract void preShutdown();

    public synchronized boolean shutdown() {
        try {
            if (!this.loaded) {
                LOGGER.info("RocksDBStorage is not loaded, shutdown OK. dbPath={}, readOnly={}", this.dbPath, this.readOnly);
                return true;
            }
            final FlushOptions flushOptions = new FlushOptions();
            flushOptions.setWaitForFlush(true); // 刷盘时候等会儿
            try {
                flush(flushOptions);
            } finally {
                flushOptions.close();
            }
            this.db.cancelAllBackgroundWork(true); // 取消所有后台功能
            this.db.pauseBackgroundWork(); // 冻结所有任务接受
            //The close order matters.
            //1. close column family handles
            preShutdown();
            // 在底层对所有列族结构的引用和内存指针释放
            // 在关闭数据库前，必须关闭所有列族句柄；否则，这些资源不会被正确回收，可能阻碍数据库完全关闭。
            this.defaultCFHandle.close();

            // 2. close column
            for (final ColumnFamilyOptions opt : this.cfOptions) {
                opt.close();
            }

            //3. close options
            if (this.writeOptions != null) {
                this.writeOptions.close();
            }
            if (this.ableWalWriteOptions != null) {
                this.ableWalWriteOptions.close();
            }
            if (this.readOptions != null) {
                this.readOptions.close();
            }
            if (this.totalOrderReadOptions != null) {
                this.totalOrderReadOptions.close();
            }

            //4. close db.
            if (db != null && !this.readOnly) {
                this.db.syncWal();
            }
            if (db != null) {
                this.db.closeE();
            }
            // Close DBOptions after RocksDB instance is closed.
            if (this.options != null) {
                this.options.close();
            }
            // help GC
            this.cfOptions.clear();
            this.db = null;
            this.readOptions = null;
            this.totalOrderReadOptions = null;
            this.writeOptions = null;
            this.ableWalWriteOptions = null;
            this.options = null;

            this.loaded = false;
            LOGGER.info("RocksDB shutdown OK. {}", this.dbPath);

        } catch (Exception e) {
            LOGGER.error("RocksDB shutdown failed. {}", this.dbPath, e);
            return false;
        }
        return true;
    }

    /**
     * 刷盘 封装
     *
     * @param flushOptions
     * @throws RocksDBException
     */
    public void flush(final FlushOptions flushOptions) throws RocksDBException {
        flush(flushOptions, this.cfHandles);
    }

    public void flush(final FlushOptions flushOptions, List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException {
        if (!this.loaded || this.readOnly || closed) {
            return;
        }

        try {
            if (db != null) {
                // For atomic-flush, we have to explicitly specify column family handles
                // See https://github.com/rust-rocksdb/rust-rocksdb/pull/793
                // and https://github.com/facebook/rocksdb/blob/8ad4c7efc48d301f5e85467105d7019a49984dc8/include/rocksdb/db.h#L1667
                this.db.flush(flushOptions, columnFamilyHandles);
            }
        } catch (RocksDBException e) {
            scheduleReloadRocksdb(e); // 如果失败了进行10s一次的尝试调度，这里面包含了 shutdown & start... 其中还包含了刷盘....
            LOGGER.error("flush Failed. {}, {}", this.dbPath, getStatusError(e));
            throw e;
        }
    }

    public void flushWAL() throws RocksDBException {
        this.db.flushWal(true);
    }

    public Statistics getStatistics() {
        return this.options.statistics();
    }

    /**
     * 计划执行 RocksDB
     *
     * @param rocksDBException
     */
    private void scheduleReloadRocksdb(RocksDBException rocksDBException) {
        // 这里不会出发 NPE，需要先判断 null
        if (rocksDBException == null || rocksDBException.getStatus() == null) {
            return;
        }
        Status status = rocksDBException.getStatus();
        Status.Code code = status.getCode();
        // Status.Code.Incomplete == code
        if (Status.Code.Aborted == code || Status.Code.Corruption == code || Status.Code.Undefined == code) {
            LOGGER.error("scheduleReloadRocksdb. {}, {}", this.dbPath, getStatusError(rocksDBException));
            scheduleReloadRocksdb0();
        }
    }

    private void scheduleReloadRocksdb0() {
        // tryAccquire 不阻塞
        if (!this.reloadPermit.tryAcquire()) {
            return;
        }
        this.closed = true;
        this.reloadScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                boolean result = true;
                try {
                    reloadRocksdb();
                } catch (Exception e) {
                    result = false;
                } finally {
                    reloadPermit.release();
                }
                // try to reload rocksdb next time
                if (!result) {
                    LOGGER.info("reload rocksdb Retry. {}", dbPath);
                    scheduleReloadRocksdb0();
                }
            }
        }, 10, TimeUnit.SECONDS);
    }

    private void reloadRocksdb() throws Exception {
        LOGGER.info("reload rocksdb Start. {}", this.dbPath);
        if (!shutdown() || !start()) {
            LOGGER.error("reload rocksdb Failed. {}", dbPath);
            throw new Exception("reload rocksdb Error");
        }
        LOGGER.info("reload rocksdb OK. {}", this.dbPath);
    }


    /**
     * 将异常转化为更清晰的字符串
     *
     * @param e
     * @return
     */
    private String getStatusError(RocksDBException e) {
        if (e == null || e.getStatus() == null) {
            return "null";
        }
        Status status = e.getStatus();
        StringBuilder sb = new StringBuilder(64);
        sb.append("code: ");
        if (status.getCode() != null) {
            sb.append(status.getCode().name());
        } else {
            sb.append("null");
        }
        sb.append(", ").append("subCode: ");
        if (status.getSubCode() != null) {
            sb.append(status.getSubCode().name());
        } else {
            sb.append("null");
        }
        sb.append(", ").append("state: ").append(status.getState());
        return sb.toString();
    }

    /**
     * 获取SST源数据
     *
     * @return
     */
    public List<LiveFileMetaData> getCompactionStatus() {
        if (!hold()) {
            return null;
        }
        try {
            return this.db.getLiveFilesMetaData();
        } finally {
            release();
        }
    }

    /**
     * 打印 RocksDB 的状态
     *
     * @param logger
     */
    public void statRocksdb(Logger logger) {
        try {
            // Log Memory Usage
            String blockCacheMemUsage = this.db.getProperty("rocksdb.block-cache-usage");
            String indexesAndFilterBlockMemUsage = this.db.getProperty("rocksdb.estimate-table-readers-mem");
            String memTableMemUsage = this.db.getProperty("rocksdb.cur-size-all-mem-tables");
            String blocksPinnedByIteratorMemUsage = this.db.getProperty("rocksdb.block-cache-pinned-usage");
            logger.info("RocksDB Memory Usage: BlockCache: {}, IndexesAndFilterBlock: {}, MemTable: {}, BlocksPinnedByIterator: {}",
                    blockCacheMemUsage, indexesAndFilterBlockMemUsage, memTableMemUsage, blocksPinnedByIteratorMemUsage);
            // Log file metadata by level，LiveFileMetaData -- SST 文件源数据
            List<LiveFileMetaData> liveFileMetaDataList = this.getCompactionStatus();
            if (liveFileMetaDataList == null || liveFileMetaDataList.isEmpty()) {
                return;
            }
            Map<Integer, StringBuilder> map = Maps.newHashMap();
            // 每个层级都有自己的 StringBuilder
            for (LiveFileMetaData metaData : liveFileMetaDataList) {
                StringBuilder sb = map.computeIfAbsent(metaData.level(), k -> new StringBuilder(256));// 较少每次都扩容【default : 16】
                sb.append(new String(metaData.columnFamilyName(), StandardCharsets.UTF_8)).append(SPACE).
                        append(metaData.fileName()).append(SPACE).
                        append("file-size: ").append(metaData.size()).append(SPACE).
                        append("number-of-entries: ").append(metaData.numEntries()).append(SPACE).
                        append("file-read-times: ").append(metaData.numReadsSampled()).append(SPACE).
                        append("deletions: ").append(metaData.numDeletions()).append(SPACE).
                        append("being-compacted: ").append(metaData.beingCompacted()).append("\n");
            }
            // 打印一下。
            map.forEach((key, value) -> logger.info("level: {}\n{}", key, value.toString()));
        } catch (Exception ignored) {
        }
    }
}

