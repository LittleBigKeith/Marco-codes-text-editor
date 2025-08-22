package com.example.texteditor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class IOHandler {
    
    private int rows, columns;               // Number of rows and columns of the terminal window
    private int usedRows = 0;                // Number of rows occupied by content

    protected String statusBarMessage = new String();

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
    private void drawContent(StringBuilder builder, List<String> content, Cursor cursor) {
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
    private void drawStatusBar(StringBuilder builder, Cursor cursor) {
        builder.append("\033[7m"); // Set reverse video mode (inverted colors)
        String statusBarMessage = this.statusBarMessage.isEmpty() ?
            "R: " + usedRows + " cY: " + cursor.getCursorY() + " oY: " + cursor.getOffsetY() + " pw: " + cursor.getPageWrap() + " cw: " + cursor.getCursorWrap() + " hw: " + cursor.getHiddenWrap() + " cd: " + cursor.getHiddenWrapCooldown() :
            this.statusBarMessage;
        builder.append(statusBarMessage).append(String.join("", Collections.nCopies(Math.max(0, (columns - statusBarMessage.length())), " ")));
        builder.append("\033[0m"); // Reset ANSI attributes to normal
    }

    /**
     * Positions the cursor on the screen.
     */
    private void drawCursor(StringBuilder builder, Cursor cursor) {
        builder.append(String.format("\033[%d;%dH", cursor.getCursorY() - cursor.getOffsetY() + cursor.getCursorWrap() - cursor.getHiddenWrap() + cursor.getCursorX() / columns + 1, cursor.getCursorX() % columns + 1));
    }

    /**
     * Reads a single keypress from standard input.
     *
     * @return The key code or -1 if an error occurs.
     */
    public int getKey() {
        try {
            ByteBuffer byteBuffer = new ByteBuffer(6);
            System.in.read(byteBuffer.getBuffer());
            int firstByte = byteBuffer.next();

            if (firstByte != '\033') {
                return firstByte;
            }

            int secondByte = byteBuffer.next();

            if (secondByte != '[' && secondByte == 0) {
                return TextEditor.ESC;
            }

            int thirdByte = byteBuffer.next();
            if (secondByte == '[') {
                switch (thirdByte) {
                    case 'A': return TextEditor.ARROW_UP;
                    case 'B': return TextEditor.ARROW_DOWN;
                    case 'C': return TextEditor.ARROW_RIGHT;
                    case 'D': return TextEditor.ARROW_LEFT;
                    case 'F': return TextEditor.END;
                    case 'H': return TextEditor.HOME;
                    case '1': case '3': case '4': case '5': case '6': case '7': case '8':
                        int fourthByte = byteBuffer.next();
                        if (fourthByte == '~') {
                            switch (thirdByte) {
                                case '1': case '7': return TextEditor.HOME;
                                case '3': return TextEditor.DEL;
                                case '4': case '8': return TextEditor.END;
                                case '5': return TextEditor.PAGE_UP;
                                case '6': return TextEditor.PAGE_DOWN;
                            }
                        } else if (fourthByte == ';') {
                            int sixthByte = byteBuffer.skip();
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
        cursor.editContent(keyPressed, content);
        cursor.moveCursor(keyPressed, content, this, usedRows, columns);
        cursor.scroll(keyPressed, content, rows, columns);
    }

    public void handleKey(int keyPressed, Cursor cursor, List<String> content, int targetRow, int targetCol) {
        cursor.editContent(keyPressed, content);
        cursor.moveCursor(keyPressed, content, this, usedRows, columns, targetRow, targetCol);
        cursor.scroll(keyPressed, content, rows, columns);
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

    // Setters
    public void setRows(int rows) {
        this.rows = rows;
    }

    public void setColumns(int cols) {
        this.columns = cols;
    }

    public void setStatusBarMessage(StringBuilder builder) {
        this.statusBarMessage = builder.toString();
    }
}
