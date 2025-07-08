package org.apache.rocketmq.remoting.exception;

public class RemotingSendRequestException extends RemotingException {
    private static final long serialVersionUID = 5391285827332471674L;
    // cause = null ，复用多参数的初始化方法
    public RemotingSendRequestException(String addr) {
        this(addr, null);
    }

    public RemotingSendRequestException(String addr, Throwable cause) {
        super("send request to <" + addr + "> failed", cause);
    }
}
