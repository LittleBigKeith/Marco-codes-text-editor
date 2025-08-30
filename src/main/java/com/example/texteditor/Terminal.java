package com.example.texteditor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class Terminal {
    
    private int rows, columns;               // Number of rows and columns of the terminal window
    private int usedRows = 0;                // Number of rows occupied by content

    private String statusBarMessage = new String();
    private int statusBarTextColor = 30;

    private ByteBuffer byteBuffer = new ByteBuffer();

    /**
     * Refreshes the terminal screen with content and cursor position.
     *
     * @param content The list of text lines to display.
     * @param cursor  The cursor object managing position and scrolling.
     */
    public void refreshScreen(List<String> content, Cursor cursor) {
        drawScreen(content, cursor);
    }

    public void refreshScreen(List<String> content, Cursor cursor, int textColor) {
        statusBarTextColor = textColor;
        drawScreen(content, cursor);
    }

    public void drawScreen(List<String> content, Cursor cursor) {
        StringBuilder builder = new StringBuilder();
        drawContent(builder, content, cursor);
        drawStatusBar(builder, cursor, content);
        drawCursor(builder, content, cursor);
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
                int wrap = cursor.getWrap(buffer, columns, this);
                if (wrap < rows - cursor.getPageWrap() - i + 1) {
                    builder.append(buffer);  // draw a line of content
                    usedRows += cursor.getWrap(buffer, columns, this) + 1;
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
    private void drawStatusBar(StringBuilder builder, Cursor cursor, List<String> content) {
        builder.append("\033[47;");
        builder.append(statusBarTextColor);
        builder.append("m");
        drawStatusBarMessage(builder, cursor, content);
    }

    private void drawStatusBarMessage(StringBuilder builder, Cursor cursor, List<String> content) {
        String statusBarMessage = this.statusBarMessage.isEmpty() ?
            "R: " + usedRows + " cX: " + cursor.getCursorX() + " cY: " + cursor.getCursorY() + " oY: " + cursor.getOffsetY() + " pw: " + cursor.getPageWrap() + " cw: " + cursor.getCursorWrap() + " hw: " + cursor.getHiddenWrap() + " lw: " + getLineWidthUpTo(content.get(cursor.getCursorY()), cursor.getCursorX(), columns) :
            this.statusBarMessage;
        builder.append(statusBarMessage).append(String.join("", Collections.nCopies(Math.max(0, (columns - statusBarMessage.length())), " ")));
        builder.append("\033[0m"); // Reset ANSI attributes to normal
    }

    /**
     * Positions the cursor on the screen.
     */
    private void drawCursor(StringBuilder builder, List<String> content, Cursor cursor) {
        builder.append(String.format("\033[%d;%dH", cursor.getCursorY() - cursor.getOffsetY() + cursor.getCursorWrap() - cursor.getHiddenWrap() + getLineWidthUpTo(content.get(cursor.getCursorY()), cursor.getCursorX(), columns) / columns + 1, getLineWidthUpTo(content.get(cursor.getCursorY()), cursor.getCursorX(), columns) % columns + 1));
    }

    /**
     * Reads a single keypress from standard input.
     *
     * @return The key code or -1 if an error occurs.
     */
    public int getKey() {
        try {
            byteBuffer.clear();
            System.in.read(byteBuffer.getBuffer());
            int firstByte = byteBuffer.next();

            if (firstByte != '\033') {
                return firstByte;
            }

            int secondByte = byteBuffer.next();

            if (secondByte != '[' && secondByte == 0) {
                return firstByte;
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
        cursor.editContent(keyPressed, content, byteBuffer);
        cursor.moveCursor(keyPressed, content, this, usedRows, columns);
        cursor.scroll(keyPressed, content, rows, columns, this);
    }

    public void handleKey(int keyPressed, Cursor cursor, List<String> content, int targetRow, int targetCol) {
        cursor.editContent(keyPressed, content, byteBuffer);
        cursor.moveCursor(keyPressed, content, this, usedRows, columns, targetRow, targetCol);
        cursor.scroll(keyPressed, content, rows, columns, this);
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

    public int getStatusBarTextColor() {
        return statusBarTextColor;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    // Setters
    public void setRows(int rows) {
        this.rows = rows;
    }

    public void setColumns(int cols) {
        this.columns = cols;
    }

    /**
     * Update status bar message and refresh terminal screen.
     */ 
    public void setStatusBarMessage(String message) {
        this.statusBarMessage = message;
    }

    public void setStatusBarTextColor(int color) {
        statusBarTextColor = color;
    }

    public void updateStatusBarMessage(String message, Cursor cursor, List<String> content) {
        setStatusBarTextColor(30);
        setStatusBarMessage(message);
        refreshScreen(content, cursor);
    }

    public void updateStatusBarMessage(String message, Cursor cursor, List<String> content, int textColor) {
        setStatusBarMessage(message);
        refreshScreen(content, cursor, textColor);
    }

    abstract void enableRawMode();
    abstract void disableRawMode();
    abstract void initWindowSize();
    abstract void setLocale();
    abstract int getCharWidth(long c);
    abstract int getLineWidth(String line, int columns);
    abstract int getLineWidthUpTo(String line, int cursorX, int columns);
    abstract void exit();
}
