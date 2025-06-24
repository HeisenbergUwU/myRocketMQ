package io.github.heisenberguwu.myrocketmq.common.state;

public interface StateEventListener<T> {
    void fireEvent(T event);
}
