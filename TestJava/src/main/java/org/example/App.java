package org.example;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        ByteBuffer allocate = ByteBuffer.allocate(10);
        allocate.flip();
        allocate.putLong(17L);
        System.out.println(allocate.position());
        System.out.println(allocate.limit());
        allocate.limit(0);
        allocate.getLong();
        System.out.println( "Hello World!" );
    }
}
