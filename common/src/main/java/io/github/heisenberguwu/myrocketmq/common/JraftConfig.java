package io.github.heisenberguwu.myrocketmq.common;

/**
 * Raft 算法的 Java 实现。
 * Raft 协议旨在解决分布式系统中如何在多个节点之间达成一致的问题，确保所有节点在某一时刻对数据的状态保持一致。它通过以下关键机制实现这一目标：
 * 领导者选举（Leader Election）：在集群中选举出一个领导者节点，负责处理客户端请求和日志复制。
 * 日志复制（Log Replication）：领导者将客户端请求以日志条目的形式复制到其他节点，确保所有节点的日志一致。
 * 安全性（Safety）：通过任期（Term）机制和日志一致性规则，确保即使在网络分区或节点故障的情况下，系统仍能保持一致性。
 */
public class JraftConfig {
    private int jRaftElectionTimeoutMs = 1000;

    private int jRaftScanWaitTimeoutMs = 1000;
    private int jRaftSnapshotIntervalSecs = 3600;
    private String jRaftGroupId = "jRaft-Controller";
    private String jRaftServerId = "localhost:9880";
    private String jRaftInitConf = "localhost:9880,localhost:9881,localhost:9882";
    private String jRaftControllerRPCAddr = "localhost:9770,localhost:9771,localhost:9772";

    public int getjRaftElectionTimeoutMs() {
        return jRaftElectionTimeoutMs;
    }

    public void setjRaftElectionTimeoutMs(int jRaftElectionTimeoutMs) {
        this.jRaftElectionTimeoutMs = jRaftElectionTimeoutMs;
    }

    public int getjRaftSnapshotIntervalSecs() {
        return jRaftSnapshotIntervalSecs;
    }

    public void setjRaftSnapshotIntervalSecs(int jRaftSnapshotIntervalSecs) {
        this.jRaftSnapshotIntervalSecs = jRaftSnapshotIntervalSecs;
    }

    public String getjRaftGroupId() {
        return jRaftGroupId;
    }

    public void setjRaftGroupId(String jRaftGroupId) {
        this.jRaftGroupId = jRaftGroupId;
    }

    public String getjRaftServerId() {
        return jRaftServerId;
    }

    public void setjRaftServerId(String jRaftServerId) {
        this.jRaftServerId = jRaftServerId;
    }

    public String getjRaftInitConf() {
        return jRaftInitConf;
    }

    public void setjRaftInitConf(String jRaftInitConf) {
        this.jRaftInitConf = jRaftInitConf;
    }

    public String getjRaftControllerRPCAddr() {
        return jRaftControllerRPCAddr;
    }

    public void setjRaftControllerRPCAddr(String jRaftControllerRPCAddr) {
        this.jRaftControllerRPCAddr = jRaftControllerRPCAddr;
    }

    public String getjRaftAddress() {
        return this.jRaftServerId;
    }

    public int getjRaftScanWaitTimeoutMs() {
        return jRaftScanWaitTimeoutMs;
    }

    public void setjRaftScanWaitTimeoutMs(int jRaftScanWaitTimeoutMs) {
        this.jRaftScanWaitTimeoutMs = jRaftScanWaitTimeoutMs;
    }
}