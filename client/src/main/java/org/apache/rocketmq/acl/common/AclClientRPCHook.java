package org.apache.rocketmq.acl.common;

import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class AclClientRPCHook implements RPCHook {
    private final SessionCredentials sessionCredentials;

    public AclClientRPCHook(SessionCredentials sessionCredentials) {
        this.sessionCredentials = sessionCredentials;
    }

    @Override
    public void doBeforeRequest(String remoteAddr, RemotingCommand request) {
        // Add AccessKey and SecurityToken into signature calculating.
        request.addExtField(SessionCredentials.ACCESS_KEY, sessionCredentials.getAccessKey());
        // The SecurityToken value is unnecessary,user can choose this one.
        if (sessionCredentials.getSecurityToken() != null) {
            request.addExtField(SessionCredentials.SECURITY_TOKEN, sessionCredentials.getSecurityToken());
        }
        //
        byte[] total = AclUtils.combineRequestContent(request, parseRequestContent(request));
        String signature = AclUtils.calSignature(total, sessionCredentials.getSecretKey());
        request.addExtField(SessionCredentials.SIGNATURE, signature);
    }

    @Override
    public void doAfterResponse(String remoteAddr, RemotingCommand request, RemotingCommand response) {

    }

    /**
     *
     * @param request
     * @return
     */
    protected SortedMap<String, String> parseRequestContent(RemotingCommand request) {
        request.makeCustomHeaderToNet();
        Map<String, String> extFields = request.getExtFields();
        // Sort property
        return new TreeMap<>(extFields);
    }

    public SessionCredentials getSessionCredentials() {
        return sessionCredentials;
    }
}