package com.example.texteditor;

public class Ioccom {
    
    private static final int IOC_OUT = 0x40000000;
    private static final int IOCPARM_MASK = 0x00001fff;
    private static final int SIZEOF_T = 8;

    static long _IOR(char g, int n) {
        return _IOC(IOC_OUT, g, n, SIZEOF_T);
    }

    private static long _IOC(int inout, char group, int num, int len) {
        return inout | (len & IOCPARM_MASK) << 16 | (group) << 8 | num;
    }
}
