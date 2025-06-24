package io.github.heisenberguwu.myrocketmq.common.metrics;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.context.Context;

// 累加器接口
public class NopLongCounter implements LongCounter {
    @Override
    public void add(long l) {

    }

    @Override
    public void add(long l, Attributes attributes) {

    }

    @Override
    public void add(long l, Attributes attributes, Context context) {

    }
}
