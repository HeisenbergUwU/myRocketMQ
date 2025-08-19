package org.apache.rocketmq.client.latency;

/**
 * Detect whether the remote service state is normal.
 */
public interface ServiceDetector {

    /**
     * Check if the remote service is normal.
     * @param endpoint Service endpoint to check against
     * @return true if the service is back to normal; false otherwise.
     */
    boolean detect(String endpoint, long timeoutMillis);
}
