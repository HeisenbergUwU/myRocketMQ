package org.apache.rocketmq.remoting.rpc;


import java.util.concurrent.Future;

import org.apache.rocketmq.common.message.MessageQueue;


public interface RpcClient {

    //common invoke paradigm, the logic remote addr is defined in "bname" field of request
    //For oneway request, the sign is labeled in request, and do not need an another method named "invokeOneway"
    //For one
    Future<RpcResponse> invoke(RpcRequest request, long timeoutMs) throws RpcException;

    //For rocketmq, most requests are corresponded to MessageQueue
    //And for LogicQueue, the broker name is mocked, the physical addr could only be defined by MessageQueue
    Future<RpcResponse> invoke(MessageQueue mq, RpcRequest request, long timeoutMs) throws RpcException;


}
