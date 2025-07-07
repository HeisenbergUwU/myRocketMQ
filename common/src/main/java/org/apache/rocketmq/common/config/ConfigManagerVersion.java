package org.apache.rocketmq.common.config;

public enum ConfigManagerVersion {
    V1("v1"),
    V2("v2"),
    ;
    private final String version;

    ConfigManagerVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
