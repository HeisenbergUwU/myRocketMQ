package org.apache.rocketmq.remoting;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import io.netty.util.concurrent.CompleteFuture;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.remoting.netty.NettyRequestProcessor;
import org.apache.rocketmq.remoting.netty.ResponseFuture;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

/**
 * RemotingClient接口定义了RocketMQ客户端的远程通信能力，
 * 负责与NameServer或Broker等远程服务进行连接管理和请求调用，
 * 支持同步、异步、单向调用，并管理NameServer地址列表及连接状态。
 */
public interface RemotingClient extends RemotingService {
    /**
     * 更新客户端缓存的NameServer地址列表，用于负载均衡和故障切换。
     *
     * @param addrs 新的NameServer地址列表
     */
    void updateNameServerAddressList(final List<String> addrs);

    /**
     * 获取当前缓存的所有NameServer地址列表。
     *
     * @return NameServer地址列表
     */
    List<String> getNameServerAddressList();

    /**
     * 获取可用的NameServer地址列表，过滤掉不可用的地址。
     *
     * @return 可用NameServer地址列表
     */
    List<String> getAvailableNameSrvList();

    /**
     * 同步调用指定地址的远程服务，发送请求并等待响应。
     *
     * @param addr          目标服务地址（ip:port）
     * @param request       请求命令
     * @param timeoutMillis 超时时间（毫秒）
     * @return 响应命令
     * @throws InterruptedException         线程中断异常
     * @throws RemotingConnectException     连接异常
     * @throws RemotingSendRequestException 发送请求异常
     * @throws RemotingTimeoutException     请求超时异常
     */
    RemotingCommand invokeSync(final String addr, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException;

    /**
     * 异步调用指定地址的远程服务，发送请求，响应通过回调返回。
     *
     * @param addr           目标服务地址
     * @param request        请求命令
     * @param timeoutMillis  超时时间（毫秒）
     * @param invokeCallback 响应回调接口
     * @throws InterruptedException            线程中断异常
     * @throws RemotingConnectException        连接异常
     * @throws RemotingTooMuchRequestException 请求过载异常
     * @throws RemotingTimeoutException        请求超时异常
     * @throws RemotingSendRequestException    发送请求异常
     */
    void invokeAsync(final String addr, final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    /**
     * 单向调用，发送请求但不等待响应，适用于无需响应的场景。
     *
     * @param addr          目标服务地址
     * @param request       请求命令
     * @param timeoutMillis 超时时间（毫秒）
     * @throws InterruptedException            线程中断异常
     * @throws RemotingConnectException        连接异常
     * @throws RemotingTooMuchRequestException 请求过载异常
     * @throws RemotingTimeoutException        请求超时异常
     * @throws RemotingSendRequestException    发送请求异常
     */
    void invokeOneway(final String addr, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    /**
     * 单向调用，发送请求但不等待响应，适用于无需响应的场景。
     *
     * @param addr          目标服务地址
     * @param request       请求命令
     * @param timeoutMillis 超时时间（毫秒）
     * @throws InterruptedException            线程中断异常
     * @throws RemotingConnectException        连接异常
     * @throws RemotingTooMuchRequestException 请求过载异常
     * @throws RemotingTimeoutException        请求超时异常
     * @throws RemotingSendRequestException    发送请求异常
     */
    default CompletableFuture<RemotingCommand> invoke(final String addr, final RemotingCommand request, final long timeoutMillis) {
        CompletableFuture<RemotingCommand> future = new CompletableFuture<>();
        try {
            invokeAsync(addr, request, timeoutMillis, new InvokeCallback() {
                // 下述2种方法成功后才执行。
                @Override
                public void operationComplete(ResponseFuture responseFuture) {

                }

                @Override
                public void operationSucceed(RemotingCommand response) {
                    future.complete(response);
                } // 就算是 complete 也可以继续执行链式操作。

                @Override
                public void operationFail(Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    /**
     * 注册请求处理器及其线程池，用于处理服务端主动发送的请求。
     *
     * @param requestCode 请求码
     * @param processor   请求处理器实例
     * @param executor    执行线程池
     */
    void registerProcessor(final int requestCode, final NettyRequestProcessor processor, final ExecutorService executor);

    /**
     * 设置回调执行的线程池，处理异步调用回调逻辑。
     *
     * @param callbackExecutor 回调线程池
     */
    void setCallbackExecutor(final ExecutorService callbackExecutor);

    /**
     * 判断指定地址对应的网络通道是否可写。
     *
     * @param addr 服务端地址
     * @return 是否可写
     */
    boolean isChannelWritable(final String addr);

    /**
     * 判断指定服务端地址是否可达（网络连接是否正常）。
     *
     * @param addr 服务端地址
     * @return 是否可达
     */
    boolean isAddressReachable(final String addr);

    /**
     * 关闭指定地址列表对应的所有网络通道，释放资源。
     *
     * @param addrList 地址列表
     */
    void closeChannels(final List<String> addrList);
}