package io.github.heisenberguwu.myrocketmq.common.message;

import io.github.heisenberguwu.myrocketmq.common.UtilAll;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageClientIDSetter {
    private static final int LEN;
    private static final char[] FIX_STRING;
    private static final AtomicInteger COUNTER;
    private static long startTime;
    private static long nextStartTime;

    static {
        byte[] ip;
        try{
            ip= UtilAll.getIP();
        } catch (Exception e)
        {
            ip = createFakeIp();
        }
        LEN = ip.length + 2 + 4 + 4 + 2;
        ByteBuffer tempBuffer = ByteBuffer.allocate(ip.length + 2 + 4);
        tempBuffer.put(ip);
        tempBuffer.putShort((short)UtilAll.getPid());
    }
}
    