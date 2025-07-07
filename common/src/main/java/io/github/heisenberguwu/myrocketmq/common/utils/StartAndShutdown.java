package io.github.heisenberguwu.myrocketmq.common.utils;

public interface StartAndShutdown extends Start, Shutdown {
    default void preShutdown() throws Exception {
    }
}
