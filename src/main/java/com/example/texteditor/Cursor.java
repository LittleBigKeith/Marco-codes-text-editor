package com.example.texteditor;

import java.util.List;

/**
 * Manages the cursor position and scrolling behavior in a terminal-based text editor.
 * Tracks cursor coordinates, handles line wrapping, and manages vertical scrolling.
 */
public class Cursor {

    private static final int PAGE_SCROLL_OFFSET = 2;

    private int cursorX;        // Current x-coordinate of the cursor (column)
    private int cursorXcache;   // Cached x-coordinate for cursor movement
    private int cursorY;        // Current y-coordinate of the cursor (line)
    private int offsetY;        // Vertical scroll offset (first visible line)
    private int cursorWrap;     // Accumulated line wraps for the cursor
    private int pageWrap;       // Accumulated line wraps for the page
    private int hiddenWrap;     // Hidden line wraps due to scrolling
    private int hiddenWrapCooldown; // Cooldown for hidden wrap updates

    private boolean whichWrapEnabled = true;

    /**
     * Constructs a new Cursor with initialized position and scroll state.
     */
    public Cursor() {
        this.cursorX = 0;
        this.cursorXcache = 0;
        this.cursorY = 0;
        this.offsetY = 0;
        this.cursorWrap = 0;
        this.pageWrap = 0;
        this.hiddenWrap = 0;
        this.hiddenWrapCooldown = 0;
    }

    /**
     * Handles scrolling based on cursor movement and key input.
     *
     * @param keyPressed The key code representing the user input.
     * @param content    The list of text lines in the editor.
     * @param rows       The number of visible rows in the terminal.
     * @param usedRows   The number of rows currently occupied by content.
     * @param columns    The number of columns in the terminal.
     */
    public void scroll(int keyPressed, List<String> content, int rows, int usedRows, int columns) {

        if (content.size() <= 0) {
            return;
        }

        switch(keyPressed) {
            case TextEditor.ARROW_DOWN:
                handleArrowDownScroll(content, usedRows, columns);
                break;
            case TextEditor.ARROW_UP:
                handleArrowUpScroll(content, columns);
                break;
            case TextEditor.PAGE_DOWN:
                handlePageDownScroll(content, columns);
                break;
            case TextEditor.PAGE_UP:
                handlePageUpScroll(content, rows, columns);
                break;
            case TextEditor.FIND:
                handleFindScroll(content, columns);
                break;
            default:
        }
    }

    /**
     * Handles scrolling logic for ARROW_DOWN key.
     */
    private void handleArrowDownScroll(List<String> content, int usedRows, int columns) {
        if (offsetY - pageWrap + usedRows - 1 < cursorY) {
            int newContentWrap = getWrap(content.get(cursorY), columns);
            int newHiddenWrap = getWrap(content.get(offsetY), columns);
            int tempWrap = 0;
            
            // Calculate additional wraps needed to keep cursor in view
            for (int maxIter = newContentWrap; maxIter > 0; maxIter--) {
                newHiddenWrap += getWrap(content.get(offsetY + tempWrap + 1), columns);
                tempWrap++;
                if (newHiddenWrap >= newContentWrap) {
                    break;
                }
            }

            // Update scroll offset and wraps
            if (hiddenWrapCooldown <= 0) {
                offsetY = offsetY + 1 + tempWrap;
                hiddenWrap += newHiddenWrap;
                hiddenWrapCooldown = newHiddenWrap;
            } else if (cursorY + cursorWrap + getWrap(content.get(cursorY), columns) > offsetY + hiddenWrap + usedRows) {
                offsetY = Math.min(offsetY + 1 + tempWrap, content.size());
                hiddenWrap += newHiddenWrap;
            }
        }

        // Decrement cooldown if active
        if (hiddenWrapCooldown > 0) {
            hiddenWrapCooldown--;
        }
    }

    /**
     * Handles scrolling logic for ARROW_UP key.
     */
    private void handleArrowUpScroll(List<String> content, int columns) {
        if (cursorY < offsetY) {
            offsetY = Math.max(offsetY - 1, 0);
            hiddenWrap -= getWrap(content.get(cursorY), columns);
        }
    }

