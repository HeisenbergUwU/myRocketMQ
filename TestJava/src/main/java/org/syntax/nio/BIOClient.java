package org.syntax.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BIOClient {

    private static Random random = new Random(42);

    public static void main(String[] args) throws IOException {
        justConnect();
    }

    private static void multiCall(int num) {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            threads.add(new Thread(() -> {
                try {
                    send();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).start();
        }

        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void send() throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress(8080));
        int i = random.nextInt();
        ByteBuffer byteBuffer = ByteBuffer.allocate(4).putInt(i);
        byteBuffer.flip();
        sc.write(byteBuffer);
        byteBuffer.clear();
        sc.read(byteBuffer);
        byteBuffer.flip();
        System.out.println(byteBuffer.getInt());
        sc.close();
    }


    public static void justConnect() throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress(8080)); // 会出发 accept 事件。
        sc.finishConnect(); // 判断是否完成了 连接
    }
}
