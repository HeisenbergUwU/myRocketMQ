package org.apache.rocketmq.remoting;

import org.apache.rocketmq.remoting.netty.ResponseFuture;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

// 唤醒后的回调
public interface InvokeCallback {
    /**
     * This method is expected to be invoked after {@link #operationSucceed(RemotingCommand)}
     * or {@link #operationFail(Throwable)}
     *
     * @param responseFuture the returned object contains response or exception
     */
    void operationComplete(final ResponseFuture responseFuture);

    default void operationSucceed(final RemotingCommand response) {

    }

    default void operationFail(final Throwable throwable) {

    }
}
