package org.apache.rocketmq.remoting.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.opentelemetry.api.common.AttributesBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;

import org.apache.rocketmq.common.AbortProcessException;
import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.common.Pair;
import org.apache.rocketmq.common.ServiceThread;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.common.utils.ExceptionUtils;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;
import org.apache.rocketmq.remoting.ChannelEventListener;
import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.common.SemaphoreReleaseOnlyOnce;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.remoting.metrics.RemotingMetricsManager;
import org.apache.rocketmq.remoting.pipeline.RequestPipeline;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSysResponseCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;

import static org.apache.rocketmq.remoting.metrics.RemotingMetricsConstant.LABEL_IS_LONG_POLLING;
import static org.apache.rocketmq.remoting.metrics.RemotingMetricsConstant.LABEL_REQUEST_CODE;
import static org.apache.rocketmq.remoting.metrics.RemotingMetricsConstant.LABEL_RESPONSE_CODE;
import static org.apache.rocketmq.remoting.metrics.RemotingMetricsConstant.LABEL_RESULT;
import static org.apache.rocketmq.remoting.metrics.RemotingMetricsConstant.RESULT_ONEWAY;
import static org.apache.rocketmq.remoting.metrics.RemotingMetricsConstant.RESULT_PROCESS_REQUEST_FAILED;
import static org.apache.rocketmq.remoting.metrics.RemotingMetricsConstant.RESULT_WRITE_CHANNEL_FAILED;

/**
 * 用来封装 RocketMQ 的 Client 和 Server 行为
 */
public abstract class NettyRemotingAbstract {
    /**
     * Remoting logger instance.
     */
    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_REMOTING_NAME);

    // 限制最大并发 oneway 请求数量，避免内存占用超额
    protected final Semaphore semaphoreOneway;

    // 限制最大并发请求数量，保护系统内存资源。
    protected final Semaphore semaphoreAsync;

    // 存放所有“正在进行中”的请求，通过opaque进行识别。这些对象是一次性的。
    protected final ConcurrentMap<Integer/* opaque */, ResponseFuture> responseTable = new ConcurrentHashMap<>(256);

    protected final HashMap<Integer/*request code*/, Pair<NettyRequestProcessor, ExecutorService>> processorTable = new HashMap<>(64);

    protected final NettyEventExecutor nettyEventExecutor = new NettyEventExecutor();

    /**
     * 自定义 channel event listener
     *
     * @return
     */
    public abstract ChannelEventListener getChannelEventListener();


    /**
     *
     */
    class NettyEventExecutor extends ServiceThread {

        private final LinkedBlockingQueue<NettyEvent> eventQueue = new LinkedBlockingQueue<>();

        public void putNettyEvent(final NettyEvent event) {
            int currentSize = this.eventQueue.size();
            int maxSize = 10000;
            if (currentSize <= maxSize) {
                this.eventQueue.add(event);
            } else {
                log.warn("event queue size [{}] over the limit [{}], so drop this event {}", currentSize, maxSize, event.toString());
            }
        }

        @Override
        public String getServiceName() {
            return NettyEventExecutor.class.getSimpleName();
        }

        @Override
        public void run() {
            log.info(this.getServiceName() + " service started");

            final ChannelEventListener listener = NettyRemotingAbstract.this.getChannelEventListener();
            while (!this.isStopped()) {
                try {
                    // eventQueue 是一个 LinkedBlockingQueue , 这里使用 poll 是为了防止一直 阻塞 或者 忙轮训。
                    /**
                     * poll()：非阻塞，立即返回队列头或者 null（若为空）
                     *
                     * poll(long timeout, TimeUnit unit)：最多等待指定时间，若在这段时间内有元素入队，会立即取走并返回；若仍无元素，则返回 null。该方法也可能因线程中断抛出 InterruptedException
                     *
                     * take()：无限期阻塞等待，直到有可取元素。
                     *
                     *
                     *     public E poll(long timeout, TimeUnit unit) throws InterruptedException {
                     *         E x = null;
                     *         int c = -1;
                     *         long nanos = unit.toNanos(timeout);
                     *         final AtomicInteger count = this.count;
                     *         final ReentrantLock takeLock = this.takeLock;
                     *         takeLock.lockInterruptibly();
                     *         try {
                     *             while (count.get() == 0) {
                     *                 if (nanos <= 0)
                     *                     return null;
                     *                 nanos = notEmpty.awaitNanos(nanos);
                     *             }
                     *             x = dequeue();
                     *             c = count.getAndDecrement();
                     *             if (c > 1)
                     *                 notEmpty.signal();
                     *         } finally {
                     *             takeLock.unlock();
                     *         }
                     *         if (c == capacity)
                     *             signalNotFull();
                     *         return x;
                     *     }
                     */
                    NettyEvent event = this.eventQueue.poll(3000, TimeUnit.MILLISECONDS);
                    if (event != null && listener != null) {
                        // listener 负责将事件上传给对应的业务。
                        switch (event.getType()) {
                            case IDLE:
                                listener.onChannelIdle(event.getRemoteAddr(), event.getChannel());
                                break;
                            case CLOSE:
                                listener.onChannelClose(event.getRemoteAddr(), event.getChannel());
                                break;
                            case CONNECT:
                                listener.onChannelConnect(event.getRemoteAddr(), event.getChannel());
                                break;
                            case EXCEPTION:
                                listener.onChannelException(event.getRemoteAddr(), event.getChannel());
                                break;
                            case ACTIVE:
                                listener.onChannelActive(event.getRemoteAddr(), event.getChannel());
                                break;
                            default:
                                break;
                        }
                    }
                } catch (Exception e) {
                    log.warn(this.getServiceName() + " service has exception. ", e);
                }
            }
            log.info(this.getServiceName() + " service end");
        }
    }
}