package com.example.texteditor;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class ByteBuffer {

    public final static int BYTEBUFFER_LEN = 32767;
    private byte[] byteBuffer;
    private int index;

    public ByteBuffer() {
        byteBuffer = new byte[BYTEBUFFER_LEN];
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
        index = 0;
    }

    public byte[] getBuffer() {
        return byteBuffer;
    }

    public byte[] getFilteredBuffer() {
        List<Byte> filtered = new ArrayList<>();
        for (byte b: byteBuffer) {
            if (b != 0) {
                filtered.add(b);
            }
        }

        byte[] result = new byte[filtered.size()];
        int index = 0;
        for (byte f: filtered) {
            result[index++] = f;
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (byte b: byteBuffer) {
            builder.append(b + " ");
        }
        return builder.toString();
    }
}
