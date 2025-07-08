# 🚀 rocketmq-common

`rocketmq-common` 是 Apache RocketMQ 的核心公共模块，提供消息体结构定义、协议、序列化、配置管理、工具类等基础设施，是客户端 (`rocketmq-client`) 和服务器 (`rocketmq-broker` 等) 的重要依赖。

当前最新稳定版本为 **5.3.3**（发布日期：2025‑05‑12）([mvnrepository.com][1])。

---

## 📦 Maven 依赖

```xml
<dependency>
  <groupId>org.apache.rocketmq</groupId>
  <artifactId>rocketmq-common</artifactId>
  <version>5.3.3</version>
</dependency>
```

Gradle：

```groovy
implementation 'org.apache.rocketmq:rocketmq-common:5.3.3'
```

---

## 📘 模块结构 ✨

`rocketmq-common` 包含以下关键包：

* `constant`：常量定义（如系统主题、配置 key 等）。
* `annotation`：注解定义。
* `consumer`：消费者相关的公共类型（如消费模型枚举）。
* `protocol`：用于客户端与 Broker/Ns 通信的协议体，如请求/响应消息对象。
* `tool`：工具类集合，包含 JSON 序列化、CRC、URL 格式化、时间处理、环境检测等实用工具。
* `config`：配置读取与校验组件。
* `filter`：消息过滤表达式与条件构建器。
* `message`：消息类（如 `Message`、`MessageExt` 等）的元数据定义。

---

## 🔧 主要功能

1. **消息和协议定义**
   包含通用消息元信息与业务字段，例如 `Message`, `MessageExt`, `MessageQueue` 等，支持灵活序列化与属性过滤。

2. **序列化与反序列化**
   内置 JSON/JSON Fast 序列化工具，如 `MixAll`、`JSONUtils`、`FastJson` 等，支持高性能使用。

3. **工具方法**
   提供一整套常用工具类，如：

    * CRC64、CRC32 校验
    * URL 格式化工具支持属性编码
    * 时间和文件系统相关工具
    * 属性加载与环境变量读取

4. **消息过滤器支持**
   实现 SQL92/Tag 过滤表达式解析与校验，支持消费者端预过滤。

5. **客户端—Broker 通信协议**
   定义所有 Client/Broker 通信的请求/响应类，确保 `rocketmq-client` 与 `broker` 模块的交互结构一致。

6. **配置校验和公共常量**
   包括 `ClientConfig`、`ConsumerConfig` 等配置基础校验逻辑和 RocketMQ 常量（主题名、默认端口等）。

---

## 🚀 示例代码

### 检查 broker 地址配置

```java
String nsAddr = namesrvAddr; // e.g. "127.0.0.1:9876"
MixAll.checkTcpAddressFormat(nsAddr);
List<String> addrs = BoltTraceUtil.parseIPs(nsAddr);
```

### 消息过滤示例

```java
FilterExpression exp = new FilterExpression("tagA || tagB", FilterExpressionType.SQL92);
boolean valid = exp.validate();
String formatted = exp.getExpression();
```

### 引用通用消息类型

```java
Message msg = new Message("TopicTest", "TagA", ("Hello RocketMQ").getBytes(StandardCharsets.UTF_8));
msg.putUserProperty("a", "1");
```

---

## 🌐 与其它模块依赖关系

* **rocketmq-client**：严重依赖 `common` 中定义的消息和协议结构。
* **rocketmq-broker/rmq-namesrv**：用于协议解析、命令处理及序列化操作。
* **rocketmq-remoting**：依赖 `common.protocol` 中命令类型定义。
* **rocketmq-tools**：复用了 `MixAll` 和配置工具。

---

## 🧪 兼容性版本与持续更新

* 与 Java 客户端保持同步版本迭代；5.x 推出后，功能更全 ([mvnrepository.com][1], [javadoc.io][2], [rocketmq.apache.org][3], [central.sonatype.com][4])。
* 4.x 和 5.x 版本共存时，建议使用与客户端一致版本。
* 定期在 Maven 中更新到最新的 patch 版（如 `5.3.3`）。

---

## ❤️ 贡献指南

欢迎提交 PR 或 issue：

1. Fork 本仓库，基于最新 `develop` 分支开发；
2. 新增功能或修复 Issue 后，编写测试用例；
3. 提交 PR，并在标题中说明模块（`common:`）；
4. 按照 RocketMQ 社区规范格式。
   检查 JavaDoc 注释与代码风格。

---

## 📚 参考链接

* Maven 仓库版本说明&#x20;
* JavaDoc（3.6.2.Final 供参考）([javadoc.io][2])

---

## 🏁 总结

`rocketmq-common` 是 RocketMQ 核心模块中定义消息结构、工具方法与协议体的基础依赖库。它支持客户端与 broker 的通信规范、配置体系、过滤条件以及常用工具，是 `rocketmq-client`, `broker` 模块不可或缺的基础。

---

[1]: https://mvnrepository.com/artifact/org.apache.rocketmq/rocketmq-common?utm_source=chatgpt.com "org.apache.rocketmq » rocketmq-common - Maven Repository"
[2]: https://javadoc.io/doc/com.alibaba.rocketmq/rocketmq-common/latest/index.html?utm_source=chatgpt.com "rocketmq-common 3.6.2.Final javadoc (com.alibaba.rocketmq)"
[3]: https://rocketmq.apache.org/docs/?utm_source=chatgpt.com "Why choose RocketMQ"
[4]: https://central.sonatype.com/artifact/com.alibaba.rocketmq/rocketmq-all/3.5.2?utm_source=chatgpt.com "com.alibaba.rocketmq:rocketmq-all:3.5.2 - Maven Central - Sonatype"
