package io.github.heisenberguwu.myrocketmq.common.namesrv;

/**
 * 主要负责获取 NameServer 地址以及在地址变更时触发回调。
 */
public interface TopAddressing {
    // 返回当前客户端使用的 NameServer 地址列表（如 "ip1:9876;ip2:9876"），供 RPC 或负载均衡时使用。
    String fetchNSAddr();

    // 当 NameServer 地址发生变化（比如 DNS 更新或 HTTP 超时获取新地址）时，会触发这个回调，客户端可以重新刷新连接或路由信息。
    void registerChangeCallBack(NameServerUpdateCallback changeCallBack);
}