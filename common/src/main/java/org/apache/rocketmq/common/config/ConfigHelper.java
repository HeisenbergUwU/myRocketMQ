package org.apache.rocketmq.common.config;

import org.apache.rocketmq.common.UtilAll;
import com.google.common.base.Strings;

import java.io.File;

import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.CompactionStyle;
import org.rocksdb.CompressionType;
import org.rocksdb.DBOptions;
import org.rocksdb.DataBlockIndexType;
import org.rocksdb.IndexType;
import org.rocksdb.InfoLogLevel;
import org.rocksdb.LRUCache;
import org.rocksdb.RateLimiter;
import org.rocksdb.SkipListMemTableConfig;
import org.rocksdb.Statistics;
import org.rocksdb.StatsLevel;
import org.rocksdb.StringAppendOperator;
import org.rocksdb.WALRecoveryMode;
import org.rocksdb.util.SizeUnit;

/**
 * 主要是为了 ColumnFamily 的一些定义操作
 */
public class ConfigHelper {
    public static ColumnFamilyOptions createConfigColumnFamilyOptions() {
        // 创建块表格式配置（RocksDB 默认使用的是 Block-based table 格式）
        BlockBasedTableConfig blockBasedTableConfig = new BlockBasedTableConfig()
                // 使用 RocksDB 的 FormatVersion 5，支持最新特性（如更好的压缩/索引）
                .setFormatVersion(5)
                // 索引方式为二分查找（经典模式）
                .setIndexType(IndexType.kBinarySearch)
                // 数据块内部使用二分查找索引
                .setDataBlockIndexType(DataBlockIndexType.kDataBlockBinarySearch)
                // 设置每个数据块大小为 32KB
                .setBlockSize(32 * SizeUnit.KB)
                // 设置布隆过滤器：16 哈希函数，非 block-based
                .setFilterPolicy(new BloomFilter(16, false))
                // 启用将索引和过滤器 block 缓存进 block cache
                .setCacheIndexAndFilterBlocks(true)
                // 设置索引和过滤器 block 在缓存中具有高优先级
                .setCacheIndexAndFilterBlocksWithHighPriority(true)
                // 不强制将 L0 层的过滤器和索引 pin 在缓存中
                .setPinL0FilterAndIndexBlocksInCache(false)
                // 固定顶层的索引和过滤器 block 到缓存，避免被逐出
                .setPinTopLevelIndexAndFilter(true)
                // 设置 block cache 为 4MB，分为 8 个 shard，不开启 strict capacity limit
                .setBlockCache(new LRUCache(4 * SizeUnit.MB, 8, false))
                // 启用整键过滤（针对完整 key 应用布隆过滤器）
                .setWholeKeyFiltering(true);

        // 设置 ColumnFamily 的整体参数
        ColumnFamilyOptions options = new ColumnFamilyOptions();

        return options
                // 最多允许 4 个 MemTable 在内存中
                .setMaxWriteBufferNumber(4)
                // 每个 write buffer（MemTable）大小为 64MB
                .setWriteBufferSize(64 * SizeUnit.MB)
                // 至少 1 个 MemTable 满了才触发合并
                .setMinWriteBufferNumberToMerge(1)
                // 应用上面配置好的 Block-based 表格式
                .setTableFormatConfig(blockBasedTableConfig)
                // 设置 MemTable 使用跳表（SkipList）作为数据结构
                .setMemTableConfig(new SkipListMemTableConfig())
                // 不使用压缩（适用于追求性能但存储占用大）
                .setCompressionType(CompressionType.NO_COMPRESSION)
                // 总共设置 7 层（Level 0 ~ Level 6）
                .setNumLevels(7)
                // 使用 Level Compaction 策略（层级结构、按层合并）
                .setCompactionStyle(CompactionStyle.LEVEL)
                // L0 层文件数达到 4 时触发 compaction
                .setLevel0FileNumCompactionTrigger(4)
                // L0 层文件数达到 8 时开始减速写入（写延迟）
                .setLevel0SlowdownWritesTrigger(8)
                // L0 层文件数达到 12 时完全停止写入（写阻塞）
                .setLevel0StopWritesTrigger(12)
                // 设置 Level 1 的目标文件大小为 64MB
                .setTargetFileSizeBase(64 * SizeUnit.MB)
                // 每层的目标文件大小为上一层的 2 倍
                .setTargetFileSizeMultiplier(2)
                // L1 层最大总文件大小为 256MB
                .setMaxBytesForLevelBase(256 * SizeUnit.MB)
                // 每层最大数据量为上一层的 2 倍
                .setMaxBytesForLevelMultiplier(2)
                // 设置 merge 操作为字符串追加（适用于 value 是可追加字符串的场景）
                .setMergeOperator(new StringAppendOperator())
                // 启用原地更新（若 value 空间足够，则在原地址上更新）
                .setInplaceUpdateSupport(true);
    }


