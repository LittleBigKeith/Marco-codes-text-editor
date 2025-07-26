package com.example.texteditor;

import java.io.IOException;
import java.util.Collections;

// Main class for a simple terminal-based text editor
public class Main {

    // Stores the original terminal attributes to restore on exit
    private static LibC.Termios originalAttributes;
    // Stores the number of rows and columns of the terminal window
    private static int rows, columns;

    public static void main(String[] args) {
        // Enable raw mode to capture keypresses directly without buffering
        enableRawMode();

        // Get the terminal window size
        LibC.Winsize winSize = getWinSize();
        rows = winSize.ws_row; // Set rows from window size
        columns = winSize.ws_col; // Set columns from window size

        // Main loop to continuously refresh screen and handle input
        while (true) {
            // Clear and refresh the terminal screen
            // refreshScreen();
            // Read a single keypress from the user
            int key = getKey();
            // Process the keypress (e.g., exit on 'q')
            handleKey(key);
            // Display the pressed key and its ASCII code
            printKey(key);
        }
    }

    // Clears the screen and draws a simple interface with a status bar
    private static void refreshScreen() {
        // ANSI escape code to clear the entire screen
        System.out.print("\033[2J");
        // ANSI escape code to move cursor to top-left (home position)
        System.out.print("\033[H");
        // Print '~' on each row except the last one (status bar)
        for (int i = 0; i < rows - 1; i++) {
            System.out.print("~\r\n");
        }
        // Define and print the status bar text
        String statusBar = "Text Editor";
        // Set reverse video mode (inverted colors) for status bar
        System.out.print("\033[7m");
        // Print status bar text and pad with spaces to fill the line
        System.out.print(
                statusBar + String.join("", Collections.nCopies(Math.max(0, (columns - statusBar.length())), " ")));
        // Reset ANSI attributes to normal
        System.out.print("\033[0m");
    }

    // Reads a single keypress from standard input
    private static int getKey() {
        int key = 0;
        try {
            // Read one byte from System.in (returns ASCII code of the key)
            key = System.in.read();
        } catch (IOException e) {
            System.err.println("System.in.read throws an exception: " + e);
        }
        return key;
    }

    // Handles keypresses, currently only exits on 'q'
    private static void handleKey(int key) {
        if (key == 'q') {
            // Clear the screen
            System.out.print("\033[2J");
            // Move cursor to top-left
            System.out.print("\033[H");
            // Restore original terminal attributes before exiting
            LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, originalAttributes);
            // Exit the program
            System.exit(0);
        }
    }

    // Prints the character and its ASCII code for debugging
    private static void printKey(int key) {
        // Print the character and its ASCII value
        System.out.print((char) key + " => (" + key + ")\r\n");
    }

    // Configures the terminal to raw mode for direct keypress handling
    private static void enableRawMode() {
        // Create a new Termios structure to store terminal settings
        LibC.Termios termios = new LibC.Termios();

        // Get current terminal attributes
        int rc = LibC.INSTANCE.tcgetattr(LibC.SYSTEM_OUT_FD, termios);
        // Check if getting attributes failed
        if (rc != 0) {
            System.err.println("tcgetattr generated an error code: " + rc);
            System.exit(rc);
        }
        // Save a copy of the original terminal attributes
        originalAttributes = LibC.Termios.t_copy(termios);
        // Modify terminal settings to disable:
        // - ECHO: echoing input characters
        // - ICANON: canonical mode (line buffering)
        // - IEXTEN: extended input processing
        // - ISIG: signal-generating characters (e.g., Ctrl+C)
        termios.c_lflag &= ~(LibC.ECHO | LibC.ICANON | LibC.IEXTEN | LibC.ISIG);
        // Disable input processing:
        // - IXON: software flow control (Ctrl+S, Ctrl+Q)
        // - ICRNL: carriage return to newline conversion
        termios.c_iflag &= ~(LibC.IXON | LibC.ICRNL);
        // Disable output processing (e.g., newline conversions)
        termios.c_oflag &= ~(LibC.OPOST);

        // Apply the modified terminal settings
        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, termios);
    }

    // Retrieves the terminal window size using ioctl
    private static LibC.Winsize getWinSize() {
        // Create a Winsize structure to store window dimensions
        LibC.Winsize winSize = new LibC.Winsize();
        // Debug: Print the TIOCGWINSZ constant
        System.out.print("LibC.TIOCGWINSZ = " + LibC.TIOCGWINSZ + "\r\n");
        // Call ioctl to get window size
        int rc = LibC.INSTANCE.ioctl(LibC.SYSTEM_OUT_FD, LibC.TIOCGWINSZ, winSize);
        // Check if ioctl call failed
        if (rc != 0) {
            System.err.println("ioctl generated an error code: " + rc);
            System.exit(rc);
        }

        // Debug: Print Winsize details
        System.out.print(winSize.toString() + "\r\n");
        return winSize;
    }
}