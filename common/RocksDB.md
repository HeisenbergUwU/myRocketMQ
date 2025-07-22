![](https://cdn.nlark.com/yuque/0/2025/png/930158/1750161031340-4235161a-2100-467c-a7fa-f1f8f2266ae8.png)

RocksDB 是一个 **高性能嵌入式键值数据库（key-value store）**，由**<font style="color:#DF2A3F;"> Facebook（现 Meta）</font>**于 2012 年 fork 自 Google 的 LevelDB，并被优化以适应多核 CPU 和 SSD 存储等高吞吐需求

+ **LSM 树结构**  
使用 Log-Structured Merge-tree（LSM）来组织数据，写入性能优良，特别适合写密集型场景[engineering.fb.com+10en.wikipedia.org+10simplyblock.io+10](https://en.wikipedia.org/wiki/RocksDB?utm_source=chatgpt.com)。
+ **优化 SSD 与多核处理**  
充分利用闪存与多核 CPU，提高随机读写性能，降低写放大，提高 IOPS 效能[worldvectorlogo.com+4zh.wikipedia.org+4icon-icons.com+4](https://zh.wikipedia.org/wiki/MyRocks?utm_source=chatgpt.com)[engineering.fb.com](https://engineering.fb.com/2013/11/21/core-infra/under-the-hood-building-and-open-sourcing-rocksdb/?utm_source=chatgpt.com)。
+ **丰富的特性支持**  
提供事务、快照、列族（column families）、布隆过滤器（Bloom filter）、TTL 支持、合并操作（merge operators）、自定义压缩与压实策略等功能[github.com+6en.wikipedia.org+6en.wikipedia.org+6](https://en.wikipedia.org/wiki/RocksDB?utm_source=chatgpt.com)。
+ **多语言绑定**  
虽然底层为 C++ 实现，但提供官方的 C、Java（通过 RocksJava）接口，并有 Python、Go、Rust 等社区支持。
+ **嵌入式库**  
RocksDB **不是独立服务器**，而是作为嵌入式库被嵌入应用，程序直接操作其 API，而不是通过网络协议访问

| 特性 | 描述 |
| --- | --- |
| 数据模型 | 简单的 key–value 存储 |
| 架构 | 使用 LSM 树，写性能优秀 |
| 最佳用途 | 写密集、低延迟场景 |
| 优势 | 强大的可配置能力、丰富功能 |
| 部署形式 | 作为库嵌入应用，无网络接口 |




# <font style="background-color:#FBDE28;">我又看了一遍</font>！！ -- 2025-07-22
![](https://cdn.nlark.com/yuque/0/2025/png/930158/1753198821299-8dcee971-08fb-48e6-bcfe-302024476768.png)

1. 一旦发生了有状态操作，先操作 WAL 再说内存，这种设计主要是为了灾后恢复
2. **有状态操作**的时候 memtable 装不下了就找个 immemtable，再不行就一层一层落盘了。
    1. L0 不压缩
    2. 之后的会进行压缩，在压缩的过程中会处理在 tombStone、merge 的一些操作，**<font style="color:#DF2A3F;">保证同层情况下数据没有重复。</font>**
    3. 写入的时候**最好按照一个 **`** lexicographical order **`** 的形式写入**，user：1234 这种。RocksDB是按照字节顺序排序的，逐个字节对比大小。 -- 这么干能**<font style="color:#DF2A3F;">减少读放大、写放大</font>**
3. **读取的时候**先看看 内存、内存没有就开始看磁盘了，L0会有重复数据【不进行压缩】、后面会 重排序 压缩。
    1. 每一个SST文件都有自己的`FileMetaData.smallest` 和 `FileMetaData.largest`分别表示SST文件存储的大小。
    2. 如果在一层 Miss 了，那么就去下一层通过一个大概的范围找几个文件去查查。
4. WAL 是一个循环日志逻辑，写满了就切到下一个，所有被写入SST的Key，WAL就可以清理了，防止无线增长。

> 这个数据库非常适合SSD，因为是块级别的写入比较适合目前的存储设备。
>

# <font style="color:rgb(25, 27, 31);">使用上的原理</font>
## 写入
1. **写入**会先写到MemTable的内存缓存中，同时也会在磁盘上记录 WAL（Write Ahead Log）。
    1. MemTable 是跳表组成，插入读取时间复杂度都是O(logn)
2. 后续的写入都会转入到新的MemTable和WAL 中。
3. 系统会将只读的MemTable 和 WAL 内容，落盘到Sorted String Table （SSTable）。
4. 已经落盘的WAL和 MemTable就可以丢弃了。

![](https://cdn.nlark.com/yuque/0/2025/png/930158/1753198821299-8dcee971-08fb-48e6-bcfe-302024476768.png)

| 目的 | 解释 |
| --- | --- |
| **减少写放大** | 将新写入的数据先写入上层，延迟并批量合并写入下层，提高写入效率。 |
| **提高压缩效率** | 下层数据量大、生命周期长，更适合压缩，减少磁盘占用。 |
| **提升读性能** | 数据被有序合并存放在不同层中，查询时范围缩小，Bloom Filter + 稀疏索引提高效率。 |
| **控制文件数量** | 避免所有 SST 文件都堆在一个目录中导致查找复杂度升高。 |


## 查询 & 合并 & 删除
| 层级 | 特点 |
| --- | --- |
| **MemTable** | 内存中的跳表结构，写入数据先到这里。 |
| **Level 0** | 没有排序保证，可能有键重叠，写入频繁。 |
| **Level 1+** | 每层内的文件之间 **没有键范围重叠**，文件数量和大小有增长限制，读取更快。 |
| **Level N** | 随着层级增加，数据量也增加，压缩频率降低。通常 Level 6 是最后一层。 |


### 举个例子
+ 你连续插入 `user_1`, `user_2`, ..., `user_1000000`
+ 这些数据首先进入 MemTable。
+ 落盘形成几个 Level 0 的 SST 文件。
+ 接下来会自动 compaction，把它们合并成有序的 Level 1 文件。
+ 如果继续写入，Level 1 会合并到 Level 2，逐层下推。

### 会不会有重复数据？
答案是肯定得，但是读取的时候不会读取到重复的数据，因为是从上往下读的，同层之间是不会有重复的数据的。

+ 查询会根据L0 -> L1 -> L2 .. 上层数据是较为新的



**删除**的时候则将Key添加到TombStone中，在下一次Compaction的时候进行删除

+ 节省磁盘空间
+ 降低读取开销

### 目的是什么？
RocksDB 多层是 **LSM-Tree 的优化结果**，目标是：

+ 降低写入放大（Write Amplification）
+ 提高压缩率（Space Amplification）
+ 保持读取效率（Read Amplification）
+ 实现更好的可扩展性与稳定性

### 读取的时候是否还要解压？
在使用RocksDB的时候可以配置SST文件的压缩形式：

| 压缩类型 | 优点 | 说明 |
| --- | --- | --- |
| `Snappy` | 快速，压缩率一般 | 默认，适合低延迟应用 |
| `Zlib`<br/> / `Zstd` | 压缩率高 | 适合磁盘受限场景 |
| `LZ4` | 平衡性能和压缩率 | 高吞吐写场景常用 |
| `NoCompression` | 不压缩 | 适合 I/O 便宜或极低延迟要求的场景 |


读取数据的时候：

1. 从MemTable查询内存中的数据
2. 如果没查到，搜索每一层的SST文件
3. 定位到了Block之后
4. IF Compressed -> 调用对应的解压方案
    1. ⚠️  每次解压缩会解压多个Blocks。
5. 在不进行解压缩的情况下就可以查询到Key所在的区域。
    1. 🚀每个 block 是压缩的，但 **Block Index** 和 **Footer** 是不压缩的。

```nginx
+--------------------+
|     Block 1        |
|     Block 2        |
|     Block 3        |
+--------------------+
|   Block Index      | ← key-range 定位某个 Block（不需解压）
+--------------------+
|    Footer          | ← 指向索引和元信息

```

# 仔细阅读一下
## 读写放大问题
内核**写入磁盘**的数据与用户逻辑写入数据量的比值，我写入10MB数据，但是磁盘实际写入了30MB，则写放大为3.

+ LSM树写入会重复多次
+ MemTable Flush 也会重复写入

> 磁盘容易满，SSD容易报废
>



读放大，用户一次读取1KB数据，但是系统读取了10KB数据，则放大为10.

+ LSM树结合会跨多个SST文件层级查找键值
+ Bloom Filter 、 Block Cache 缓存命中也会影响读取次数

> IO CPU消耗都增大
>

## LSM （Log-Structured-Merge-Tree）
不像是B+树、红黑树这种严格的数据结构，是一种存储结构。RocksDB存储都是采用LSM树。

![](https://cdn.nlark.com/yuque/0/2025/png/930158/1750178079963-5a0081bc-e51d-4adc-8682-56bcd742cce9.png)

### MemTable
MemTable是内存中的数据结构，用于保存最近更新的数据，按照Key有序的组织这些数据，LSM树对于具体如何组织并没有明确定义，HBase中使用跳表来保证内存中的Key有序。



因为数据暂时保存咋及内存中，内存并不是可靠的存储，通常使用WAL（Write Ahead Logging，预写式日志）的方式来保证数据的可靠性。

#### Skip List - 跳表
[Understanding RocksDB Internals: LSM-Trees, MemTables, SSTables, and Compaction](https://medium.com/%40ghufrankhan_921/understanding-rocksdb-internals-lsm-trees-memtables-sstables-and-compaction-5cba4138de71)

![](https://cdn.nlark.com/yuque/0/2025/png/930158/1750180920794-3b050ade-acda-44e0-9070-1b65338c9d3a.png)

+ 默认 MemTable 使用 **跳表**（SkipList），实现为 `SkipListRep : MemTableRep` 类。
+ 跳表适合并发插入（通过 `allow_concurrent_memtable_write` 支持）、范围查询和随机读取
+ 跳表空间开销适中，也能稳定提供 O(log n) 插入查找性能

| 数据结构 | 平均查找时间 | 插入/删除 | 实现复杂度 |
| --- | --- | --- | --- |
| 单链表 | O(n) | O(n) | 简单 |
| 数组 + 二分 | O(log n) | O(n) | 简单 |
| 平衡树（红黑/AVL） | O(log n) | O(log n)（含平衡） | 较复杂 |
| **跳表** | **O(log n)** | **O(log n)** | **较简单，只要链表** |




**插入**

当我们不断地插入数据，有可能出现

![](https://cdn.nlark.com/yuque/0/2025/png/930158/1750181083382-91310086-0fe8-4f4f-a67c-d21e48e8ae5a.png)

这样的情况，那么我们的跳表就退化了。



为了解决这个问题，我们需要重新建立索引；但是不可以每次插入数据都重建索引，因为每次创建一遍索引都是 O(n)的操作，我们不能付出每次插入都是O（N）的代价。

+ 索引层数： MaxLevel = ⌊log₁ₚ N⌋   p 为向上提升概率（1/2、1/4）** 在概率上，第 i 层索引节点大约是第 i–1 层节点数量的一半（假设 p=1/2），底层为 N，往上依次大约 N/2、N/4、…，故平均层数约为 **`**O(log N)**`



### Immutable MemTable
档MemTable到达一定大小之后，会转化为Immutable MemTable，这是一种 SSTable的一种中间状态，写操作由心的MemTable处理，在转存过程中不阻塞数据更新操作。

### SSTable （Sorted String Table）
有序键值对集合，是LSM树组在磁盘中的数据结构，为了加快SSTable的读取速度，可以通过**建立Key的索引** && **布隆过滤器 **来加快Key的查询。

![](https://cdn.nlark.com/yuque/0/2025/png/930158/1750179040718-8e01629c-2a01-44c5-9f6e-4519a6337866.png)

#### 布隆过滤器
Bloom Filter 会使用一个较大的Bit数组来保存数据，用于检索元素是否存在于大集合中

+ 存在一定错误识别率

![](https://cdn.nlark.com/yuque/0/2025/png/930158/1750179540332-97470142-e758-40d6-aeec-3c0c26f22c0a.png)

1. 二进制数组 + 多个哈希函数 组成
2. 添加元素：通过多个哈希函数计算得到多个位数组位置，讲这些位置设置为1。
3. 查询元素：进行哈希函数进行计算，判断数组中的位置是否都是1，**如果都是1则可能存在**，**如果有一个不为1则一定不存在。**

**<font style="color:#DF2A3F;">示例说明</font>**

假设位数组长度 _m = 10_，使用 _k = 3_ 个哈希函数：

+ **插入 "red"**：
    - `h1(red) % 10 = 1`
    - `h2(red) % 10 = 3`
    - `h3(red) % 10 = 5`  
→ 将位置 1、3、5 设为 1。
+ **插入 "blue"**：
    - `h1(blue) % 10 = 4`
    - `h2(blue) % 10 = 5`
    - `h3(blue) % 10 = 9`  
→ 将位置 4、5、9 设为 1（注意位置 5 已被“red”置过）。
+ **查询 "green"**（未插入）：
    - `h1(green) = 3`、`h2(green) = 4`、`h3(green) = 5`。
    - 这三个位置都为 1（部分由 red 和 blue 设置），因此布隆过滤器**可能**判断“存在”（误报）[juejin.cn+9systemdesign.one+9systemdesign.one+9](https://systemdesign.one/bloom-filters-explained/?utm_source=chatgpt.com)[interviewcake.com+1cn.pingcap.com+1](https://www.interviewcake.com/concept/java/bloom-filter?utm_source=chatgpt.com)。
+ **查询 "black"**（未插入）：
    - `h1(black) = 0`（对应 bit 是 0）。
    - 可立即判断“black 不在集合中”（正确不在）[interviewcake.com](https://www.interviewcake.com/concept/java/bloom-filter?utm_source=chatgpt.com)[leapcell.medium.com+3zh.wikipedia.org+3dongzl.github.io+3](https://zh.wikipedia.org/wiki/%E5%B8%83%E9%9A%86%E8%BF%87%E6%BB%A4%E5%99%A8?utm_source=chatgpt.com)。





## RocksDB 中如何实现MemTable？
<font style="color:rgb(25, 27, 31);">LSM树(Log-Structured-Merge-Tree)正如它的名字一样，LSM树会将所有的数据插入、修改、删除等操作记录(注意是操作记录)保存在内存之中，当此类操作达到一定的数据量后，再批量地顺序写入到磁盘当中。这与B+树不同，B+树数据的更新会直接在原数据所在处修改对应的值，但是LSM数的数据更新是日志式的，当一条数据更新是直接append一条更新记录完成的。这样设计的目的就是为了顺序写，不断地将Immutable MemTable flush到持久化存储即可，而不用去修改之前的SSTable中的key，保证了顺序写。</font>

# 