    public static DBOptions createConfigDBOptions() {
        DBOptions options = new DBOptions();

        // 创建统计对象，并设置统计级别为：排除详细定时器（节省开销）
        Statistics statistics = new Statistics();
        statistics.setStatsLevel(StatsLevel.EXCEPT_DETAILED_TIMERS);

        return options
                // 设置数据库日志输出目录
                .setDbLogDir(getDBLogDir())

                // 设置日志的详细程度为 INFO 级别
                .setInfoLogLevel(InfoLogLevel.INFO_LEVEL)

                // 设置 WAL（预写日志）恢复策略：跳过任何损坏的记录
                .setWalRecoveryMode(WALRecoveryMode.SkipAnyCorruptedRecords)

                // 设置手动触发 WAL flush，而不是每次写入都立即同步
                .setManualWalFlush(true)

                // 设置整个 DB 实例写缓冲区的大小为 128MB
                .setDbWriteBufferSize(128 * SizeUnit.MB)

                // 每写入 1MB 数据就进行一次 fsync()，减少丢失数据的概率
                .setBytesPerSync(SizeUnit.MB)

                // WAL 每写入 1MB 数据也进行 fsync()
                .setWalBytesPerSync(SizeUnit.MB)

                // 如果数据库不存在则自动创建
                .setCreateIfMissing(true)

                // 如果缺失列族，则自动创建（通常在多列族场景下需要开启）
                .setCreateMissingColumnFamilies(true)

                // 设置最大打开文件数，-1 表示不限制（由 OS 管理）
                .setMaxOpenFiles(-1)

                // 每个 LOG 文件最大为 1GB，超过则切换新文件
                .setMaxLogFileSize(SizeUnit.GB)

                // 最多保留 5 个历史日志文件
                .setKeepLogFileNum(5)

                // 设置 MANIFEST 文件的最大大小，超过后会生成新的 MANIFEST
                .setMaxManifestFileSize(SizeUnit.GB)

                // 禁止多个线程并发写入 MemTable（线程安全更高，但性能下降）
                .setAllowConcurrentMemtableWrite(false)

                // 设置统计对象，收集运行时指标
                .setStatistics(statistics)

                // 每隔 600 秒输出一次统计信息
                .setStatsDumpPeriodSec(600)

                // 后台最大并发作业数（如 compaction、flush 等）
                .setMaxBackgroundJobs(32)

                // 每次 compaction 任务最多可分成 4 个子任务并发执行
                .setMaxSubcompactions(4)

                // 启用偏执性检查，遇到潜在数据错误时立刻失败
                .setParanoidChecks(true)

                // 达到写入速率限制时，延迟写入速率设置为 16MB/s
                .setDelayedWriteRate(16 * SizeUnit.MB)

                // 设置限速器，总写入速率控制为 100MB/s
                .setRateLimiter(new RateLimiter(100 * SizeUnit.MB))

                // 使用 Direct I/O 进行 flush 和 compaction，绕过 OS 缓存
                .setUseDirectIoForFlushAndCompaction(true)

                // 读取时也使用 Direct I/O，进一步绕过 OS 缓存
                .setUseDirectReads(true);
    }


    public static String getDBLogDir() {
        String[] rootPaths = new String[]{
                System.getProperty("user.home"),
                System.getProperty("java.io.tmpdir"),
                File.separator + "data"
        };
        for (String rootPath : rootPaths) {
            // Refer bazel test encyclopedia: https://bazel.build/reference/test-encyclopedia
            // Not all directories is available
            if (Strings.isNullOrEmpty(rootPath)) {
                continue;
            }
            File rootPathFile = new File(rootPath);
            if (!rootPathFile.exists() || !rootPathFile.canWrite()) {
                continue;
            }
            String logDirectory = rootPath + File.separator + "logs" + File.separator + "rocketmqlogs";
            // Create directories recursively.
            UtilAll.ensureDirOK(logDirectory);
            return logDirectory;
        }
        throw new RuntimeException("Failed to get log directory");
    }
}