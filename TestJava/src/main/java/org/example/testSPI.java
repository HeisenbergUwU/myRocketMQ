package org.example;

import org.example.spi.ImageProcessorService;

import java.util.ServiceLoader;

public class testSPI {
    public static void main(String[] args) {
        ServiceLoader<ImageProcessorService> load = ServiceLoader.load(ImageProcessorService.class);
        for (ImageProcessorService proc : load) {
            System.out.println("Found processor: " + proc.getType());
            proc.process();
            System.out.println("---");
        }
    }
}
