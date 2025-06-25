package io.github.heisenberguwu.myrocketmq.common.filter;

/**
 * RocketMQ 过滤表达式类型 TAG
 */
public class ExpressionType {

    // 表示使用 基于 SQL-92 语法的属性过滤器，能对消息属性做复杂条件判断（比如比较运算、IN、BETWEEN、IS NULL 等
    public static final String SQL92 = "SQL92";

    public static final String TAG = "TAG";

    public static boolean isTagType(String type) {
        if (type == null || "".equals(type) || TAG.equals(type)) {
            return true;
        }
        return false;
    }
}
