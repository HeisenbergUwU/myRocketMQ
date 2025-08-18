package org.syntax.netty;

import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;

// 定义两个简单的 Outbound Handler
class OutboundA extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        System.out.println("OutboundA processed: " + msg);
    }
}

class OutboundB extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        System.out.println("OutboundB processed: " + msg);
    }
}


// 用于测试的 Handler，根据 flag 选择写方式
class MyHandler extends ChannelOutboundHandlerAdapter {
    private final boolean useCtx;

    MyHandler(boolean useCtx) {
        this.useCtx = useCtx;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        String msg = useCtx ? "from ctx.write" : "from channel.write";
        if (useCtx) {
            ctx.write(msg);
            ctx.flush();
        } else {
            ctx.channel().write(msg);
            ctx.channel().flush();
        }
    }
}

public class testWrite {
    public static void main(String[] args) {
        System.out.println("=== Test with ctx.write() ===");
        EmbeddedChannel ch1 = new EmbeddedChannel(new OutboundA(), new MyHandler(true), new OutboundB());
        ch1.finish(); // 确保所有事件执行完毕

        System.out.println("\n=== Test with channel.write() ===");
        EmbeddedChannel ch2 = new EmbeddedChannel(new OutboundA(), new MyHandler(false), new OutboundB());
        ch2.finish();
    }
}
