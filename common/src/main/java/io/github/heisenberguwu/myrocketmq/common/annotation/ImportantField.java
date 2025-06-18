package io.github.heisenberguwu.myrocketmq.common.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD,ElementType.PARAMETER,ElementType.LOCAL_VARIABLE})
public @interface ImportantField {
    /**
     * 这段代码定义了一个名为 @ImportantField 的自定义注解（Annotation），
     * 它的主要作用是 标记代码中重要的字段（Field）、方法（Method）、参数（Parameter）或
     * 局部变量（Local Variable），
     * 以便在开发、测试或运行时进行特殊处理（例如日志记录、权限校验、数据验证等）。下面详细解析它的设计意图和可能的用途：
     */
}
