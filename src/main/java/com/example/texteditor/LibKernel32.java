package com.example.texteditor;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.LongByReference;


interface LibKernel32 extends Library {

    LibKernel32 INSTANCE = com.sun.jna.Native.load("kernel32", LibKernel32.class);
    
    static final long STD_INPUT_HANDLE = (long) Math.pow(2, 32) - 10;
    static final long STD_OUTPUT_HANDLE = (long) Math.pow(2, 32) - 11;
    static final long INVALID_FILE_HANDLER = -1;
    static final long ENABLE_ECHO_INPUT = 0x0004;
    static final long ENABLE_INSERT_MODE = 0x0020;
    static final long ENABLE_LINE_INPUT = 0x0002;
    static final long ENABLE_MOUSE_INPUT = 0x0010;
    static final long ENABLE_PROCESSED_INPUT = 0x0001;
    static final long ENABLE_WINDOW_INPUT = 0x0008;
    static final long ENABLE_VIRTUAL_TERMINAL_INPUT = 0x0200;
    static final long ENABLE_PROCESSED_OUTPUT = 0x0001;
    static final long ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

    @Structure.FieldOrder({"dwSize", "dwCursorPosition", "wAttrivutes", "srWindow", "dwMaximumWindowSize"})
    class ConsoleScreenBufferInfo extends Structure {
        Coord      dwSize;
        Coord      dwCursorPosition;
        short      wAttributes;
        SmallRect  srWindow;
        Coord      dwMaximumWindowSize;
    }

    @Structure.FieldOrder({"x", "y"})
    class Coord extends Structure {
        short x, y;
    }

     @Structure.FieldOrder({"left", "top", "right", "bottom"})
    class SmallRect extends Structure {
        short left, top, right, bottom;
    }
    
    Pointer GetStdHandle(long nStdHandle);
    boolean GetConsoleMode(Pointer hConsoleHandle, LongByReference lpMode);
    boolean SetConsoleMode(Pointer hConsoleHandle, long dwMode);
    boolean GetConsoleScreenBufferInfo(Pointer hConsoleOutput, ConsoleScreenBufferInfo csbiInfo);
}
