package io.github.heisenberguwu.myrocketmq.common.resource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RocketMQResource {

    ResourceType value();

    String splitter() default "";
}
