package org.apache.rocketmq.common;

public interface ObjectCreator<T> {
    T create(Object... args);
}
