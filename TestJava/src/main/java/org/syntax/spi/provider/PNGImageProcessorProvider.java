package org.syntax.spi.provider;


import org.syntax.spi.ImageProcessorService;

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

