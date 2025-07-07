package org.apache.rocketmq.common;

public enum BoundaryType {
    /**
     * 用来表示边界的枚举类型
     * | 场景          | 示例                                     |
     * | ----------- | -------------------------------------- |
     * | **范围查询**    | 比如时间、分区、消息偏移等上下限的处理                    |
     * | **排序/范围校验** | 比如判断一个值是否落在指定边界内                       |
     * | **构造条件语句**  | SQL 查询、过滤规则、分片逻辑等                      |
     * | **判断边界方向**  | 比如 compaction、range split、message scan |
     */
    /**
     * Indicate that lower boundary is expected.
     */
    LOWER("lower"),

    /**
     * Indicate that upper boundary is expected.
     */
    UPPER("upper");

    private String name;

    BoundaryType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static BoundaryType getType(String name) {
        if (BoundaryType.UPPER.getName().equalsIgnoreCase(name)) {
            return UPPER;
        }
        return LOWER;
    }
}
