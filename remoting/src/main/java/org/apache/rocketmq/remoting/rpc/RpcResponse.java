package org.apache.rocketmq.remoting.rpc;

import org.apache.rocketmq.remoting.CommandCustomHeader;

public class RpcResponse {
    private int code; // 回答code
    private CommandCustomHeader header; // Header
    private Object body;
    public RpcException exception;

    public RpcResponse() {

    }

    public RpcResponse(int code, CommandCustomHeader header, Object body) {
        this.code = code;
        this.header = header;
        this.body = body;
    }

    public RpcResponse(RpcException rpcException) {
        this.code = rpcException.getErrorCode();
        this.exception = rpcException;
    }

    public int getCode() {
        return code;
    }

    public CommandCustomHeader getHeader() {
        return header;
    }

    public void setHeader(CommandCustomHeader header) {
        this.header = header;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public RpcException getException() {
        return exception;
    }

    public void setException(RpcException exception) {
        this.exception = exception;
    }

}
