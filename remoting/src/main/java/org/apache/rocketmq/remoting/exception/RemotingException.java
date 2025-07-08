package org.apache.rocketmq.remoting.exception;

/**
 * Exception 的子类（不包括 RuntimeException）是 受检异常，需编译期确认处理
 *
 * 若你扩展 RuntimeException，就创建了不受检异常（Unchecked）；调用者无需处理，会在 runtime 抛出
 *
 * RuntimeException 及其子类不会在编译期间被要求 throw 或 catch。
 */
public class RemotingException extends Exception {
    private static final long serialVersionUID = -5690687334570505110L;

    public RemotingException(String message) {
        super(message);
    }

    public RemotingException(String message, Throwable cause) {
        super(message, cause);
    }
}
