package org.syntax.spi.provider;

import org.syntax.spi.ImageProcessorService;

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