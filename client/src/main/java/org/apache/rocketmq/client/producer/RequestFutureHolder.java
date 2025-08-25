package org.apache.rocketmq.client.producer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.common.ClientErrorCode;
import org.apache.rocketmq.client.exception.RequestTimeoutException;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.common.ThreadFactoryImpl;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

public class RequestFutureHolder {
    private static final Logger log = LoggerFactory.getLogger(RequestFutureHolder.class);
    private static final RequestFutureHolder INSTANCE = new RequestFutureHolder();
    private ConcurrentHashMap<String, RequestResponseFuture> requestFutureTable = new ConcurrentHashMap<>();
    private final Set<DefaultMQProducerImpl> producerSet = new HashSet<>();
    private ScheduledExecutorService scheduledExecutorService = null;

    public ConcurrentHashMap<String, RequestResponseFuture> getRequestFutureTable() {
        return requestFutureTable;
    }

    private void scanExpiredRequest() {
        final List<RequestResponseFuture> rfList = new LinkedList<>();
        Iterator<Map.Entry<String, RequestResponseFuture>> it = requestFutureTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, RequestResponseFuture> next = it.next();
            RequestResponseFuture rep = next.getValue();

            if (rep.isTimeout()) {
                it.remove();
                rfList.add(rep);
                log.warn("remove timeout request, CorrelationId={}" + rep.getCorrelationId());
            }
        }
        // 执行过期消息的回调
        for (RequestResponseFuture rf : rfList) {
            try {
                Throwable cause = new RequestTimeoutException(ClientErrorCode.REQUEST_TIMEOUT_EXCEPTION, "request timeout, no reply message.");
                rf.setCause(cause);
                rf.executeRequestCallback();
            } catch (Throwable e) {
                log.warn("scanResponseTable, operationComplete Exception", e);
            }
        }
    }

    public synchronized void startScheduledTask(DefaultMQProducerImpl producer) {
        this.producerSet.add(producer);
        if (null == scheduledExecutorService) {
            this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("RequestHouseKeepingService"));

            this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        RequestFutureHolder.getInstance().scanExpiredRequest();
                    } catch (Throwable e) {
                        log.error("scan RequestFutureTable exception", e);
                    }
                }
            }, 1000 * 3, 1000, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void shutdown(DefaultMQProducerImpl producer) {
        this.producerSet.remove(producer);
        if (this.producerSet.size() <= 0 && null != this.scheduledExecutorService) {
            ScheduledExecutorService executorService = this.scheduledExecutorService;
            this.scheduledExecutorService = null;
            executorService.shutdown();
        }
    }

    private RequestFutureHolder() {
    }

    public static RequestFutureHolder getInstance() {
        return INSTANCE;
    }
}
