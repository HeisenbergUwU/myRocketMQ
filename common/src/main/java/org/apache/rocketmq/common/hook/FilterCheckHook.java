package org.apache.rocketmq.common.hook;

import java.nio.ByteBuffer;

public interface FilterCheckHook {
    String hookName();

    boolean isFilterMatched(final boolean isUnitMode, final ByteBuffer byteBuffer);
}
