package org.apache.rocketmq.client.hook;

/**
 * 事务结束后的钩子函数
 */
public interface EndTransactionHook {
    String hookName();

    void endTransaction(final EndTransactionContext context);
}
