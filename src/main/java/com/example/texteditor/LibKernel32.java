package com.example.texteditor;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.LongByReference;

/**
 * Interface to interact with Windows Kernel32 library using JNA (Java Native Access).
 * Provides methods and structures to manage console input/output operations.
 */
interface LibKernel32 extends Library {

    // Singleton instance of the Kernel32 library loaded via JNA.
    LibKernel32 INSTANCE = com.sun.jna.Native.load("kernel32", LibKernel32.class);
    
    static final long STD_INPUT_HANDLE = -10;
    static final long STD_OUTPUT_HANDLE = -11;
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

    /**
     * Structure representing console screen buffer information.
     * Contains details about the console's size, cursor position, attributes, and window dimensions.
     */
    @Structure.FieldOrder({"dwSize", "dwCursorPosition", "wAttributes", "srWindow", "dwMaximumWindowSize"})
    class ConsoleScreenBufferInfo extends Structure {
        public Coord      dwSize;
        public Coord      dwCursorPosition;
        public short      wAttributes;
        public SmallRect  srWindow;
        public Coord      dwMaximumWindowSize;
    }

    /**
     * Structure representing a 2D coordinate (x, y).
     * Used for positions like cursor location or buffer size.
     */
    @Structure.FieldOrder({"x", "y"})
    class Coord extends Structure {
        public short x, y;
    }

    /**
     * Structure representing a rectangular region in the console.
     * Defines the boundaries of the console window (left, top, right, bottom).
     */
     @Structure.FieldOrder({"left", "top", "right", "bottom"})
    class SmallRect extends Structure {
        public short left, top, right, bottom;
    }
    
    /**
     * Retrieves a handle to the specified standard device (input, output, or error).
     * @param nStdHandle The standard handle to retrieve (e.g., STD_INPUT_HANDLE, STD_OUTPUT_HANDLE).
     * @return A Pointer to the requested handle or INVALID_FILE_HANDLER if failed.
     */
    Pointer GetStdHandle(long nStdHandle);

    /**
     * Retrieves the current console mode for the specified handle.
     * @param hConsoleHandle Handle to the console input or output device.
     * @param lpMode Reference to store the current console mode flags.
     * @return True if successful, false otherwise.
     */
    boolean GetConsoleMode(Pointer hConsoleHandle, LongByReference lpMode);

    /**
     * Sets the console mode for the specified handle.
     * @param hConsoleHandle Handle to the console input or output device.
     * @param dwMode The new mode flags to set.
     * @return True if successful, false otherwise.
     */
    boolean SetConsoleMode(Pointer hConsoleHandle, long dwMode);

    /**
     * Retrieves information about the console screen buffer.
     * @param hConsoleOutput Handle to the console output device.
     * @param csbiInfo Structure to store the console screen buffer information.
     * @return True if successful, false otherwise.
     */
    boolean GetConsoleScreenBufferInfo(Pointer hConsoleOutput, ConsoleScreenBufferInfo csbiInfo);
}
