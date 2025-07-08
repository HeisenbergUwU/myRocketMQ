package org.apache.rocketmq.remoting.common;

public class HeartbeatV2Result {
    private int version = 0;
    private boolean isSubChange = false;
    private boolean isSupportV2 = false;

    public HeartbeatV2Result(int version, boolean isSubChange, boolean isSupportV2) {
        this.version = version; // 心跳版本号
        this.isSubChange = isSubChange; // 订阅状态
        this.isSupportV2 = isSupportV2; // 是否支持v2版本
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isSubChange() {
        return isSubChange;
    }

    public void setSubChange(boolean subChange) {
        isSubChange = subChange;
    }

    public boolean isSupportV2() {
        return isSupportV2;
    }

    public void setSupportV2(boolean supportV2) {
        isSupportV2 = supportV2;
    }
}
