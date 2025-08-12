package org.apache.rocketmq.remoting;

import org.apache.rocketmq.remoting.pipeline.RequestPipeline;

/**
 * RemotingService接口定义了RocketMQ远程通信服务的基本操作，
 * 包括启动、关闭、注册RPC钩子、设置请求处理流水线等功能。
 */
public interface RemotingService {
    /**
     * 启动远程通信服务，初始化资源并开始监听。
     */
    void start();

    /**
     * 关闭远程通信服务，释放资源并停止监听。
     */
    void shutdown();

    /**
     * 注册RPC钩子，用于在请求处理前后执行特定操作。
     *
     * @param rpcHook RPC钩子实例
     */
    void registerRPCHook(RPCHook rpcHook);

    /**
     * 设置请求处理流水线，用于对请求进行预处理和后处理。
     *
     * @param pipeline 请求处理流水线实例
     */
    void setRequestPipeline(RequestPipeline pipeline);

    /**
     * 移除所有已注册的RPC钩子。
     */
    void clearRPCHook();
}

