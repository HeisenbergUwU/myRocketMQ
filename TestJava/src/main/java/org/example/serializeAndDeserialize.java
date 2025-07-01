package org.example;

import java.io.*;
import java.util.Arrays;

public class serializeAndDeserialize {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Dog husky = new Dog("Husky");
        ByteArrayOutputStream bufferStream = new ByteArrayOutputStream(4096);
        ObjectOutputStream out = new ObjectOutputStream(bufferStream);
        out.writeObject(husky);
        byte[] huskyBytes = bufferStream.toByteArray();
        System.out.println(Arrays.toString(huskyBytes));

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(huskyBytes));
        Object o = in.readObject();
        Dog dog = (Dog) o;
        System.out.println(dog.name);
    }

    static class Dog implements Serializable {

        private static final long serialVersionUID = 1L;
        public String name;

        public Dog(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
