package com.example.texteditor;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;

public class WindowsTerminal extends IOHandler implements Terminal {

    private static Pointer outHandle;
    private static Pointer inHandle;
    private static LongByReference dwOriginalOutMode;
    private static LongByReference dwOriginalInMode;
    private static LibKernel32.ConsoleScreenBufferInfo bufferInfo;

    @Override
    public void enableRawMode() {
        outHandle = LibKernel32.INSTANCE.GetStdHandle(LibKernel32.STD_OUTPUT_HANDLE);
        if (outHandle.getLong(0) == LibKernel32.INVALID_FILE_HANDLER) {
            System.err.println("An error occured while getting output file handler: " + LibKernel32.INVALID_FILE_HANDLER);
            System.exit(-1);
        }

        inHandle = LibKernel32.INSTANCE.GetStdHandle(LibKernel32.STD_INPUT_HANDLE);
        if (outHandle.getLong(0) == LibKernel32.INVALID_FILE_HANDLER) {
            System.err.println("An error occured while getting input file handler: " + LibKernel32.INVALID_FILE_HANDLER);
            System.exit(-1);
        }

        dwOriginalOutMode = new LongByReference(0);
        if (!LibKernel32.INSTANCE.GetConsoleMode(outHandle, dwOriginalOutMode)) {
            System.err.println("An error occured while getting output console mode");
            System.exit(-1);
        }

        dwOriginalInMode = new LongByReference(0);;
        if (!LibKernel32.INSTANCE.GetConsoleMode(inHandle, dwOriginalInMode)) {
            System.err.println("An error occured while getting input console mode");
            System.exit(-1);
        }

        long dwRequestedOutModes = LibKernel32.ENABLE_PROCESSED_OUTPUT | LibKernel32.ENABLE_VIRTUAL_TERMINAL_PROCESSING;
        long dwRequestedInModes = 
            ~(
                LibKernel32.ENABLE_ECHO_INPUT
              | LibKernel32.ENABLE_LINE_INPUT
              | LibKernel32.ENABLE_MOUSE_INPUT
              | LibKernel32.ENABLE_PROCESSED_INPUT
              | LibKernel32.ENABLE_WINDOW_INPUT
            )
            | LibKernel32.ENABLE_VIRTUAL_TERMINAL_INPUT;
        
        long dwOutMode = dwOriginalOutMode.getValue() | dwRequestedOutModes;
        if (!LibKernel32.INSTANCE.SetConsoleMode(outHandle, dwOutMode))
        {
            // Failed to set any VT mode, can't do anything here.
            System.err.println("An error occured while setting output console mode");
            System.exit(-1);
        }

        long dwInMode = dwOriginalInMode.getValue() | dwRequestedInModes;
        if (!LibKernel32.INSTANCE.SetConsoleMode(inHandle, dwInMode))
        {
            // Failed to set VT input mode, can't do anything here.
            System.err.println("An error occured while setting input console mode");
            System.exit(-1);
        }
    }

    @Override
    public void disableRawMode() {
        if (!LibKernel32.INSTANCE.SetConsoleMode(outHandle, dwOriginalOutMode.getValue())) {
            System.err.println("An error occured while restoring output console mode");
            System.exit(-1);
        } // Restore original terminal attributes before exiting
        
        if (!LibKernel32.INSTANCE.SetConsoleMode(inHandle, dwOriginalInMode.getValue())) {
            System.err.println("An error occured while restoring input console mode");
            System.exit(-1);
        }; // Restore original terminal attributes before exiting
    }

    @Override
    public void initWindowSize() {
        getBufferInfo();
        setRows(bufferInfo.srWindow.bottom - bufferInfo.srWindow.top - 1);
        setColumns(bufferInfo.srWindow.right - bufferInfo.srWindow.left + 1);
    }

    private static void getBufferInfo() {
        outHandle = LibKernel32.INSTANCE.GetStdHandle(LibKernel32.STD_OUTPUT_HANDLE);
        if (outHandle.getLong(0) == LibKernel32.INVALID_FILE_HANDLER) {
            System.err.println("An error occured while getting output file handler: " + LibKernel32.INVALID_FILE_HANDLER);
            System.exit(-1);
        }
        LibKernel32.INSTANCE.GetConsoleScreenBufferInfo(outHandle, bufferInfo);
    }

    @Override
    public void exit() {
        System.out.print("\033[2J");    // Clear the screen
        System.out.print("\033[H");     // Move cursor to top-left
        disableRawMode();
        System.exit(0);
    }
    
}
