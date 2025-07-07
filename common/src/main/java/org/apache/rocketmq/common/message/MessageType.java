package org.apache.rocketmq.common.message;

public enum MessageType {
    Normal_Msg("Normal"), // 普通消息
    Trans_Msg_Half("Trans"), // 消息事务一阶段，半消息
    Trans_msg_Commit("TransCommit"), // 消息事务二阶段，已经可以消费
    Delay_Msg("Delay"), // 延迟消息，不会被立刻消费
    Order_Msg("Order"); // 顺序消息

    private final String shortName;

    MessageType(String shortName) {
        this.shortName = shortName;
    }

    public String getShortName() {
        return shortName;
    }

    public static MessageType getByShortName(String shortName) {
        for (MessageType msgType : MessageType.values()) {
            if (msgType.getShortName().equals(shortName)) {
                return msgType;
            }
        }
        return Normal_Msg;
    }
}
