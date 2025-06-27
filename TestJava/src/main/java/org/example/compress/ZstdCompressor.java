package org.example.compress;

import java.io.IOException;

public class ZstdCompressor implements ZstdCompressor.Compressor {
    @Override
    public byte[] compress(byte[] src, int level) throws IOException {
        return new byte[0];
    }

    @Override
    public byte[] decompress(byte[] src) throws IOException {
        return new byte[0];
    }

    public interface Compressor {

        /**
         * Compress message by different compressor.
         *
         * @param src   bytes ready to compress
         * @param level compression level used to balance compression rate and time consumption
         * @return compressed byte data
         * @throws IOException
         */
        byte[] compress(byte[] src, int level) throws IOException;

        /**
         * Decompress message by different compressor.
         *
         * @param src bytes ready to decompress
         * @return decompressed byte data
         * @throws IOException
         */
        byte[] decompress(byte[] src) throws IOException;
    }

}