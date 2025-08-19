package org.apache.rocketmq.client.rpchook;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;


public class NamespaceRpcHook implements RPCHook {
    private final ClientConfig clientConfig;

    public NamespaceRpcHook(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    /**
     * 在 RocketMQ 中，RPCHook 接口用于在发送 RPC 请求前后执行自定义操作。NamespaceRpcHook 类通过在请求头中添加命名空间信息，
     * 使得 RocketMQ 的服务端能够识别和处理不同命名空间的请求，从而实现多租户环境下的资源隔离和管理。
     *
     * @param remoteAddr
     * @param request
     */
    @Override
    public void doBeforeRequest(String remoteAddr, RemotingCommand request) {
        if (StringUtils.isNotEmpty(clientConfig.getNamespaceV2())) {
            request.addExtField(MixAll.RPC_REQUEST_HEADER_NAMESPACED_FIELD, "true");
            request.addExtField(MixAll.RPC_REQUEST_HEADER_NAMESPACE_FIELD, clientConfig.getNamespaceV2());
        }
    }

    @Override
    public void doAfterResponse(String remoteAddr, RemotingCommand request,
                                RemotingCommand response) {

    }
}
