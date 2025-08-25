package org.apache.rocketmq.client.producer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.rocketmq.common.message.Message;

public class RequestResponseFuture {
    private final String correlationId; // 唯一标识符
    private final RequestCallback requestCallback; // 请求回调
    private final long beginTimestamp = System.currentTimeMillis(); // 开始时间
    private final Message requestMsg = null; // 消息实体
    private long timeoutMillis; // 过期时间
    private CountDownLatch countDownLatch = new CountDownLatch(1); // 阻塞一次性锁
    private volatile Message responseMsg = null; // 返回信息
    private volatile boolean sendRequestOk = true; // 发送成功判断
    private volatile Throwable cause = null;

    public RequestResponseFuture(String correlationId, long timeoutMillis, RequestCallback requestCallback) {
        this.correlationId = correlationId;
        this.timeoutMillis = timeoutMillis;
        this.requestCallback = requestCallback;
    }

    public void executeRequestCallback() {
        if (requestCallback != null) {
            if (sendRequestOk && cause == null) {
                requestCallback.onSuccess(responseMsg);
            } else {
                requestCallback.onException(cause);
            }
        }
    }

    public boolean isTimeout() {
        long diff = System.currentTimeMillis() - this.beginTimestamp;
        return diff > this.timeoutMillis;
    }

    public Message waitResponseMessage(final long timeout) throws InterruptedException {
        this.countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
        return this.responseMsg;
    }

    public void putResponseMessage(final Message responseMsg) {
        this.responseMsg = responseMsg;
        this.countDownLatch.countDown();
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public RequestCallback getRequestCallback() {
        return requestCallback;
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public void setCountDownLatch(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    public Message getResponseMsg() {
        return responseMsg;
    }

    public void setResponseMsg(Message responseMsg) {
        this.responseMsg = responseMsg;
    }

    public boolean isSendRequestOk() {
        return sendRequestOk;
    }

    public void setSendRequestOk(boolean sendRequestOk) {
        this.sendRequestOk = sendRequestOk;
    }

    public void acquireCountDownLatch() {
        this.countDownLatch.countDown();
    }

    public Message getRequestMsg() {
        return requestMsg;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }
}
