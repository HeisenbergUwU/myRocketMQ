package io.github.heisenberguwu.myrocketmq.common.metrics;

// 定义了不同的度量（metrics）导出方式类型
public enum MetricsExporterType {
    DISABLE(0),     // 不导出度量
    OTLP_GRPC(1),   // 通过 OTLP/gRPC 导出
    PROM(2),        // 通过 Prometheus 导出
    LOG(3);         // 输出到日志

    private final int value;

    MetricsExporterType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MetricsExporterType valueOf(int value) {
        switch (value) {
            case 1:
                return OTLP_GRPC;
            case 2:
                return PROM;
            case 3:
                return LOG;
            default:
                return DISABLE;
        }
    }

    public boolean isEnable() {
        return this.value > 0;
    }
}