    /**
     * Handles scrolling logic for PAGE_DOWN key.
     */
    private void handlePageDownScroll(List<String> content, int columns) {
        int addedHiddenWrap = 0;
        for (int i = offsetY; i < cursorY; i++) {
            addedHiddenWrap += getWrap(content.get(i), columns); 
        }
        hiddenWrap += addedHiddenWrap;
        offsetY = cursorY;
    }

    /**
     * Handles scrolling logic for PAGE_UP key.
     */
    private void handlePageUpScroll(List<String> content, int rows, int columns) {
        int reducedHiddenWrap = 0;
        while (offsetY > 0) {
            offsetY--;
            reducedHiddenWrap += getWrap(content.get(offsetY), columns);
            if (cursorY - offsetY + content.get(cursorY).length() / columns + reducedHiddenWrap + 1 > rows) {
                reducedHiddenWrap -= getWrap(content.get(offsetY), columns);
                offsetY++;
                break;
            }
        }
        hiddenWrap -= reducedHiddenWrap;
    }

    private void handleFindScroll(List<String> content, int columns) {
        if (offsetY < cursorY) {
            handlePageDownScroll(content, columns);
        } else {
            int reducedHiddenWrap = 0;
            for (int i = cursorY; i < offsetY; i++) {
                reducedHiddenWrap += getWrap(content.get(i), columns);
            }
            hiddenWrap -= reducedHiddenWrap;
            offsetY = cursorY;
        }
    }

    /**
     * Moves the cursor based on the key pressed.
     *
     * @param key      The key code representing the user input.
     * @param content  The list of text lines in the editor.
     * @param usedRows The number of rows currently occupied by content.
     * @param columns  The number of columns in the terminal.
     */
    public void moveCursor(int key, List<String> content, IOHandler terminal, int usedRows, int columns) {

        if (content.isEmpty()) {
            return;
        }

        int prevCursorY = cursorY;

        switch (key) {
            case TextEditor.ARROW_DOWN:
                moveCursorDown(prevCursorY, content, columns);
                break;
            case TextEditor.ARROW_UP:
                moveCursorUp(prevCursorY, content, columns);
                break;
            case TextEditor.PAGE_DOWN:
                moveCursorPageDown(prevCursorY, content, usedRows, columns);
                break;
            case TextEditor.PAGE_UP:
                moveCursorPageUp(prevCursorY, content, columns);
                break;
            case TextEditor.ARROW_LEFT:
                moveCursorLeft(content, terminal);
                break;
            case TextEditor.ARROW_RIGHT:
                moveCursorRight(content, terminal);
                break;
            case TextEditor.HOME:
                moveCursorHome();
                break;
            case TextEditor.END:
                moveCursorEnd(content);
                break;
            case TextEditor.DEL:
                moveCursorDel(content);
                break;
            case TextEditor.BACKSPACE:
                moveCursorBackspace(content);
            default:
        }
        // Ensure cursorX stays within valid bounds
        cursorX = Math.min(cursorXcache, Math.max(content.get(cursorY).length(), 0));
    }

    public void moveCursor(int key, List<String> content, IOHandler ioHandler, int usedRows, int columns, int targetRow, int targetCol) {
        if (content.isEmpty()) {
            return;
        }

        int prevCursorY = cursorY;

        cursorY = targetRow;
        handleCursorWrap(prevCursorY, content, columns);

        cursorX = targetCol;
        cursorXcache = cursorX;
    }

    /**
     * Moves the cursor down one line.
     */
    private void moveCursorDown(int prevCursorY, List<String> content, int columns) {
        if (cursorY < content.size() - 1) {
            cursorY++;
            handleCursorWrap(prevCursorY, content, columns);
        }
    }

    /**
     * Moves the cursor up one line.
     */
    private void moveCursorUp(int prevCursorY, List<String> content, int columns) {
        if (cursorY > 0) {
            cursorY--;
            handleCursorWrap(prevCursorY, content, columns);
        }
    }

    /**
     * Moves the cursor for PAGE_DOWN key.
     */
    private void moveCursorPageDown(int prevCursorY, List<String> content, int usedRows, int columns) {
        if (offsetY + usedRows - pageWrap == content.size()) {
            cursorY = offsetY + usedRows - pageWrap - 1;
        } else {
            cursorY = offsetY + usedRows - pageWrap - PAGE_SCROLL_OFFSET;
        }
        handleCursorWrap(prevCursorY, content, columns);
    }

