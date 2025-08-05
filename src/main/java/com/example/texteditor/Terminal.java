package com.example.texteditor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Handles terminal operations, including raw mode configuration, screen rendering, and keypress handling.
 */
public class Terminal {

    private static LibC.Termios originalAttributes; // Original terminal attributes to restore on exit
    private static int rows, columns;               // Number of rows and columns of the terminal window
    private static int usedRows = 0;                // Number of rows occupied by content

    /**
     * Configures the terminal to raw mode for direct keypress handling.
     */
    public void enableRawMode() {
        // Create a new Termios structure to store terminal settings
        LibC.Termios termios = new LibC.Termios();

        // Get current terminal attributes
        int rc = LibC.INSTANCE.tcgetattr(LibC.SYSTEM_OUT_FD, termios);
        if (rc != 0) {
            System.err.println("tcgetattr generated an error code: " + rc);
            System.exit(rc);
        }
        // Save a copy of the original terminal attributes
        originalAttributes = LibC.Termios.t_copy(termios);
        // Disable:
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

    /**
     * Initialize terminal window size.
     */ 
    public void initWindowSize() {
        LibC.Winsize winSize = getWinSize();
        rows = winSize.ws_row - 2;
        columns = winSize.ws_col;
    }

    /**
     * Retrieves the terminal window size using ioctl.
     * 
     * @return The Winsize structure containing terminal dimensions.
     */
    public LibC.Winsize getWinSize() {
        LibC.Winsize winSize = new LibC.Winsize();
        int rc = LibC.INSTANCE.ioctl(LibC.SYSTEM_OUT_FD, LibC.TIOCGWINSZ, winSize);
        if (rc != 0) {
            System.err.println("ioctl generated an error code: " + rc);
            System.exit(rc);
        }
        return winSize;
    }

    /**
     * Refreshes the terminal screen with content and cursor position.
     *
     * @param content The list of text lines to display.
     * @param cursor  The cursor object managing position and scrolling.
     */
    public void refreshScreen(List<String> content, Cursor cursor) {
        StringBuilder builder = new StringBuilder();
        drawContent(builder, content, cursor);
        drawStatusBar(builder, cursor);
        drawCursor(builder, cursor);
        System.out.print(builder);
    }

    /**
     * Draws the file content with line wrapping.
     */
    private static void drawContent(StringBuilder builder, List<String> content, Cursor cursor) {
        usedRows = 0;
        cursor.resetPageWrap();
        builder.append("\033[H"); // move cursor to top-left
        for (int i = 0; i <= rows - cursor.getPageWrap(); i++) {
            if (i + cursor.getOffsetY() >= content.size()) {
                builder.append("~");  // draw ~
            } else {
                String buffer = content.get(i + cursor.getOffsetY());
                int wrap = cursor.getWrap(buffer, columns);
                if (wrap < rows - cursor.getPageWrap() - i + 1) {
                    builder.append(buffer);  // draw a line of content
                    usedRows += cursor.getWrap(buffer, columns) + 1;
                } else {
                    for (int j = 0; j < rows - cursor.getPageWrap() - i + 1; j++) {
                        builder.append("@\033[K\r\n");  // draw @
                    }
                    break;
                }
                cursor.addPageWrap(wrap);
            }
            builder.append("\033[K\r\n");  // draw new line character
        }
    }

    /**
     * Draws the status bar with editor information.
     */
    private static void drawStatusBar(StringBuilder builder, Cursor cursor) {
        builder.append("\033[7m"); // Set reverse video mode (inverted colors)
        String statusBar = "R: " + usedRows + " cY: " + cursor.getCursorY() + " oY: " + cursor.getOffsetY() + " pw: " + cursor.getPageWrap() + " cw: " + cursor.getCursorWrap() + " hw: " + cursor.getHiddenWrap() + " cd: " + cursor.getHiddenWrapCooldown();
        builder.append(statusBar).append(String.join("", Collections.nCopies(Math.max(0, (columns - statusBar.length())), " ")));
        builder.append("\033[0m"); // Reset ANSI attributes to normal
    }

    /**
     * Positions the cursor on the screen.
     */
    private static void drawCursor(StringBuilder builder, Cursor cursor) {
        builder.append(String.format("\033[%d;%dH", cursor.getCursorY() - cursor.getOffsetY() + cursor.getCursorWrap() - cursor.getHiddenWrap() + cursor.getCursorX() / columns + 1, cursor.getCursorX() % columns + 1));
    }
    
    /**
     * Exits the editor and restores original terminal settings.
     */
    // Exits the editor and restores terminal settings
    public void exit() {
        System.out.print("\033[2J");    // Clear the screen
        System.out.print("\033[H");     // Move cursor to top-left
        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, originalAttributes);  // Restore original terminal attributes before exiting
        System.exit(0);
    }

    /**
     * Reads a single keypress from standard input.
     *
     * @return The key code or -1 if an error occurs.
     */
    public int getKey() {
        try {
            int firstByte = System.in.read();

            if (firstByte != '\033') {
                return firstByte;
            }

            int secondByte = System.in.read();

            if (secondByte != '[' && secondByte != 'O') {
                return secondByte;
            }

            int thirdByte = System.in.read();
            if (secondByte == '[') {
                switch (thirdByte) {
                    case 'A': return TextEditor.ARROW_UP;
                    case 'B': return TextEditor.ARROW_DOWN;
                    case 'C': return TextEditor.ARROW_RIGHT;
                    case 'D': return TextEditor.ARROW_LEFT;
                    case 'F': return TextEditor.END;
                    case 'H': return TextEditor.HOME;
                    case '1': case '3': case '4': case '5': case '6': case '7': case '8':
                        int fourthByte = System.in.read();
                        if (fourthByte == '~') {
                            switch (thirdByte) {
                                case '1': case '7': return TextEditor.HOME;
                                case '3': return TextEditor.DEL;
                                case '4': case '8': return TextEditor.END;
                                case '5': return TextEditor.PAGE_UP;
                                case '6': return TextEditor.PAGE_DOWN;
                            }
                        } else if (fourthByte == ';') {
                            System.in.read(); // Skip modifier
                            int sixthByte = System.in.read();
                            switch (sixthByte) {
                                case 'A': return TextEditor.PAGE_UP;
                                case 'B': return TextEditor.PAGE_DOWN;
                                case 'C': case 'F': return TextEditor.END;
                                case 'D': case 'H': return TextEditor.HOME;
                            }
                            return sixthByte;
                        }
                        return fourthByte;
                    default:
                        return thirdByte;
                }
            } else {
                switch (thirdByte) {
                    case 'F': return TextEditor.END;
                    case 'H': return TextEditor.HOME;
                    default: return thirdByte;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Processes user keypresses to update cursor and content.
     *
     * @param keyPressed The key code to process.
     * @param cursor     The cursor object managing position and scrolling.
     * @param content    The list of text lines in the editor.
     */
    public void handleKey(int keyPressed, Cursor cursor, List<String> content) {
        switch (keyPressed) {
            case 'q':
                exit();
                break;
            case TextEditor.ARROW_UP:
            case TextEditor.ARROW_DOWN:
            case TextEditor.ARROW_LEFT:
            case TextEditor.ARROW_RIGHT:
            case TextEditor.PAGE_DOWN:
            case TextEditor.PAGE_UP:
            case TextEditor.HOME:
            case TextEditor.END:
            case TextEditor.DEL:
                cursor.editContent(keyPressed, content);
                cursor.moveCursor(keyPressed, content, usedRows, columns);
                break;
            default:
        }
    }

    // Getters
    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public int getUsedRows() {
        return usedRows;
    }
}