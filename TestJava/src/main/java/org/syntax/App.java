package org.syntax;

/**
 * Hello world!
 *
 */
public class App 
{

    private static String clearNewLine(final String str) {
        /**
         * str.trim()：去掉输入字符串 str 两端的空白字符（包括空格、Tab、回车 \r 和换行 \n）。
         * 查找第一个回车符 \r 的位置：
         * 如果存在，返回从开头到该回车符前的子串（不包括回车和它后面的内容）。
         * 查找第一个换行符 \n 的位置：
         * 如果存在（并且没有回车），返回它之前的内容。
         * 如果既没有 \r 也没有 \n，返回已 trim() 处理的字符串。
         */
        String newString = str.trim();
        int index = newString.indexOf("\r");
        if (index != -1) {
            return newString.substring(0, index);
        }

        index = newString.indexOf("\n");
        if (index != -1) {
            return newString.substring(0, index);
        }

        return newString;
    }
    public static void main( String[] args )
    {
        System.out.println(clearNewLine("Hello\r\n world"));
    }
}
