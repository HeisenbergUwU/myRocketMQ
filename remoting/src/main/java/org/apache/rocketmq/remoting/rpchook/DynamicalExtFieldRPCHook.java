package org.apache.rocketmq.remoting.rpchook;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

public class DynamicalExtFieldRPCHook implements RPCHook {

    @Override
    public void doBeforeRequest(String remoteAddr, RemotingCommand request) {
        // 每一个RPC 请求都设定一下时区
        String zoneName = System.getProperty(MixAll.ROCKETMQ_ZONE_PROPERTY, System.getenv(MixAll.ROCKETMQ_ZONE_ENV));
        if (StringUtils.isNotBlank(zoneName)) {
            request.addExtField(MixAll.ZONE_NAME, zoneName);
        }
        String zoneMode = System.getProperty(MixAll.ROCKETMQ_ZONE_MODE_PROPERTY, System.getenv(MixAll.ROCKETMQ_ZONE_MODE_ENV));
        if (StringUtils.isNotBlank(zoneMode)) {
            request.addExtField(MixAll.ZONE_MODE, zoneMode);
        }
    }

    @Override
    public void doAfterResponse(String remoteAddr, RemotingCommand request, RemotingCommand response) {

    }
}
