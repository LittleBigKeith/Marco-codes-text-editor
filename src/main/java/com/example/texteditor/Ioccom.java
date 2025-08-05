package com.example.texteditor;

/**
 * Provides constants and utility methods for terminal I/O control (ioctl) operations.
 */
public class Ioccom {
    
    private static final int IOC_OUT = 0x40000000;
    private static final int IOCPARM_MASK = 0x00001fff;
    private static final int SIZEOF_T = 8;

    /**
     * Constructs an ioctl read command for a given group and number.
     *
     * @param group The group identifier (e.g., 't' for terminal).
     * @param num   The command number.
     * @return The constructed ioctl command.
     */
    static long _IOR(char g, int n) {
        return _IOC(IOC_OUT, g, n, SIZEOF_T);
    }

    /**
     * Constructs a generic ioctl command.
     *
     * @param inout The I/O direction (e.g., IOC_OUT).
     * @param group The group identifier.
     * @param num   The command number.
     * @param len   The length of the data structure.
     * @return The constructed ioctl command.
     */
    private static long _IOC(int inout, char group, int num, int len) {
        return inout | (len & IOCPARM_MASK) << 16 | (group) << 8 | num;
    }
}
