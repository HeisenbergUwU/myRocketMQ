package org.example.spi.provider;

import org.example.spi.ImageProcessorService;

public class JPGImageProcessorProvider implements ImageProcessorService {
    @Override
    public String getType() {
        return "JPG";
    }

    @Override
    public void process() {
        System.out.println("Processing JPG image");
    }
}