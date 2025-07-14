package org.apache.rocketmq.remoting.rpc;

// RPC 回调类
public abstract class RpcClientHook {

    //if the return is not null, return it
    public abstract RpcResponse beforeRequest(RpcRequest rpcRequest) throws RpcException;

    //if the return is not null, return it
    public abstract RpcResponse afterResponse(RpcResponse rpcResponse) throws RpcException;

}