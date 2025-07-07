package org.apache.rocketmq.common.metrics;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.context.Context;


// Histogram 历史统计接口
public class NopLongHistogram implements LongHistogram {
    @Override public void record(long l) {

    }

    @Override public void record(long l, Attributes attributes) {

    }

    @Override public void record(long l, Attributes attributes, Context context) {

    }
}
