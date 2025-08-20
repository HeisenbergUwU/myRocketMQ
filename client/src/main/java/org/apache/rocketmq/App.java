package org.apache.rocketmq;

/**
 * | 功能/模块                        | 主要职责与内容                                                     |
 * | ---------------------------- | ----------------------------------------------------------- |
 * | `org.apache.rocketmq.client` | 实现 Producer 和 Consumer 的功能逻辑，管理客户端实例，封装客户端 API 与 Broker 的交互 |
 * | Remoting 模块                  | 提供底层网络通信支持，如编码、解码、连接管理、调用 Broker 接口等逻辑                      |
 * | 关系                           | `client` 包调用 Remoting 模块完成网络通信；在 4.x 是内嵌调用，5.x 提供 gRPC 替代方案 |
 *
 *
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}
