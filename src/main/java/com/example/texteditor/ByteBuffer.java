package com.example.texteditor;

import java.util.Arrays;

public class ByteBuffer {

    private byte[] byteBuffer;
    private int index;

    public ByteBuffer(int maxSize) {
        byteBuffer = new byte[maxSize];
        index = 0;
    }

    public int next() {
        if (index >= byteBuffer.length) {
            return -1;
        }
        int nextByte = byteBuffer[index];
        index += 1;
        return nextByte;
    }

    public int skip() {
        index += 1;
        return next();
    }

    public void clear() {
        Arrays.fill(byteBuffer, (byte) 0);
    }

    public byte[] getBuffer() {
        return byteBuffer;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for(int i = 0 ; i < byteBuffer.length; i++) {
            builder.append(byteBuffer[i] + " ");
        }
        return builder.toString();
    }
}
