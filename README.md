## 🚀 RocketMQ 源码复现与学习项目

本项目旨在作为学习 [Apache RocketMQ](https://rocketmq.apache.org/) 的辅助资料，**通过手动复现其核心模块与设计理念**，加深对其消息队列模型、架构设计、底层机制的理解。

---

### 📚 项目背景

RocketMQ 是阿里巴巴开源的分布式消息中间件，现已成为 Apache 顶级项目，具有高吞吐、低延迟、高可用、分布式、高可靠等特点。

由于 RocketMQ 官方代码结构较为复杂，对于刚开始学习的同学来说不太友好。因此本项目以“复现 + 拆解 + 注释”的方式，帮助读者**从零搭建消息队列核心模块**，包括：

* NameServer 模块原理与轻量级实现
* Broker 启动流程与 CommitLog 存储机制
* 消息发送、消费、拉取协议
* Producer / Consumer 客户端模拟
* Netty 通信协议处理器实现
* 以及 Zero-copy、MappedFile、HA机制等底层优化技巧
### 📚 推荐阅读路径
以源码顺序为主，以下是推荐的阅读顺序（逐步深入）：

1. remoting：理解通信协议和序列化逻辑
2. client：理解生产者/消费者端的核心流程
3. store：掌握消息持久化、索引和队列管理
4. broker：深入消息路由、调度、请求处理和 HA
5. （可选）namesrv：理解轻量级路由服务机制
---

### 🧩 项目结构
RocketMQ 开源项目的整体结构

| **目录**                       | **作用与功能说明**                                   |
| ---------------------------- | --------------------------------------------- |
| **broker/**                  | Broker 核心模块：启动流程、消息接收、存储、消费、主备 HA、集群治理等       |
| **namesrv/**                 | NameServer 模块：服务注册发现、心跳管理、路由信息维护              |
| **client/**                  | 客户端 API：Producer/Consumer 的发送、拉取、事务、过滤逻辑      |
| **common/**                  | 公共模块：协议定义、配置解析、序列化、线程池、日志等工具类                 |
| **remoting/**                | 通信层：基于 Netty 实现 RPC 请求/响应、连接管理、消息封装           |
| **store/**                   | 存储核心：CommitLog、ConsumeQueue、IndexFile、刷盘与检索机制 |
| **filter/** & **filtersrv/** | 过滤模块：消息过滤机制与 Filter Server 支持                 |
| **openmessaging/**           | OpenMessaging 标准支持层：兼容并接入多协议                  |
| **logappender/**             | 日志收集上报模块：集群日志传输与分析支持                          |
| **tools/**                   | 管理命令与脚本：mqadmin、监控脚本、tools.sh 等               |
| **example/**                 | 示例代码：常见业务场景演示，快速上手                            |
| **test/**                    | 自动化测试：单元测试与集成测试验证各模块                          |
| **style/**                   | 代码规范：Checkstyle 配置与代码格式指引                     |
| **distribution/**、**dev/** 等 | 部署包与开发辅助资源，如构建脚本、文档样例                         |


---

### ✅ 已实现功能

* [] 模拟 NameServer 注册中心与路由表
* [] Broker 启动流程、Topic 初始化
* [] Producer 发送消息到 Broker 的基本链路
* [] 基于 Netty 的 RemotingCommand 封装与解析
* [] CommitLog 文件存储模拟（带 MappedByteBuffer）
* [] 消费者拉取消息逻辑、消费队列管理
* [] 简单 HA 高可用机制介绍与实验

---

### 🛠️ 技术栈

| 模块     | 技术                     |
| ------ | ---------------------- |
| 编程语言   | Java 8+               |
| 构建工具   | Maven                  |
| 网络通信   | Netty 4.x              |
| 日志记录   | SLF4J + Logback        |
| 序列化    | 自定义命令结构，兼容 JSON        |
| IDE 支持 | IntelliJ IDEA / VSCode |
| 文档     | Markdown + 注释 + 示例日志打印 |

---

### 🧪 如何运行

> 需提前安装好 JDK 8 和 Maven

---

### 📌 学习重点 & 注释说明

---

### 🎯 项目目标

本项目是为了\*\*“复现以深入理解”\*\*，目标并非完全功能齐备的 RocketMQ 副本，而是：

1. **搭建学习环境**：帮助自己理解源代码架构与模块职责
2. **追踪执行流程**：通过日志与注释记录实际运行路径
3. **实验架构演化**：可以逐步演化出消息重试、消费进度存储、Topic 自动创建等进阶机制
4. **后续可视化**：未来可能加入简单的 Web 控制台

---

### 📖 推荐配套资料

* [RocketMQ 官方文档](https://rocketmq.apache.org/docs/)
* 源码版本参考：[rocketmq-all 5.3.3](https://github.com/apache/rocketmq/tree/5.3.3)
* Netty 官方入门教程
* Java NIO 与 MappedByteBuffer 详解

---

### 🙋‍♂️ 作者笔记

本项目由个人为学习 RocketMQ 而搭建。每一份代码背后都力求“**从源码学设计**”，如果你也有类似的目标，欢迎讨论、交流、提交 PR！