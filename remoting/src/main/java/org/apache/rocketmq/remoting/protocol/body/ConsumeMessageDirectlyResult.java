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

package org.apache.rocketmq.remoting.protocol.body;

import org.apache.rocketmq.remoting.protocol.RemotingSerializable;

public class ConsumeMessageDirectlyResult extends RemotingSerializable {
    private boolean order = false; // 标识消费是否需要保证顺序。
    private boolean autoCommit = true; // 是否自动提交
    private CMResult consumeResult; // 消费结果 成功、失败、充实
    private String remark; // 备注信息
    private long spentTimeMills; // 记录所花费的时间

    public boolean isOrder() {
        return order;
    }

    public void setOrder(boolean order) {
        this.order = order;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public CMResult getConsumeResult() {
        return consumeResult;
    }

    public void setConsumeResult(CMResult consumeResult) {
        this.consumeResult = consumeResult;
    }

    public long getSpentTimeMills() {
        return spentTimeMills;
    }

    public void setSpentTimeMills(long spentTimeMills) {
        this.spentTimeMills = spentTimeMills;
    }

    @Override
    public String toString() {
        return "ConsumeMessageDirectlyResult [order=" + order + ", autoCommit=" + autoCommit
            + ", consumeResult=" + consumeResult + ", remark=" + remark + ", spentTimeMills="
            + spentTimeMills + "]";
    }
}
