package org.apache.rocketmq.client.producer;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.common.ThreadLocalIndex;
import org.apache.rocketmq.client.latency.LatencyFaultTolerance;
import org.apache.rocketmq.client.latency.MQFaultStrategy;
import org.apache.rocketmq.client.latency.Resolver;
import org.apache.rocketmq.client.latency.ServiceDetector;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

public class LatencyFaultToleranceImpl implements LatencyFaultTolerance<String> {
    private final static Logger log = LoggerFactory.getLogger(MQFaultStrategy.class);
    private final ConcurrentHashMap<String, FaultItem> faultItemTable = new ConcurrentHashMap<String, FaultItem>(16);
    private int detectTimeout = 200;
    private int detectInterval = 2000;
    private final ThreadLocalIndex whichItemWorst = new ThreadLocalIndex();
    private volatile boolean startDetectorEnable = false;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "LatencyFaultToleranceScheduledThread");
        }
    });
    private final Resolver resolver;

    private final ServiceDetector serviceDetector;

    public LatencyFaultToleranceImpl(Resolver resolver, ServiceDetector serviceDetector) {
        this.resolver = resolver;
        this.serviceDetector = serviceDetector;
    }

    @Override
    public void updateFaultItem(String name, long currentLatency, long notAvailableDuration, boolean reachable) {

    }

    @Override
    public boolean isAvailable(String name) {
        return false;
    }

    @Override
    public boolean isReachable(String name) {
        return false;
    }

    @Override
    public void remove(String name) {

    }

    @Override
    public String pickOneAtLeast() {
        return "";
    }

    @Override
    public void startDetector() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void detectByOneRound() {

    }

    @Override
    public void setDetectTimeout(int detectTimeout) {

    }

    @Override
    public void setDetectInterval(int detectInterval) {

    }

    @Override
    public void setStartDetectorEnable(boolean startDetectorEnable) {

    }

    @Override
    public boolean isStartDetectorEnable() {
        return false;
    }

    public class FaultItem implements Comparable<FaultItem> {
        private final String name;
        private volatile long currentLatency;
        private volatile long startTimestamp;
        private volatile long checkStamp;
        private volatile boolean reachableFlag;

        public FaultItem(final String name) {
            this.name = name;
        }

        public void updateNotAvailableDuration(long notAvailableDuration) {
            if (notAvailableDuration > 0 && System.currentTimeMillis() + notAvailableDuration > this.startTimestamp) {
                this.startTimestamp = System.currentTimeMillis() + notAvailableDuration;
                log.info(name + " will be isolated for " + notAvailableDuration + " ms.");
            }
        }

        @Override
        public int compareTo(final FaultItem other) {
            if (this.isAvailable() != other.isAvailable()) {
                if (this.isAvailable()) {
                    return -1;
                }

                if (other.isAvailable()) {
                    return 1;
                }
            }

            if (this.currentLatency < other.currentLatency) {
                return -1;
            } else if (this.currentLatency > other.currentLatency) {
                return 1;
            }

            if (this.startTimestamp < other.startTimestamp) {
                return -1;
            } else if (this.startTimestamp > other.startTimestamp) {
                return 1;
            }
            return 0;
        }

        public void setReachable(boolean reachableFlag) {
            this.reachableFlag = reachableFlag;
        }

        public void setCheckStamp(long checkStamp) {
            this.checkStamp = checkStamp;
        }

        public boolean isAvailable() {
            return System.currentTimeMillis() >= startTimestamp;
        }

        public boolean isReachable() {
            return reachableFlag;
        }

        @Override
        public int hashCode() {
            int result = getName() != null ? getName().hashCode() : 0;
            result = 31 * result + (int) (getCurrentLatency() ^ (getCurrentLatency() >>> 32));
            result = 31 * result + (int) (getStartTimestamp() ^ (getStartTimestamp() >>> 32));
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FaultItem)) {
                return false;
            }

            final FaultItem faultItem = (FaultItem) o;

            if (getCurrentLatency() != faultItem.getCurrentLatency()) {
                return false;
            }
            if (getStartTimestamp() != faultItem.getStartTimestamp()) {
                return false;
            }
            return getName() != null ? getName().equals(faultItem.getName()) : faultItem.getName() == null;
        }

        @Override
        public String toString() {
            return "FaultItem{" + "name='" + name + '\'' + ", currentLatency=" + currentLatency + ", startTimestamp=" + startTimestamp + ", reachableFlag=" + reachableFlag + '}';
        }

        public String getName() {
            return name;
        }

        public long getCurrentLatency() {
            return currentLatency;
        }

        public void setCurrentLatency(final long currentLatency) {
            this.currentLatency = currentLatency;
        }

        public long getStartTimestamp() {
            return startTimestamp;
        }

        public void setStartTimestamp(final long startTimestamp) {
            this.startTimestamp = startTimestamp;
        }

    }
}