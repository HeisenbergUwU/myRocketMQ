package org.example;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 *
 */
public class testByteBuffer {
    public static void main(String[] args) {
        // 初始化缓冲区 pos 0; limit : capacity; capacity 是我设定的 10
        ByteBuffer allocate = ByteBuffer.allocate(10);
        System.out.println("=== BB 初始化");
        System.out.println("= BB position: " + allocate.position());
        System.out.println("= BB limit: " + allocate.limit());
        System.out.println("= BB capacity: " + allocate.capacity());
        // 写入数据 position 会根据写入的数据量增加；limit 保持不变仍然是 capacity
        System.out.println("=== BB 写入一个 Long【8 Byte】");
        allocate.putLong(115414115414L);
        System.out.println("= BB position: " + allocate.position());
        System.out.println("= BB limit: " + allocate.limit());
        System.out.println("= BB capacity: " + allocate.capacity());
        // 切换读模式，pos:0 , limit = 当前pos 【刚才写到了8】
        allocate.flip();
        System.out.println("=== BB 切换读模式");
        System.out.println("= BB position: " + allocate.position());
        System.out.println("= BB limit: " + allocate.limit());
        System.out.println("= BB capacity: " + allocate.capacity());
        // 只读取了4个字节
        int anInt = allocate.getInt();
        System.out.println("=== BB 读取了一个Int【4Byte】" + anInt);
        System.out.println("= BB position: " + allocate.position());
        System.out.println("= BB limit: " + allocate.limit());
        System.out.println("= BB capacity: " + allocate.capacity());
        // 切换写模式 clear
        allocate.clear();
        System.out.println("=== BB clear 切换写模式");
        System.out.println("= BB position: " + allocate.position());
        System.out.println("= BB limit: " + allocate.limit());
        System.out.println("= BB capacity: " + allocate.capacity());
        System.out.println("= 【DEBUG】位置切换了，但是并没有 Zero Memory。");
        System.out.println("= " + Arrays.toString(allocate.array()));

        // 现在我们还原读取之前的现场
        /*
            = BB position: 4
            = BB limit: 8
            = BB capacity: 10
         */
        System.out.println("=== 还原读取之前的现场\n" +
                "            = BB position: 4\n" +
                "            = BB limit: 8\n" +
                "            = BB capacity: 10");
        allocate.position(4);
        allocate.limit(8);
        allocate.limit(10);
        System.out.println("= " + Arrays.toString(allocate.array()));

        allocate.flip();
        System.out.println("=== BB flip 切换读模式，AG");
        System.out.println("= BB position: " + allocate.position());
        System.out.println("= BB limit: " + allocate.limit());
        System.out.println("= BB capacity: " + allocate.capacity());
        System.out.println("= " + Arrays.toString(allocate.array()));

        System.out.println("=== BB compact 切换写模式");
        ByteBuffer compact = allocate.compact();
        System.out.println("= BB position: " + allocate.position());
        System.out.println("= BB limit: " + allocate.limit());
        System.out.println("= BB capacity: " + allocate.capacity());
        System.out.println("= " + Arrays.toString(allocate.array()));
    }
}
