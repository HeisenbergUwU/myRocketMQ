package io.github.heisenberguwu.myrocketmq.coldctr;


import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;

public class AccAndTimeStamp {
    /**
     * 冷数据计数器，线程安全的计数器 cold counter timestamp recorder？
     */
    public AtomicLong coldAcc = new AtomicLong(0L);
    public Long lastColdReadTimeMills = System.currentTimeMillis();
    public Long createTimeMills = System.currentTimeMillis();

    public AccAndTimeStamp(AtomicLong coldAcc) {
        this.coldAcc = coldAcc;
    }

    public AtomicLong getColdAcc() {
        return coldAcc;
    }

    public void setColdAcc(AtomicLong coldAcc) {
        this.coldAcc = coldAcc;
    }

    public Long getLastColdReadTimeMills() {
        return lastColdReadTimeMills;
    }

    public void setLastColdReadTimeMills(Long lastColdReadTimeMills) {
        this.lastColdReadTimeMills = lastColdReadTimeMills;
    }

    public Long getCreateTimeMills() {
        return createTimeMills;
    }

    public void setCreateTimeMills(Long createTimeMills) {
        this.createTimeMills = createTimeMills;
    }

    @Override
    public String toString() {
        return "AccAndTimeStamp{" +
                "coldAcc=" + coldAcc +
                ", lastColdReadTimeMills=" + lastColdReadTimeMills +
                ", createTimeMills=" + createTimeMills +
                '}';
    }
}