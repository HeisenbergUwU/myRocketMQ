package org.apache.rocketmq.remoting.rpc;

import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.netty.ResponseFuture;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.admin.TopicStatsTable;
import org.apache.rocketmq.remoting.protocol.header.GetEarliestMsgStoretimeResponseHeader;
import org.apache.rocketmq.remoting.protocol.header.GetMaxOffsetResponseHeader;
import org.apache.rocketmq.remoting.protocol.header.GetMinOffsetResponseHeader;
import org.apache.rocketmq.remoting.protocol.header.PullMessageResponseHeader;
import org.apache.rocketmq.remoting.protocol.header.QueryConsumerOffsetResponseHeader;
import org.apache.rocketmq.remoting.protocol.header.SearchOffsetResponseHeader;
import org.apache.rocketmq.remoting.protocol.header.UpdateConsumerOffsetResponseHeader;
import org.apache.rocketmq.remoting.protocol.statictopic.TopicConfigAndQueueMapping;


public class RpcClientImpl implements RpcClient {

    private ClientMetadata clientMetadata;

    private RemotingClient remotingClient;

    private List<RpcClientHook> clientHookList = new ArrayList<>();

    public RpcClientImpl(ClientMetadata clientMetadata, RemotingClient remotingClient) {
        this.clientMetadata = clientMetadata;
        this.remotingClient = remotingClient;
    }

    public void registerHook(RpcClientHook hook) {
        clientHookList.add(hook);
    }

    @Override
    public Future<RpcResponse> invoke(RpcRequest request, long timeoutMs) throws RpcException {
        return null;
    }

    // Promise 是 Netty 对于 Future 的一个扩展接口，引入了可写能力。允许我来主动标记一个异步操作状态的。
    public Promise<RpcClientHook> createResponseFuture() {
        // ImmediateEventExecutor ，可以在当前线程直接调用 run方法快速开启任务。
        // 通过这个单例对象创建一个可写状态的 Promise
        return ImmediateEventExecutor.INSTANCE.newPromise();
    }

    @Override
    public Future<RpcResponse> invoke(MessageQueue mq, RpcRequest request, long timeoutMs) throws RpcException {
        return null;
    }

    private String getBrokerAddrByNameOrException(String bname) throws RpcException {
        String addr = this.clientMetadata.findMasterBrokerAddr(bname);
        if (addr == null) {
            throw new RpcException(ResponseCode.SYSTEM_ERROR, "cannot find addr for broker " + bname);
        }
        return addr;
    }

    private void processFailedResponse(String addr, RemotingCommand requestCommand, ResponseFuture responseFuture, Promise<RpcResponse> rpcResponsePromise) {
        RemotingCommand responseCommand = responseFuture.getResponseCommand();
        if (responseCommand != null) {
            //this should not happen
            return;
        }
        int errorCode = ResponseCode.RPC_UNKNOWN;
        String errorMessage = null;
        if (!responseFuture.isSendRequestOK()) {
            errorCode = ResponseCode.RPC_SEND_TO_CHANNEL_FAILED;
            errorMessage = "send request failed to " + addr + ". Request: " + requestCommand;
        } else if (responseFuture.isTimeout()) {
            errorCode = ResponseCode.RPC_TIME_OUT;
            errorMessage = "wait response from " + addr + " timeout :" + responseFuture.getTimeoutMillis() + "ms" + ". Request: " + requestCommand;
        } else {
            errorMessage = "unknown reason. addr: " + addr + ", timeoutMillis: " + responseFuture.getTimeoutMillis() + ". Request: " + requestCommand;
        }
        rpcResponsePromise.setSuccess(new RpcResponse(new RpcException(errorCode, errorMessage)));
    }
}