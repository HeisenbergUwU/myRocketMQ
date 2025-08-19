package org.apache.rocketmq.client;

import java.util.List;

import org.apache.rocketmq.common.message.MessageExt;

public class QueryResult {
    private final long indexLastUpdateTimestamp;
    private final List<MessageExt> messageList;

    public QueryResult(long indexLastUpdateTimestamp, List<MessageExt> messageList) {
        this.indexLastUpdateTimestamp = indexLastUpdateTimestamp;
        this.messageList = messageList;
    }

    public long getIndexLastUpdateTimestamp() {
        return indexLastUpdateTimestamp;
    }

    public List<MessageExt> getMessageList() {
        return messageList;
    }

    @Override
    public String toString() {
        return "QueryResult [indexLastUpdateTimestamp=" + indexLastUpdateTimestamp + ", messageList="
                + messageList + "]";
    }
}
