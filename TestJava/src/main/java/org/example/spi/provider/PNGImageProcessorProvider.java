package org.example.spi.provider;


import org.example.spi.ImageProcessorService;

public class PNGImageProcessorProvider implements ImageProcessorService {
    @Override
    public String getType() {
        return "PNG";
    }

    @Override
    public void process() {
        System.out.println("Processing PNG image");
    }
}

