package io.github.heisenberguwu.myrocketmq.common.attribute;

public enum CQType {
    /**
     * | 策略        | 描述                 | 适用场景             |
     * | --------- | ------------------ | ---------------- |
     * | SimpleCQ  | 顺序处理简单变更           | 场景简单、请求量不高       |
     * | BatchCQ   | 多条变更合一批处理          | 网络/磁盘资源宝贵、吞吐需求高  |
     * | RocksDBCQ | 借助本地 DB 实现持久化与恢复机制 | 数据安全性高、需恢复或补偿的场景 |
     */
    SimpleCQ,
    BatchCQ,
    RocksDBCQ
}
