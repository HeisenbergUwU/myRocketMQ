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


public interface RemotingClient extends RemotingService {

    void updateNameServerAddressList(final List<String> addrs);

    List<String> getNameServerAddressList();

    List<String> getAvailableNameSrvList();

    RemotingCommand invokeSync(final String addr, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException;

    void invokeAsync(final String addr, final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    void invokeOneway(final String addr, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    default CompletableFuture<RemotingCommand> invoke(final String addr, final RemotingCommand request, final long timeoutMillis) {
        CompletableFuture<RemotingCommand> future = new CompletableFuture<>();
        try {
            invokeAsync(addr, request, timeoutMillis, new InvokeCallback() {

                @Override
                public void operationComplete(ResponseFuture responseFuture) {

                }

                @Override
                public void operationSucceed(RemotingCommand response) {
                    future.complete(response);
                }

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

    void registerProcessor(final int requestCode, final NettyRequestProcessor processor, final ExecutorService executor);

    void setCallbackExecutor(final ExecutorService callbackExecutor);

    boolean isChannelWritable(final String addr);

    boolean isAddressReachable(final String addr);

    void closeChannels(final List<String> addrList);
}