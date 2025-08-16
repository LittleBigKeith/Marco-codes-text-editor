package com.example.texteditor;

import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;

/**
 * Implements a terminal interface for Windows consoles using the Kernel32 library.
 * Manages raw mode configuration, window size initialization, and terminal cleanup.
 */
public class WindowsTerminal extends IOHandler implements Terminal {

    // Static handles for console input and output.
    private static Pointer outHandle;
    private static Pointer inHandle;

    // Stores the original console modes to restore them later.
    private static LongByReference dwOriginalOutMode;
    private static LongByReference dwOriginalInMode;

    /**
     * Enables raw mode for the console, disabling line buffering and enabling virtual terminal processing.
     * Configures the console to handle input and output in a raw, unprocessed manner.
     */
    @Override
    public void enableRawMode() {

        outHandle = LibKernel32.INSTANCE.GetStdHandle(LibKernel32.STD_OUTPUT_HANDLE);
        if (outHandle == Pointer.createConstant(LibKernel32.INVALID_FILE_HANDLER)) {
            System.err.println("An error occured while getting output file handler: " + LibKernel32.INVALID_FILE_HANDLER);
            System.exit(-1);
        }

        inHandle = LibKernel32.INSTANCE.GetStdHandle(LibKernel32.STD_INPUT_HANDLE);
        if (inHandle == Pointer.createConstant(LibKernel32.INVALID_FILE_HANDLER)) {
            System.err.println("An error occured while getting input file handler: " + LibKernel32.INVALID_FILE_HANDLER);
            System.exit(-1);
        }

        dwOriginalOutMode = new LongByReference(0);
        if (!LibKernel32.INSTANCE.GetConsoleMode(outHandle, dwOriginalOutMode)) {
            System.err.println("An error occured while getting output console mode");
            System.exit(-1);
        }

        dwOriginalInMode = new LongByReference(0);
        if (!LibKernel32.INSTANCE.GetConsoleMode(inHandle, dwOriginalInMode)) {
            System.err.println("An error occured while getting input console mode");
            System.exit(-1);
        }

        // Define desired output modes: processed output and virtual terminal processing.
        long dwRequestedOutModes = LibKernel32.ENABLE_PROCESSED_OUTPUT | LibKernel32.ENABLE_VIRTUAL_TERMINAL_PROCESSING;
        // Define desired input mode: enable virtual terminal input.
        long dwRequestedInModes = LibKernel32.ENABLE_VIRTUAL_TERMINAL_INPUT;
        // Disable specific input modes (e.g., echo, line input, mouse input).
        long dwDisableInModes = ~(
                LibKernel32.ENABLE_ECHO_INPUT
              | LibKernel32.ENABLE_LINE_INPUT
              | LibKernel32.ENABLE_MOUSE_INPUT
              | LibKernel32.ENABLE_PROCESSED_INPUT
              | LibKernel32.ENABLE_WINDOW_INPUT
            );
        
        // Set the new output mode by combining original and requested modes.
        long dwOutMode = dwOriginalOutMode.getValue() | dwRequestedOutModes;
        if (!LibKernel32.INSTANCE.SetConsoleMode(outHandle, dwOutMode))
        {
            // Failed to set any VT mode, can't do anything here.
            System.err.println("An error occured while setting output console mode: " + dwOutMode);
            System.exit(-1);
        }

        // Set the new input mode by enabling requested modes and disabling unwanted ones.
        long dwInMode = (dwOriginalInMode.getValue() | dwRequestedInModes) & dwDisableInModes;
        if (!LibKernel32.INSTANCE.SetConsoleMode(inHandle, dwInMode))
        {
            // Failed to set VT input mode, can't do anything here.
            System.err.println("An error occured while setting input console mode" + dwInMode);
            System.exit(-1);
        }
    }

    /**
     * Restores the console to its original mode, disabling raw mode.
     * Reverts input and output settings to their initial state.
     */
    @Override
    public void disableRawMode() {
        if (!LibKernel32.INSTANCE.SetConsoleMode(outHandle, dwOriginalOutMode.getValue())) {
            System.err.println("An error occured while restoring output console mode: " + dwOriginalOutMode);
            System.exit(-1);
        } // Restore original terminal attributes before exiting
        
        if (!LibKernel32.INSTANCE.SetConsoleMode(inHandle, dwOriginalInMode.getValue())) {
            System.err.println("An error occured while restoring input console mode: " + dwOriginalInMode);
            System.exit(-1);
        }; // Restore original terminal attributes before exiting
    }

    /**
     * Initializes the terminal window size based on the console screen buffer information.
     * Sets the number of rows and columns for the terminal.
     */
    @Override
    public void initWindowSize() {
        LibKernel32.ConsoleScreenBufferInfo bufferInfo = getBufferInfo();
        setRows(bufferInfo.srWindow.bottom - bufferInfo.srWindow.top - 1);
        setColumns(bufferInfo.srWindow.right - bufferInfo.srWindow.left + 1);
    }

    /**
     * Retrieves console screen buffer information for the output handle.
     * @return ConsoleScreenBufferInfo containing details about the console window.
     */
    private static LibKernel32.ConsoleScreenBufferInfo getBufferInfo() {
        outHandle = LibKernel32.INSTANCE.GetStdHandle(LibKernel32.STD_OUTPUT_HANDLE);
        if (outHandle == Pointer.createConstant(LibKernel32.INVALID_FILE_HANDLER)) {
            System.err.println("An error occured while getting output file handler: " + LibKernel32.INVALID_FILE_HANDLER);
            System.exit(-1);
        }
        LibKernel32.ConsoleScreenBufferInfo bufferInfo = new LibKernel32.ConsoleScreenBufferInfo();
        LibKernel32.INSTANCE.GetConsoleScreenBufferInfo(outHandle, bufferInfo);
        return bufferInfo;
    }

    /**
     * Exits the terminal by clearing the screen, resetting the cursor, and restoring original modes.
     * Terminates the program cleanly.
     */
    @Override
    public void exit() {
        System.out.print("\033[2J");    // Clear the screen
        System.out.print("\033[H");     // Move cursor to top-left
        disableRawMode();
        System.exit(0);
    }

    /**
     * Update status bar message and refresh terminal screen.
     */ 
    public void updateStatusBarMessage(StringBuilder builder, Cursor cursor, List<String> content) {
        super.setStatusBarMessage(builder);
        refreshScreen(content, cursor);
    }
}
