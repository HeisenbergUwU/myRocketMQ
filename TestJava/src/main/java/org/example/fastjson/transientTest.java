package org.example.fastjson;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

public class transientTest {

    static class A {
        public transient int age;
        public String name;
        private boolean sex;

        public A(int age, String name, boolean sex) {
            this.age = age;
            this.name = name;
            this.sex = sex;
        }
    }

    public static void main(String[] args) {
        A q = new A(20, "Q", true);

        JSONObject from = JSONObject.from(q);

        System.out.println(from);

    }
}
