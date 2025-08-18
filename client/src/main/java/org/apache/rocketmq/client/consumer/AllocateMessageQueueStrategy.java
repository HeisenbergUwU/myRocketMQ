/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.client.consumer;

import java.util.List;

import org.apache.rocketmq.common.message.MessageQueue;

/**
 * Strategy Algorithm for message allocating between consumers
 * rebalance包主要是用来将 一个Topic 下的多个MessageQueue合理分配给一个消费者组的多个消费者实例。
 */
public interface AllocateMessageQueueStrategy {

    /**
     * Allocating by consumer id - 分配消息队列
     *
     * @param consumerGroup current consumer group - 消费者组名称
     * @param currentCID    current consumer id - 当前消费者的实例ID
     * @param mqAll         message queue set in current topic - 当前消费者组下的所有消费者实例的 Consumer ID 列表
     * @param cidAll        consumer set in current consumer group - 当前消费者组下的所有消费者实例 Consumer ID 列表。
     * @return The allocate result of given strategy
     */
    List<MessageQueue> allocate(
            final String consumerGroup,
            final String currentCID,
            final List<MessageQueue> mqAll,
            final List<String> cidAll
    );

    /**
     * Algorithm name
     *
     * @return The strategy name - 策略名称
     */
    String getName();
}
