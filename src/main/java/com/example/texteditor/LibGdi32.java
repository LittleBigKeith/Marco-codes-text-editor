package com.example.texteditor;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

interface LibGdi32 extends Library {

    LibGdi32 INSTANCE = com.sun.jna.Native.load("gdi32", LibGdi32.class);

    boolean GetCharWidth32A(Pointer hdc, int iFirstChar, int iLastChar, IntByReference lpBuffer);
}
