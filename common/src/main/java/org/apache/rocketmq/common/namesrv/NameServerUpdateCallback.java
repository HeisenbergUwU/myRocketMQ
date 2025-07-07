package org.apache.rocketmq.common.namesrv;

public interface NameServerUpdateCallback {
    // NS 变化之后的回调
    String onNameServerAddressChange(String namesrvAddress);
}