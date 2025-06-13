package io.github.heisenberguwu.myrocketmq.action;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RocketMQ {
    int value();


}
