package org.apache.rocketmq.remoting.netty;

import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import com.google.common.base.Stopwatch;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

/**
 * NettyDecoder 继承自 Netty 的 LengthFieldBasedFrameDecoder，
 * 目的是 解决 TCP 粘包/拆包问题，并将读取完整的字节框架进一步 解析成 RocketMQ 的命令对象 RemotingCommand。
 * <p>
 * - io.netty.handler.codec.FixedLengthFrameDecoder; 固定长度编码器，少于等待、多则截断
 * - io.netty.handler.codec.LineBasedFrameDecoder; 分隔符解码器
 * - io.netty.handler.codec.LengthFieldBasedFrameDecoder; 长度解码器，可以读取4bytes，信息头长度，在读取剩余的信息体，配合 LengthFieldPrepender 自动输出前添加长度的字段。
 */
public class NettyDecoder extends LengthFieldBasedFrameDecoder {
    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_REMOTING_NAME);

    private static final int FRAME_MAX_LENGTH = Integer.parseInt(System.getProperty("com.rocketmq.remoting.frameMaxLength", "16777216"));

    /**
     * /**
     * * Creates a new instance.
     * *
     * * @param maxFrameLength
     * *        the maximum length of the frame.  If the length of the frame is
     * *        greater than this value, {@link TooLongFrameException} will be
     * *        thrown.
     * * @param lengthFieldOffset
     * *        the offset of the length field
     * * @param lengthFieldLength
     * *        the length of the length field
     * * @param lengthAdjustment
     * *        the compensation value to add to the value of the length field
     * * @param initialBytesToStrip
     * *        the number of first bytes to strip out from the decoded frame
     */
    public NettyDecoder() {
        /**
         * 在 NettyDecoder 的构造函数中，调用了 super(FRAME_MAX_LENGTH, 0, 4, 0, 4)，这意味着：
         * FRAME_MAX_LENGTH：指定帧的最大长度。
         * 0：长度字段从消息的第一个字节开始（即偏移量为 0）。
         * 4：长度字段占用 4 个字节。
         * 0：没有补偿值，即长度字段表示的长度与实际数据长度相同。
         * 4：在解码帧时，跳过前 4 个字节，通常用于丢弃协议头部。
         */
        super(FRAME_MAX_LENGTH, 0, 4, 0, 4);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        Stopwatch timer = Stopwatch.createStarted();

        try {
            // 本类型解码器进行解码，在这里已经丢弃了0-4byte的长度信息。
            // 因为我初始化的时候已经使用 了super(...)，因此原始方法是我想要集成的，这里的decode 是一个序列化处理过程。
            frame = (ByteBuf) super.decode(ctx, in);
            if (null == frame) {
                return null;
            }
            RemotingCommand cmd = RemotingCommand.decode(frame);
            cmd.setProcessTimer(timer);
            return cmd;
        } catch (Exception e) {
            log.error("decode exception, " + RemotingHelper.parseChannelRemoteAddr(ctx.channel()), e);
            RemotingHelper.closeChannel(ctx.channel());
        } finally {
            if (null != frame) {
                frame.release();
            }
        }
        return null;
    }
}