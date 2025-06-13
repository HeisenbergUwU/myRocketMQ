package io.github.heisenberguwu.myrocketmq;

import org.apache.commons.lang3.StringUtils;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        String cs1 = null;
        String cs2 = null;
        System.out.println(cs1 == cs2);
        boolean b = StringUtils.equalsIgnoreCase(cs1, cs2);
        System.out.println(b);

    }
}
