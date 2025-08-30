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

    private boolean contentChanged;
    private int backspaceCache;

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
        this.contentChanged = false;
    }

    /**
     * Handles scrolling based on cursor movement and key input.
     *
     * @param key The key code representing the user input.
     * @param content    The list of text lines in the editor.
     * @param rows       The number of visible rows in the terminal.
     * @param usedRows   The number of rows currently occupied by content.
     * @param columns    The number of columns in the terminal.
     */
    public void scroll(int key, List<String> content, int rows, int columns, Terminal terminal) {

        if (content.size() <= 0) {
            return;
        }

        switch(key) {
            case TextEditor.ARROW_DOWN:
                handleArrowDownScroll(content, rows, columns, terminal);
                break;
            case TextEditor.ARROW_UP:
                handleArrowUpScroll(content, columns, terminal);
                break;
            case TextEditor.PAGE_DOWN:
                handlePageDownScroll(content, columns, terminal);
                break;
            case TextEditor.PAGE_UP:
                handlePageUpScroll(content, rows, columns, terminal);
                break;
            case TextEditor.FIND:
                handleFindScroll(content, columns, terminal);
                break;
            case TextEditor.DEL:
                handleDelScroll(content, rows, columns, terminal);
                break;
            case TextEditor.BACKSPACE:
                handleBackspaceScroll(content, columns, terminal);
                break;
            case TextEditor.ENTER:
                handleEnterScroll(content, rows, columns, terminal);
                break;
            default:
                if (!Character.isISOControl(key) && key < 128) {
                    handleInsertCharScroll(content, rows, columns, terminal);
                }
        }
    }

    /**
     * Handles scrolling logic for ARROW_DOWN key.
     */
    private void handleArrowDownScroll(List<String> content, int rows, int columns, Terminal terminal) {
        moveCursorIntoView(content, rows, columns, terminal);
    }

    private void moveCursorIntoView(List<String> content, int rows, int columns, Terminal terminal) {
        while (cursorY + cursorWrap + getWrap(content.get(cursorY), columns, terminal) > offsetY + hiddenWrap + rows) {
            hiddenWrap += getWrap(content.get(offsetY), columns, terminal);
            offsetY = Math.min(offsetY + 1, content.size());
        }
    }

    /**
     * Handles scrolling logic for inserting a character.
     */
    private void handleInsertCharScroll(List<String> content, int rows, int columns, Terminal terminal) {
        if (cursorY + cursorWrap + getWrap(content.get(cursorY), columns, terminal) > offsetY + hiddenWrap + rows) {
            hiddenWrap += getWrap(content.get(offsetY), columns, terminal);
            offsetY = Math.min(offsetY + 1, content.size());
        }
    }

    /**
     * Handles scrolling logic for ARROW_UP key.
     */
    private void handleArrowUpScroll(List<String> content, int columns, Terminal terminal) {
        if (cursorY < offsetY) {
            scrollUpOneLine(content, columns, terminal);
        }
    }

    private void scrollUpOneLine(List<String> content, int columns, Terminal terminal) {
        offsetY = Math.max(offsetY - 1, 0);
        hiddenWrap -= getWrap(content.get(cursorY), columns, terminal);
    }
    
    /**
     * Handles scrolling logic for PAGE_DOWN key.
     */
    private void handlePageDownScroll(List<String> content, int columns, Terminal terminal) {
        int addedHiddenWrap = 0;
        for (int i = offsetY; i < cursorY; i++) {
            addedHiddenWrap += getWrap(content.get(i), columns, terminal); 
        }
        hiddenWrap += addedHiddenWrap;
        offsetY = cursorY;
    }

    /**
     * Handles scrolling logic for PAGE_UP key.
     */
    private void handlePageUpScroll(List<String> content, int rows, int columns, Terminal terminal) {
        int reducedHiddenWrap = 0;
        int startOffsetWrap = getWrap(content.get(offsetY), columns, terminal);
        while (offsetY > 0) {
            offsetY--;
            reducedHiddenWrap += getWrap(content.get(offsetY), columns, terminal);
            if (cursorY - offsetY + 1 + startOffsetWrap + reducedHiddenWrap > rows) {
                reducedHiddenWrap -= getWrap(content.get(offsetY), columns, terminal);
                offsetY++;
                break;
            }
        }
        hiddenWrap -= reducedHiddenWrap;
    }

    /**
     * Handles scrolling logic for find function.
     */
    private void handleFindScroll(List<String> content, int columns, Terminal terminal) {
        if (offsetY < cursorY) {
            handlePageDownScroll(content, columns, terminal);
        } else {
            int reducedHiddenWrap = 0;
            for (int i = cursorY; i < offsetY; i++) {
                reducedHiddenWrap += getWrap(content.get(i), columns, terminal);
            }
            hiddenWrap -= reducedHiddenWrap;
            offsetY = cursorY;
        }
    }

    private void handleDelScroll(List<String> content, int rows, int columns, Terminal terminal) {
        moveCursorIntoView(content, rows, columns, terminal);
    }

    private void handleBackspaceScroll(List<String> content, int columns, Terminal terminal) {
        if (cursorY < offsetY) {
            scrollUpOneLine(content, columns, terminal);
        }
    }

    private void handleEnterScroll(List<String> content, int rows, int columns, Terminal terminal) {
        moveCursorIntoView(content, rows, columns, terminal);
    }

    /**
     * Moves the cursor based on the key pressed.
     *
     * @param key      The key code representing the user input.
     * @param content  The list of text lines in the editor.
     * @param usedRows The number of rows currently occupied by content.
     * @param columns  The number of columns in the terminal.
     */
    public void moveCursor(int key, List<String> content, Terminal terminal, int usedRows, int columns) {

        if (content.isEmpty()) {
            return;
        }

        int prevCursorY = cursorY;

        switch (key) {
            case TextEditor.ARROW_DOWN:
                moveCursorDown(prevCursorY, content, columns, terminal);
                break;
            case TextEditor.ARROW_UP:
                moveCursorUp(prevCursorY, content, columns, terminal);
                break;
            case TextEditor.PAGE_DOWN:
                moveCursorPageDown(prevCursorY, content, columns, usedRows, terminal);
                break;
            case TextEditor.PAGE_UP:
                moveCursorPageUp(prevCursorY, content, columns, terminal);
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
                moveCursorBackspace(content, columns, terminal);
                break;
            case TextEditor.ENTER:
                moveCursorEnter(prevCursorY, content, columns, terminal);
                break;
            default:
                if (!Character.isISOControl(key)) {
                    moveCursorInsertChar(content, columns, terminal);
                }
        }

        // Ensure cursorX stays within valid bounds
        cursorX = Math.min(cursorXcache, Math.max(content.get(cursorY).length(), 0));
    }

    public void moveCursor(int key, List<String> content, Terminal terminal, int usedRows, int columns, int targetRow, int targetCol) {
        if (content.isEmpty()) {
            return;
        }

        int prevCursorY = cursorY;

        cursorY = targetRow;
        handleCursorWrap(prevCursorY, content, columns, terminal);

        setCursorX(targetCol);
    }

    /**
     * Moves the cursor down one line.
     */
    private void moveCursorDown(int prevCursorY, List<String> content, int columns, Terminal terminal) {
        if (cursorY < content.size() - 1) {
            cursorY++;
            handleCursorWrap(prevCursorY, content, columns, terminal);
        }
    }

    /**
     * Moves the cursor up one line.
     */
    private void moveCursorUp(int prevCursorY, List<String> content, int columns, Terminal terminal) {
        if (cursorY > 0) {
            cursorY--;
            handleCursorWrap(prevCursorY, content, columns, terminal);
        }
    }

    /**
     * Moves the cursor for PAGE_DOWN key.
     */
    private void moveCursorPageDown(int prevCursorY, List<String> content, int columns, int usedRows, Terminal terminal) {
        if (offsetY + usedRows - pageWrap == content.size()) {
            cursorY = offsetY + usedRows - pageWrap - 1;
        } else {
            cursorY = offsetY + usedRows - pageWrap - PAGE_SCROLL_OFFSET;
        }
        handleCursorWrap(prevCursorY, content, columns, terminal);
    }

    /**
     * Moves the cursor for PAGE_UP key.
     */
    private void moveCursorPageUp(int prevCursorY, List<String> content, int columns, Terminal terminal) {
        if (offsetY == 0) {
            cursorY = offsetY;
        } else if (offsetY == content.size() - 1) {
            cursorY = offsetY - 1;
        } else {
            cursorY = offsetY + PAGE_SCROLL_OFFSET - 1;
        }
        handleCursorWrap(prevCursorY, content, columns, terminal);
    }

    /**
     * Moves the cursor left one character.
     */
    private void moveCursorLeft(List<String> content, Terminal terminal) {
        if (cursorX > 0) {
            int charCount = cursorX >= 2 && Character.charCount(content.get(cursorY).codePointAt(cursorX - 2)) == 2 ? 2 : 1;
            setCursorX(cursorX - charCount);
            cursorXcache = cursorX;
        } else if (cursorY > 0) {
            terminal.handleKey(TextEditor.ARROW_UP, this, content);
            terminal.handleKey(TextEditor.END, this, content);
        }
    }

    /**
     * Moves the cursor right one character.
     */
    private void moveCursorRight(List<String> content, Terminal terminal) {
        if (cursorX < content.get(cursorY).length()) {
            int codePoint = content.get(cursorY).codePointAt(cursorX);
            setCursorX(cursorX + Character.charCount(codePoint));
            cursorXcache = cursorX;
        } else if (cursorY < content.size() - 1) {
            terminal.handleKey(TextEditor.ARROW_DOWN, this, content);
            terminal.handleKey(TextEditor.HOME, this, content);
        }
    }

    /**
     * Moves the cursor for HOME key.
     */
    private void moveCursorHome() {
        setCursorX(0);
    }

    /**
     * Moves the cursor for END key.
     */
    private void moveCursorEnd(List<String> content) {
        setCursorX(content.get(cursorY).length());
    }

    /**
     * Moves the cursor when delete one character.
     */
    private void moveCursorDel(List<String> content) {
        // do nothing
    }

    /**
     * Moves the cursor for backspace key.
     */
    private void moveCursorBackspace(List<String> content, int columns, Terminal terminal) {
        if (cursorX == 0) {
            if (cursorY <= 0) {
                return;
            }
            cursorY -= 1;
            setCursorX(cursorXcache);
            cursorWrap -= getWrap(content.get(cursorY).substring(0, cursorX), columns, terminal);
        } else {
            setCursorX(Math.max(cursorX - backspaceCache, 0));
        }
    }

    /**
     * Moves the cursor when insert one character.
     */
    private void moveCursorInsertChar(List<String> content, int columns, Terminal terminal) {
        int codePoint = content.get(cursorY).codePointAt(cursorX);
        setCursorX(cursorX + Character.charCount(codePoint));
        if (cursorY > 0) {
            cursorWrap += getWrap(content.get(cursorY - 1), columns, terminal);
        }
    }

    private void moveCursorEnter(int prevCursorY, List<String> content, int columns, Terminal terminal) {
        cursorY += 1;
        setCursorX(0);
        handleCursorWrap(prevCursorY, content, columns, terminal);
    }

    /**
     * Updates cursor wrap count when scrolling pages.
     */
    private void handleCursorWrap(int prevCursorY, List<String> content, int columns, Terminal terminal) {
        int addedCursorWrap= 0;
        for (int i = cursorY; i < prevCursorY; i++) {
            addedCursorWrap -= getWrap(content.get(i), columns, terminal);
        }
        for (int i = prevCursorY; i < cursorY; i++) {
            addedCursorWrap += getWrap(content.get(i), columns, terminal);
        }
        cursorWrap += addedCursorWrap;
    }

    /**
     * Edits the content based on the key pressed (e.g., delete character).
     *
     * @param key     The key code representing the user input.
     * @param content The list of text lines in the editor.
     */
    public void editContent(int key, List<String> content, ByteBuffer byteBuffer) {
        if (content.size() <= 0) {
            return;
        }

        switch (key) {
            case TextEditor.DEL:
                editContentDel(content);
                break;
            case TextEditor.BACKSPACE:
                editContentBackspace(content);
                break;
            case TextEditor.ENTER:
                editContentEnter(content);
                break;
            case TextEditor.ARROW_DOWN:
            case TextEditor.ARROW_LEFT:
            case TextEditor.ARROW_RIGHT:
            case TextEditor.ARROW_UP:
            case TextEditor.END:
            case TextEditor.ESC:
            case TextEditor.FIND:
            case TextEditor.HOME:
            case TextEditor.PAGE_DOWN:
            case TextEditor.PAGE_UP:
                break;
            default:
                editContentInsertChar(key, content, byteBuffer);
        }
    }

    private void editContentDel(List<String> content) {
        if (cursorX < content.get(cursorY).length()) {
            content.set(cursorY, String.join("", content.get(cursorY).substring(0, cursorX), content.get(cursorY).substring(cursorX + 1)));
        } else {
            if (cursorY >= content.size() - 1) {
                return;
            }
            content.set(cursorY, String.join("", content.get(cursorY), content.get(cursorY + 1)));
            content.remove(cursorY + 1);
        }
        contentChanged = true;
    }

    private void editContentBackspace(List<String> content) {
        if (cursorX > 0) {
            backspaceCache = cursorX >= 2 && Character.charCount(content.get(cursorY).codePointAt(cursorX - 2)) == 2 ? 2 : 1;
            content.set(cursorY, String.join("", content.get(cursorY).substring(0, Math.max(cursorX - backspaceCache, 0)), content.get(cursorY).substring(cursorX)));
        } else {
            if (cursorY == 0) {
                return;
            }
            cursorXcache = content.get(cursorY - 1).length();
            backspaceCache = cursorX >= 2 && Character.charCount(content.get(cursorY).codePointAt(cursorX - 2)) == 2 ? 2 : 1;
            content.set(cursorY - 1, String.join("", content.get(cursorY - backspaceCache), content.get(cursorY)));
            content.remove(cursorY);
        }
        contentChanged = true;
    }

    private void editContentInsertChar(int key, List<String> content, ByteBuffer byteBuffer) {
        if (!Character.isISOControl(key)) {
            content.set(cursorY, String.join("", content.get(cursorY).substring(0, cursorX), new String(byteBuffer.getFilteredBuffer()), content.get(cursorY).substring(cursorX)));
            contentChanged = true;
        }
    }

    private void editContentEnter(List<String> content) {
        String line = content.get(cursorY);
        content.set(cursorY, line.substring(0, cursorX));
        content.add(cursorY + 1, line.substring(cursorX));
        contentChanged = true;
    }

    /**
     * Calculates the number of line wraps for a given line based on terminal width.
     *
     * @param line    The text line to calculate wrapping for.
     * @param columns The number of columns in the terminal.
     * @return The number of wraps needed for the line.
     */
    public int getWrap(String line, int columns, Terminal terminal) {
        return Math.max(terminal.getLineWidth(line, columns) - 1, 0) / columns;
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

    // Setters

    public void setCursorX(int x) {
        cursorX = x;
        cursorXcache = cursorX;
    }

    public void addPageWrap(int wrap) {
        pageWrap += wrap;
    }

    public void resetPageWrap() {
        pageWrap = 0;
    }

    public void resetContentChanged() {
        contentChanged = false;
    }

    public boolean isContentChanged() {
        return contentChanged;
    }
}
