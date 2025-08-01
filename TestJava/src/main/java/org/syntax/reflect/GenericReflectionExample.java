package org.syntax.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class GenericReflectionExample {

    // 一个带泛型参数的父类
    static class Parent<T> {
    }

    // 继承并且指定 T 为 String
    static class Child extends Parent<String> {
        // 带有泛型参数的字段
        public List<String> stringList;

        // 带泛型参数的方法
        public List<Integer> getIntegerList() {
            return Arrays.asList(1, 2, 3, 4);
        }
    }

    public static void main(String[] args) {
        Class<Child> clz = Child.class;

        // 1. 获取父类的泛型参数
        Type superType = clz.getGenericSuperclass();
        if (superType instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) superType).getActualTypeArguments();
            String typeName = superType.getTypeName();
            Type ownerType = ((ParameterizedType) superType).getOwnerType();
            Type rawType = ((ParameterizedType) superType).getRawType();
            System.out.println(ownerType.getTypeName());
            System.out.println(rawType.getTypeName());
            System.out.println("类型是：" + typeName);
            System.out.println("Parent<T> 中 T 类型是: " + actualTypeArguments[0].getTypeName());
            /**
             * org.example.reflect.GenericReflectionExample
             * org.example.reflect.GenericReflectionExample$Parent
             * 类型是：org.example.reflect.GenericReflectionExample$Parent<java.lang.String>
             * Parent<T> 中 T 类型是: java.lang.String
             */
        }

        // 2. 获取字段 stringList 的泛型参数
        try {
            Field field = clz.getField("stringList");
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                Type[] fieldArgs = ((ParameterizedType) genericType).getActualTypeArguments();
                System.out.println("字段 List<String> 的泛型类型是: " + fieldArgs[0].getTypeName());
                // 输出: 字段 List<String> 的泛型类型是: java.lang.String
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        // 3. 获取方法返回值的泛型参数
        try {
            Method m = clz.getMethod("getIntegerList");
            Type returnType = m.getGenericReturnType();
            if (returnType instanceof ParameterizedType) {
                Type[] returnArgs = ((ParameterizedType) returnType).getActualTypeArguments();
                System.out.println("方法返回 List<Integer> 的泛型类型是: " + returnArgs[0].getTypeName());
                // 输出: 方法返回 List<Integer> 的泛型类型是: java.lang.Integer
            }

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
