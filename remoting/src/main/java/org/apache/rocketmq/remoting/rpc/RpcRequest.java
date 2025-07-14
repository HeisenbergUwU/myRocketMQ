package org.apache.rocketmq.remoting.rpc;

// RPC 请求类型
public class RpcRequest {
    int code;
    private RpcRequestHeader header;
    private Object body;

    public RpcRequest(int code, RpcRequestHeader header, Object body) {
        this.code = code;
        this.header = header;
        this.body = body;
    }

    public RpcRequestHeader getHeader() {
        return header;
    }

    public Object getBody() {
        return body;
    }

    public int getCode() {
        return code;
    }
}
