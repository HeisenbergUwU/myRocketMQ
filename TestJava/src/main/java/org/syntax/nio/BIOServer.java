package org.syntax.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class BIOServer {


    /**
     * 需要手动处理已经读取之后的通道，
     * <p>
     * java.nio.channels.SocketChannel[connected local=/127.0.0.1:8080 remote=/127.0.0.1:53992]
     * /127.0.0.1:53992 < - > /127.0.0.1:8080
     * -1170105035
     * java.nio.channels.SocketChannel[connected local=/127.0.0.1:8080 remote=/127.0.0.1:53992]
     * /127.0.0.1:53992 < - > /127.0.0.1:8080
     * java.nio.channels.SocketChannel[connected local=/127.0.0.1:8080 remote=/127.0.0.1:53996]
     * /127.0.0.1:53996 < - > /127.0.0.1:8080
     * -1170105035
     */

    public static void main(String[] args) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(8080));
        ByteBuffer read = ByteBuffer.allocate(1024);
        List<SocketChannel> channels = new ArrayList<>();
        while (true) {
            SocketChannel sc = ssc.accept();
            channels.add(sc);
            for (SocketChannel channel : channels) {
                System.out.println(channel);
                int read1 = channel.read(read);
                // == -1 的时候，说明channel 已经关闭了。
                if (read1 == -1) {
                    System.out.println("GG" + channels.size());
                    continue;
                }
                read.flip();
                if (read.hasRemaining()) {
                    int anInt = read.getInt();
                    System.out.println(anInt);
                }
                read.clear();
                read.putInt(115414);
                read.flip();
                channel.write(read);
                read.clear();
            }
        }
    }
}
