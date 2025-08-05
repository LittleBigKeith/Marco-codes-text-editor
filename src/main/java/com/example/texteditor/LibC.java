package com.example.texteditor;

import java.util.Arrays;

import com.sun.jna.Library;
import com.sun.jna.Structure;

/**
 * Interface for native C library functions and structures used in terminal operations.
 */
interface LibC extends Library {
    
    static final int SYSTEM_OUT_FD = 1;
    
    // Terminal flags
    static final int ISIG = 0x00000080;
    static final int ICANON = 0x00000100;
    static final int ECHO = 0x00000008;
    static final int TCSAFLUSH = 2;
    static final int IXON = 0x00000200;
    static final int ICRNL = 0x00000100;
    static final int IEXTEN = 0x00000400;
    static final int OPOST = 0x00000001;
    static final int VMIN = 16;
    static final int VTIME = 17;
    static final long TIOCGWINSZ = Ioccom._IOR('t', 104);
    LibC INSTANCE = com.sun.jna.Native.load("c", LibC.class);

    /**
     * Represents the terminal window size structure.
     */
    @Structure.FieldOrder({"ws_row", "ws_col", "ws_xpixel", "ws_ypixel"})
    class Winsize extends Structure {
        public short ws_row, ws_col, ws_xpixel, ws_ypixel;
        
        @Override
        public String toString() {
            return "Winsize{" +
                   "ws_row=" + ws_row +
                   ", ws_col=" + ws_col +
                   ", ws_xpixel=" + ws_xpixel +
                   ", ws_ypixel=" + ws_ypixel +
                   "}";
        }
    }

    /**
     * Represents the terminal attributes structure.
     */
    @Structure.FieldOrder({"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc", "c_ispeed", "c_ospeed"})
    class Termios extends Structure {
        public long c_iflag, c_oflag, c_cflag, c_lflag;
        public char[] c_cc = new char[20];
        public long c_ispeed, c_ospeed;

        /**
         * Creates a deep copy of a Termios instance.
         *
         * @param source The Termios instance to copy.
         * @return A new Termios instance with copied values.
         */
        public static Termios t_copy(Termios t) {
            Termios copy = new Termios();
            copy.c_iflag = t.c_iflag;
            copy.c_oflag = t.c_oflag;
            copy.c_cflag = t.c_cflag;
            copy.c_lflag = t.c_lflag;
            copy.c_cc = t.c_cc.clone();
            copy.c_ispeed = t.c_ispeed;
            copy.c_ospeed = t.c_ospeed;
            return copy;
        }

        @Override
        public String toString() {
            return "Termios{" +
                   "c_iflag=" + c_iflag +
                   ", c_oflag=" + c_oflag +
                   ", c_cflag=" + c_cflag +
                   ", c_lflag=" + c_lflag +
                   ", c_cc=" + Arrays.toString(c_cc) +
                   ", c_ispeed=" + c_ispeed +
                   ", c_ospeed=" + c_ospeed +
                   "}";
        }
    }

    /**
     * Gets the terminal attributes for a file descriptor.
     */
    int tcgetattr(int fildes, Termios termios);

    /**
     * Sets the terminal attributes for a file descriptor.
     */
    int tcsetattr(int fildes, int optional_actions, Termios termios);

    /**
     * Performs an ioctl operation to get window size.
     */
    int ioctl(int fd, long request, Winsize winSize);
}