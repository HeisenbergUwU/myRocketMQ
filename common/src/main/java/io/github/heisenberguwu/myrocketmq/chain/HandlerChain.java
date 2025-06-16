package io.github.heisenberguwu.myrocketmq.chain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HandlerChain<T, R> {
    private List<Handler<T, R>> handlers;
    private Iterator<Handler<T, R>> iterator;

    public static <T, R> HandlerChain<T, R> create() {
        // 静态方法需要自己显示的声明泛型
        return new HandlerChain<>();
    }

    public HandlerChain<T, R> addNext(Handler<T, R> handler) {
        // 实例方法不需要显示声明
        if (this.handlers == null) {
            this.handlers = new ArrayList<>();
        }
        this.handlers.add(handler);
        return this;
    }

    public R handle(T t) {
        if (iterator == null) {
            iterator = handlers.iterator();
        }
        if (iterator.hasNext()) {
            Handler<T, R> handler = iterator.next();
            return handler.handle(t, this);
        }
        return null;
    }
}