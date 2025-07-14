package org.apache.rocketmq.remoting.netty;

import io.netty.channel.Channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.common.SemaphoreReleaseOnlyOnce;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

/**
 * 用于处理一次请求响应的核心类，在使用之后就失效了。
 */
public class ResponseFuture {
    private final Channel channel; // 使用的 Netty 通道
    private final int opaque; // 唯一请求标识符，用于匹配响应
    private final RemotingCommand request; // 请求消息体
    private final long timeoutMillis; // 尝试时长
    private final InvokeCallback invokeCallback; // 回调函数
    private final long beginTimestamp = System.currentTimeMillis(); // 开始时间戳
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // 同步控制模块

    private final SemaphoreReleaseOnlyOnce once; // 信号量释放工具，只释放一次。

    private final AtomicBoolean executeCallbackOnlyOnce = new AtomicBoolean(false); // 并发标记安全位，确保回调只执行一次。
    private volatile RemotingCommand responseCommand; // 存放收到的响应命令
    private volatile boolean sendRequestOK = true; // 是否发送成功
    private volatile Throwable cause; // 异常原因
    private volatile boolean interrupted = false; // 线程中断标识符

    public ResponseFuture(Channel channel, int opaque, long timeoutMillis, InvokeCallback invokeCallback,
                          SemaphoreReleaseOnlyOnce once) {
        this(channel, opaque, null, timeoutMillis, invokeCallback, once);
    }

    public ResponseFuture(Channel channel, int opaque, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback,
                          SemaphoreReleaseOnlyOnce once) {
        this.channel = channel;
        this.opaque = opaque;
        this.request = request;
        this.timeoutMillis = timeoutMillis;
        this.invokeCallback = invokeCallback;
        this.once = once;
    }

    public void executeInvokeCallback() {
        if (invokeCallback != null) {
            if (this.executeCallbackOnlyOnce.compareAndSet(false, true)) {
                RemotingCommand response = getResponseCommand();
                if (response != null) {
                    invokeCallback.operationSucceed(response);
                } else {
                    if (!isSendRequestOK()) {
                        invokeCallback.operationFail(new RemotingSendRequestException(channel.remoteAddress().toString(), getCause()));
                    } else if (isTimeout()) {
                        invokeCallback.operationFail(new RemotingTimeoutException(channel.remoteAddress().toString(), getTimeoutMillis(), getCause()));
                    } else {
                        invokeCallback.operationFail(new RemotingException(getRequestCommand().toString(), getCause()));
                    }
                }
                invokeCallback.operationComplete(this);
            }
        }
    }

    public void interrupt() {
        interrupted = true;
        executeInvokeCallback();
    }

    public void release() {
        if (this.once != null) {
            this.once.release();
        }
    }

    public boolean isTimeout() {
        long diff = System.currentTimeMillis() - this.beginTimestamp;
        return diff > this.timeoutMillis;
    }

    public RemotingCommand waitResponse(final long timeoutMillis) throws InterruptedException {
        this.countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        return this.responseCommand;
    }

    public void putResponse(final RemotingCommand responseCommand) {
        this.responseCommand = responseCommand;
        this.countDownLatch.countDown();
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    public boolean isSendRequestOK() {
        return sendRequestOK;
    }

    public void setSendRequestOK(boolean sendRequestOK) {
        this.sendRequestOK = sendRequestOK;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public InvokeCallback getInvokeCallback() {
        return invokeCallback;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public RemotingCommand getResponseCommand() {
        return responseCommand;
    }

    public void setResponseCommand(RemotingCommand responseCommand) {
        this.responseCommand = responseCommand;
    }

    public int getOpaque() {
        return opaque;
    }

    public RemotingCommand getRequestCommand() {
        return request;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    @Override
    public String toString() {
        return "ResponseFuture [responseCommand=" + responseCommand + ", sendRequestOK=" + sendRequestOK
                + ", cause=" + cause + ", opaque=" + opaque + ", timeoutMillis=" + timeoutMillis
                + ", invokeCallback=" + invokeCallback + ", beginTimestamp=" + beginTimestamp
                + ", countDownLatch=" + countDownLatch + "]";
    }
}