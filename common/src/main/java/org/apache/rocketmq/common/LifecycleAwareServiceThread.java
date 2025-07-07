package org.apache.rocketmq.common;

import sun.jvm.hotspot.runtime.ServiceThread;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class LifecycleAwareServiceThread extends ServiceThread {

    private final AtomicBoolean started = new AtomicBoolean(false);


}
