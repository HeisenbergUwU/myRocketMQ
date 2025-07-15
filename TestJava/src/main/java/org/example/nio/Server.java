package org.example.nio;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {

    public static void main(String[] args) throws IOException {
        bossLoop();
    }

    private static final ExecutorService workers = Executors.newFixedThreadPool(8);

    /**
     * 用来处理 SELECTOR
     */
    public static void bossLoop() throws IOException {
        // ServerSocketChannel 用来托管多个 SocketChannel
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(8080));
        ssc.configureBlocking(false); // non block
        Selector selector = Selector.open();
//         这一行就是看看API，没啥用。。 ServerSocket 只能处理 SelectionKey.OP_ACCEPT
//        ServerSocket socket = ssc.socket(); // 拿到服务器绑定的socket，因为是ServerSocket所以不能写
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        while (!Thread.currentThread().isInterrupted()) {
            if (selector.select() == 0) continue;// 阻塞，一直等到有事件来，然后就分发事件
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
            while (selectionKeyIterator.hasNext()) {
                SelectionKey next = selectionKeyIterator.next();
                selectionKeyIterator.remove(); // 先从 ACCEPT 事件中列表中删除，也就是清除这个Socket绑定的状态。
                if (next.isAcceptable()) {
                    SocketChannel client = ssc.accept();
                    client.configureBlocking(false);
                    System.out.println("Accepted " + client.getRemoteAddress());
                    workers.submit(new WorkerHandler(client));
                }
            }


        }
        ssc.close();
    }

    // Worker：简单处理 HTTP 请求
    static class WorkerHandler implements Runnable {
        private final SocketChannel client;

        WorkerHandler(SocketChannel client) {
            this.client = client;
        }

        @Override
        public void run() {
            try (SocketChannel ch = client) {
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                ch.read(buffer);
                buffer.flip();

                String request = new String(buffer.array(), 0, buffer.limit());
                System.out.println("Request: \n" + request);

                // 简单响应
                String httpResp = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: 12\r\n" +
                        "\r\n" +
                        "Hello World";
                ByteBuffer respBuf = ByteBuffer.wrap(httpResp.getBytes());
                while (respBuf.hasRemaining()) {
                    ch.write(respBuf);
                }
                System.out.println("Response sent to " + ch.getRemoteAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