    /**
     * Moves the cursor for PAGE_UP key.
     */
    private void moveCursorPageUp(int prevCursorY, List<String> content, int columns) {
        if (offsetY == 0) {
            cursorY = offsetY;
        } else if (offsetY == content.size() - 1) {
            cursorY = offsetY - 1;
        } else {
            cursorY = offsetY + PAGE_SCROLL_OFFSET - 1;
        }
        handleCursorWrap(prevCursorY, content, columns);
    }

    /**
     * Moves the cursor left one character.
     */
    private void moveCursorLeft(List<String> content, IOHandler terminal) {
        if (cursorX > 0) {
            cursorX--;
            cursorXcache = cursorX;
        } else if (whichWrapEnabled && cursorY > 0) {
            terminal.handleKey(TextEditor.ARROW_UP, this, content);
            terminal.handleKey(TextEditor.END, this, content);
        }
    }

    /**
     * Moves the cursor right one character.
     */
    private void moveCursorRight(List<String> content, IOHandler terminal) {
        if (cursorX < content.get(cursorY).length()) {
            cursorX++;
            cursorXcache = cursorX;
        } else if (whichWrapEnabled && cursorY < content.size() - 1) {
            terminal.handleKey(TextEditor.ARROW_DOWN, this, content);
            terminal.handleKey(TextEditor.HOME, this, content);
        }
    }

    /**
     * Moves the cursor for HOME key.
     */
    private void moveCursorHome() {
        cursorX = 0;
        cursorXcache = cursorX;
    }

    /**
     * Moves the cursor for END key.
     */
    private void moveCursorEnd(List<String> content) {
        cursorX = content.get(cursorY).length();
        cursorXcache = cursorX;
    }

    /**
     * Moves the cursor when delete one character.
     */
    private void moveCursorDel(List<String> content) {
        cursorX = Math.min(cursorX, Math.max(content.get(cursorY).length() -  1, 0));
        cursorXcache = cursorX;
    }

    private void moveCursorBackspace(List<String> content) {
        cursorX = Math.max(cursorX - 1, 0);
        cursorXcache = cursorX;
    }

    /**
     * Updates cursor wrap count when scrolling pages.
     */
    private void handleCursorWrap(int prevCursorY, List<String> content, int columns) {
        int addedCursorWrap= 0;
        for (int i = cursorY; i < prevCursorY; i++) {
            addedCursorWrap -= getWrap(content.get(i), columns);
        }
        for (int i = prevCursorY; i < cursorY; i++) {
            addedCursorWrap += getWrap(content.get(i), columns);
        }
        cursorWrap += addedCursorWrap;
    }

    /**
     * Edits the content based on the key pressed (e.g., delete character).
     *
     * @param key     The key code representing the user input.
     * @param content The list of text lines in the editor.
     */
    public void editContent(int key, List<String> content) {
        if (content.size() <= 0) {
            return;
        }

        switch (key) {
            case TextEditor.DEL:
                if (content.get(cursorY).length() > 0) {
                    content.set(cursorY, String.join("", content.get(cursorY).substring(0, cursorX), content.get(cursorY).substring(cursorX + 1)));
                }
                break;
            case TextEditor.BACKSPACE:
                if (content.get(cursorY).length() > 0) {
                    content.set(cursorY, String.join("", content.get(cursorY).substring(0, Math.max(cursorX - 1, 0)), content.get(cursorY).substring(cursorX)));
                }
                break;
            default:
        }
    }

    /**
     * Calculates the number of line wraps for a given line based on terminal width.
     *
     * @param line    The text line to calculate wrapping for.
     * @param columns The number of columns in the terminal.
     * @return The number of wraps needed for the line.
     */
    public int getWrap(String line, int columns) {
        return Math.max(line.length() - 1, 0) / columns;
    }

    // Getters
    public int getCursorX() {
        return cursorX;
    }

    public int getCursorY() {
        return cursorY;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getCursorWrap() {
        return cursorWrap;
    }

    public int getPageWrap() {
        return pageWrap;
    }

    public int getHiddenWrap() {
        return hiddenWrap;
    }

    public int getHiddenWrapCooldown() {
        return hiddenWrapCooldown;
    }

    // Setters
    public void addPageWrap(int wrap) {
        pageWrap += wrap;
    }

    public void resetPageWrap() {
        pageWrap = 0;
    }
}
