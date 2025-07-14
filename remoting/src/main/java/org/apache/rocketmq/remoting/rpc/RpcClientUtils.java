package org.apache.rocketmq.remoting.rpc;

import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;

import java.nio.ByteBuffer;

public class RpcClientUtils {

    public static RemotingCommand createCommandForRpcRequest(RpcRequest rpcRequest) {
        RemotingCommand cmd = RemotingCommand.createRequestCommand(rpcRequest.getCode(), rpcRequest.getHeader());
        cmd.setBody(encodeBody(rpcRequest.getBody()));
        return cmd;
    }

    public static RemotingCommand createCommandForRpcResponse(RpcResponse rpcResponse) {
        RemotingCommand cmd = RemotingCommand.createResponseCommandWithHeader(rpcResponse.getCode(), rpcResponse.getHeader());
        cmd.setRemark(rpcResponse.getException() == null ? "" : rpcResponse.getException().getMessage());
        cmd.setBody(encodeBody(rpcResponse.getBody()));
        return cmd;
    }

    public static byte[] encodeBody(Object body) {
        if (body == null) {
            return null;
        }
        if (body instanceof byte[]) {
            return (byte[]) body;
        } else if (body instanceof RemotingSerializable) {
            // RemotingSerializable的 encode 方法： 转化成 json-utf8 然后转换成 byte[]
            return ((RemotingSerializable) body).encode();
        } else if (body instanceof ByteBuffer) {
            ByteBuffer buffer = (ByteBuffer) body;
            /**
             *     public final Buffer mark() {
             *         mark = position;
             *         return this;
             *     }
             */
            buffer.mark(); // 记录当前position 位置。
            byte[] data = new byte[buffer.remaining()]; // 还有多少没读
            buffer.get(data);
            buffer.reset(); // position = mark;
            return data;
        } else {
            throw new RuntimeException("Unsupported body type " + body.getClass());
        }
    }

}