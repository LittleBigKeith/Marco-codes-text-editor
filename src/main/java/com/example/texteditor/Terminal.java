package com.example.texteditor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

// Handles terminal operations and rendering
public class Terminal {

    // Stores the original terminal attributes to restore on exit
    private static LibC.Termios originalAttributes;
    // Stores the number of rows and columns of the terminal window
    private static int rows, columns;

    // Configures the terminal to raw mode for direct keypress handling
    public void enableRawMode() {
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

    // Initialize terminal window size
    public void initWindowSize() {
        LibC.Winsize winSize = getWinSize();
        rows = winSize.ws_row - 2;
        columns = winSize.ws_col;
    }

    // Retrieves the terminal window size using ioctl
    public LibC.Winsize getWinSize() {
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
        return winSize;
    }

    // Clears the screen and draws a simple interface with a status bar
    public void refreshScreen(List<String> content, Cursor cursor) {
        StringBuilder builder = new StringBuilder();
        drawContent(builder, content, cursor);
        drawStatusBar(builder, cursor);
        drawCursor(builder, cursor);
        System.out.print(builder);
    }

    // Draws the file content with wrapping
    private static void drawContent(StringBuilder builder, List<String> content, Cursor cursor) {
        
        cursor.resetPageWrap();
        // ANSI escape code to clear the entire screen
        // ANSI escape co de to move cursor to top-left (home position)
        builder.append("\033[H");
        // Print '~' on each row except the last one (status bar)
        for (int i = 0; i <= rows - cursor.getPageWrap(); i++) {
            if (i + cursor.getOffsetY() >= content.size()) {
                builder.append("~");
            } else {
                String buffer = content.get(i + cursor.getOffsetY());
                int wrap = cursor.getWrap(buffer, columns);
                if (wrap < rows - cursor.getPageWrap() -i + 1) {
                    builder.append(buffer);
                } else {
                    for (int j = 0; j < rows - cursor.getPageWrap() - i + 1; j++) {
                        builder.append("@\033[K\r\n");
                    }
                    break;
                }
                cursor.addPageWrap(wrap);
            }
            builder.append("\033[K\r\n");
        }
    }

    // Draws the status bar with editor information
    private static void drawStatusBar(StringBuilder builder, Cursor cursor) {
        // Set reverse video mode (inverted colors) for status bar
        builder.append("\033[7m");
        // Define and print the status bar text
        String statusBar = "R: " + rows + " cY: " + cursor.getCursorY() + " oY: " + cursor.getOffsetY() + " pw: " + cursor.getPageWrap() + " cw: " + cursor.getCursorWrap() + " hw: " + cursor.getHiddenWrap() + " cd: " + cursor.getHiddenWrapCooldown();
        // Print status bar text and pad with spaces to fill the line
        builder.append(statusBar).append(String.join("", Collections.nCopies(Math.max(0, (columns - statusBar.length())), " ")));
        // Reset ANSI attributes to normal
        builder.append("\033[0m");
    }

    // Positions the cursor on the screen
    private static void drawCursor(StringBuilder builder, Cursor cursor) {
        builder.append(String.format("\033[%d;%dH", cursor.getCursorY() - cursor.getOffsetY() + cursor.getCursorWrap() - cursor.getHiddenWrap() + 1, cursor.getCursorX() + 1));
    }
    
    // Exits the editor and restores terminal settings
    public void exit() {
        // Clear the screen
        System.out.print("\033[2J");
        // Move cursor to top-left
        System.out.print("\033[H");
        // Restore original terminal attributes before exiting
        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, originalAttributes);
        // Exit the program
        System.exit(0);
    }

    // Reads a single keypress from standard input
    public int getKey() {
        try {
            // Read one byte from System.in (returns ASCII code of the key)
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
                    case 'A': // \033[A
                        thirdByte = TextEditor.ARROW_UP;
                        return thirdByte;
                    case 'B': // \033[B
                        thirdByte = TextEditor.ARROW_DOWN;
                        return thirdByte;
                    case 'C': // \033[C
                        thirdByte = TextEditor.ARROW_RIGHT;
                        return thirdByte;
                    case 'D': // \033[D
                        thirdByte = TextEditor.ARROW_LEFT;
                        return thirdByte;
                    // end = fn OR shift + arrow_right
                    case 'F': // \033[F
                        thirdByte = TextEditor.END;
                        return thirdByte;
                    // home = fn OR shift + arrow_left
                    case 'H': // \033[H
                        thirdByte = TextEditor.HOME;
                        return thirdByte;
                    case '1':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                        int fourthByte = System.in.read();
                        switch (fourthByte) {
                            case '~':
                                switch (thirdByte) {
                                    // home = fn OR shift + arrow_left
                                    case '1':
                                    case '7':
                                        return TextEditor.HOME;
                                    // del = fn + delete
                                    case '3':
                                        return TextEditor.DEL;
                                    // end = fn OR shift + arrow_right
                                    case '4':
                                    case '8':
                                        return TextEditor.END;
                                    // page_up = fn OR/AND shift + arrow_up
                                    case '5':
                                        return TextEditor.PAGE_UP;
                                    // page_down = fn OR/AND shift + arrow_down
                                    case '6':
                                        return TextEditor.PAGE_DOWN;
                                    default:
                                        return thirdByte;
                                }
                            case ';':
                                System.in.read();
                                int sixthByte = System.in.read();
                                switch (sixthByte) {
                                    // page_up = fn OR/AND shift + arrow_up
                                    case 'A':
                                        sixthByte = TextEditor.PAGE_UP;
                                        break;
                                    // page_down = fn OR/AND shift + arrow_down
                                    case 'B':
                                        sixthByte = TextEditor.PAGE_DOWN;
                                        break;
                                    // end = fn OR shift + arrow_right    
                                    case 'C':
                                    case 'F':
                                        sixthByte = TextEditor.END;
                                        break;
                                    // home = fn OR shift + arrow_left
                                    case 'D':
                                    case 'H':
                                        sixthByte = TextEditor.HOME;
                                        break;
                                    default:
                                }
                                return sixthByte;
                            default:
                                return fourthByte;
                        }
                    default:
                        return thirdByte;
                }
            } else {
                switch (thirdByte) {
                    case 'F':
                        return TextEditor.END;
                    case 'H':
                        return TextEditor.HOME;
                    default:
                        return thirdByte;
                }
            }

        } catch (IOException e) {
            System.err.println("System.in.read throws an exception: " + e);
        }
        return -1;
    }

    // Processes user keypresses
    public void handleKey(int keyPressed, Cursor cursor, List<String> content) {
        switch (keyPressed) {
            case 'q':
                exit();
                break;
            case TextEditor.ARROW_UP:
            case TextEditor.ARROW_DOWN:
            case TextEditor.ARROW_LEFT:
            case TextEditor.ARROW_RIGHT:
            case TextEditor.HOME:
            case TextEditor.END:
                cursor.moveCursor(keyPressed, content, getColumns());
                break;
            default:
        }
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }
}