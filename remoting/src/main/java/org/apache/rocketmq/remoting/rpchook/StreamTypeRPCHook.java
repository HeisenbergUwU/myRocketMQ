package org.apache.rocketmq.remoting.rpchook;

import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestType;

public class StreamTypeRPCHook implements RPCHook {
    @Override
    public void doBeforeRequest(String remoteAddr, RemotingCommand request) {
        request.addExtField(MixAll.REQ_T, String.valueOf(RequestType.STREAM.getCode()));
    }

    @Override
    public void doAfterResponse(String remoteAddr, RemotingCommand request,
                                RemotingCommand response) {

    }
}
