package io.github.heisenberguwu.myrocketmq.chain;


public interface Handler<T, R> {

    R handle(T t, HandlerChain<T, R> chain);
}
