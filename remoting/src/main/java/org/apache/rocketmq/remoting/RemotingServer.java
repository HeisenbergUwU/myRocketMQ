package org.apache.rocketmq.remoting;

import io.netty.channel.Channel;

import java.util.concurrent.ExecutorService;

import org.apache.rocketmq.common.Pair;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.remoting.netty.NettyRequestProcessor;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

/**
 * RemotingServer接口定义了RocketMQ服务端的网络通信能力，
 * 提供请求处理器注册、服务端口管理及多种调用方式（同步、异步、单向）的方法。
 */
public interface RemotingServer extends RemotingService {
    /**
     * 注册特定请求码对应的处理器及其执行线程池。
     *
     * @param requestCode 请求码，用于区分不同请求类型
     * @param processor   处理请求的NettyRequestProcessor实例
     * @param executor    处理请求的线程池
     */
    void registerProcessor(final int requestCode, final NettyRequestProcessor processor, final ExecutorService executor);

    /**
     * 注册默认的请求处理器及其执行线程池。
     * 当请求码未匹配到具体处理器时使用该默认处理器。
     *
     * @param processor 默认处理器
     * @param executor  处理线程池
     */
    void registerDefaultProcessor(final NettyRequestProcessor processor, final ExecutorService executor);

    /**
     * 获取本地服务监听端口号。
     *
     * @return 本地监听端口
     */
    int localListenPort();

    /**
     * 根据请求码获取对应的处理器及其执行线程池。
     *
     * @param requestCode 请求码
     * @return 处理器与线程池的组合Pair
     */
    Pair<NettyRequestProcessor, ExecutorService> getProcessorPair(final int requestCode);

    /**
     * 获取默认处理器及其执行线程池。
     *
     * @return 默认处理器与线程池组合
     */
    Pair<NettyRequestProcessor, ExecutorService> getDefaultProcessorPair();

    /**
     * 创建并返回绑定指定端口的新的RemotingServer实例。
     *
     * @param port 监听端口号
     * @return 新的RemotingServer实例
     */
    RemotingServer newRemotingServer(int port);

    /**
     * 移除指定端口对应的RemotingServer，释放资源。
     *
     * @param port 需要移除的端口号
     */
    void removeRemotingServer(int port);

    /**
     * 同步调用接口，发送请求并阻塞等待响应结果。
     *
     * @param channel       发送请求的Netty通道
     * @param request       请求命令
     * @param timeoutMillis 超时时间（毫秒）
     * @return 响应命令
     * @throws InterruptedException         线程被中断异常
     * @throws RemotingSendRequestException 请求发送失败异常
     * @throws RemotingTimeoutException     请求超时异常
     */
    RemotingCommand invokeSync(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException;

    /**
     * 异步调用接口，发送请求并通过回调处理响应。
     *
     * @param channel        发送请求的Netty通道
     * @param request        请求命令
     * @param timeoutMillis  超时时间（毫秒）
     * @param invokeCallback 响应回调处理接口
     * @throws InterruptedException            线程被中断异常
     * @throws RemotingTooMuchRequestException 请求过载异常
     * @throws RemotingTimeoutException        请求超时异常
     * @throws RemotingSendRequestException    请求发送失败异常
     */
    void invokeAsync(final Channel channel, final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    /**
     * 单向调用接口，发送请求但不等待响应，适合无需回复的场景。
     *
     * @param channel       发送请求的Netty通道
     * @param request       请求命令
     * @param timeoutMillis 超时时间（毫秒）
     * @throws InterruptedException            线程被中断异常
     * @throws RemotingTooMuchRequestException 请求过载异常
     * @throws RemotingTimeoutException        请求超时异常
     * @throws RemotingSendRequestException    请求发送失败异常
     */
    void invokeOneway(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;
}