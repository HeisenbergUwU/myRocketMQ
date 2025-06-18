package io.github.heisenberguwu.myrocketmq.common.action;

import io.github.heisenberguwu.myrocketmq.common.resource.ResourceType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RocketMQAction {
    int value(); // 操作的值

    ResourceType resource() default ResourceType.UNKNOWN; // 操作的资源

    Action[] action(); // 操作的动作

}
