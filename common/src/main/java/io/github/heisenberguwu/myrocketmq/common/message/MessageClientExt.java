package io.github.heisenberguwu.myrocketmq.common.message;

public class MessageClientExt extends MessageExt {

    public String getOffsetMsgId() {
        return super.getMsgId();
    }

    public void setOffsetMsgId(String offsetMsgId) {
        super.setMsgId(offsetMsgId);
    }


    @Override
    public String getMsgId() {
        String uniqID = MessageClientID
        return super.getMsgId();
    }
}
