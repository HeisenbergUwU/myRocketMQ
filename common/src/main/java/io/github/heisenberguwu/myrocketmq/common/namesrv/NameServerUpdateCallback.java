package io.github.heisenberguwu.myrocketmq.common.namesrv;

public interface NameServerUpdateCallback {
    // NS 变化之后的回调
    String onNameServerAddressChange(String namesrvAddress);
}