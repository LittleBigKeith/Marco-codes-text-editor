package com.example.texteditor;

import java.util.List;

// Manages cursor position and scrolling
public class Cursor {

    private static int cursorX = 0, cursorY = 0;
    private static int offsetX = 0, offsetY = 0;
    private static int cursorWrap = 0, pageWrap = 0, hiddenWrap = 0, hiddenWrapCooldown = 0;

    // Handles scrolling based on cursor movement
    public void scroll(int keyPressed, List<String> content, int rows, int columns) {
        switch(keyPressed) {
            case TextEditor.ARROW_DOWN:
                if (cursorY - rows + pageWrap - 1 >= 0 && offsetY - pageWrap + rows - 1 < cursorY) {
                    int newContentWrap = getWrap(content.get(cursorY), columns);
                    int newHiddenWrap = getWrap(content.get(offsetY), columns);
                    int tempWrap = 0;
                    for (int maxIter = newContentWrap; maxIter > 0; maxIter--) {
                        newHiddenWrap += getWrap(content.get(offsetY + tempWrap + 1), columns);
                        tempWrap++;
                        if (newHiddenWrap >= newContentWrap) {
                            break;
                        }
                    }
                    if (hiddenWrapCooldown <= 0) {
                        offsetY = offsetY + 1 + tempWrap;
                        hiddenWrap += newHiddenWrap;
                        hiddenWrapCooldown = newHiddenWrap;
                    } else if (hiddenWrapCooldown > 0) {
                        if (cursorY + cursorWrap + getWrap(content.get(cursorY), columns) > offsetY + hiddenWrap + rows) {
                            offsetY = Math.min(offsetY + 1 + tempWrap, content.size());
                            hiddenWrap += newHiddenWrap;
                            hiddenWrapCooldown = newHiddenWrap;
                        } else {
                            hiddenWrapCooldown--;
                        }
                    }
                }
                break;
            case TextEditor.ARROW_UP:
                if (cursorY < offsetY) {
                    offsetY = Math.max(offsetY - 1, 0);
                    hiddenWrap -= getWrap(content.get(cursorY), columns);
                }
                break;
            case TextEditor.PAGE_DOWN:
                /* TODO: implement page down */
                break;
            case TextEditor.PAGE_UP:
                /* TODO: implement page up */
                break;
            default:
        }
    }

    // Moves the cursor based on keypress
    public void moveCursor(int key, List<String> content, int columns) {
        switch (key) {
            case TextEditor.ARROW_UP:
                if (cursorY > 0) {
                    cursorY--;
                    cursorWrap -= getWrap(content.get(cursorY), columns);
                }
                break;
            case TextEditor.ARROW_DOWN:
                if (cursorY < content.size() - 1) {
                    cursorWrap += getWrap(content.get(cursorY), columns);
                    cursorY++;
                }
                break;
            case TextEditor.PAGE_DOWN:
                /* TODO: implement page down */
                break;
            case TextEditor.PAGE_UP:
                /* TODO: implement page up */
                break;
            case TextEditor.ARROW_LEFT:
                /* TODO: implement cursorX limit and wrapping logic */
                if (cursorX > 0) {
                    cursorX--;
                }
                break;
            case TextEditor.ARROW_RIGHT:
                /* TODO: implement cursorX limit and wrapping logic */
                if (cursorX < columns - 1) {
                    cursorX++;
                }
                break;
            case TextEditor.HOME:
                /* TODO: implement cursorX limit and wrapping logic */
                cursorX = 0;
                break;
            case TextEditor.END:
                /* TODO: implement cursorX limit and wrapping logic */
                cursorX = columns - 1;
                break;
            case TextEditor.DEL:
                /* TODO: implemente delete */
                break;
        }
    }

    // Change content of the file based on keypress
    public void editContent(int key) {
        switch (key) {
            case TextEditor.DEL:
                /* TODO: implemente delete */
                break;
        }
    }

    // Calculates line wrapping based on terminal width
    public int getWrap(String line, int columns) {
        return Math.max(line.length() - 1, 0) / columns;
    }

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

    public void addPageWrap(int wrap) {
        pageWrap += wrap;
    }

    public void resetPageWrap() {
        pageWrap = 0;
    }

    public int getHiddenWrap() {
        return hiddenWrap;
    }

    public int getHiddenWrapCooldown() {
        return hiddenWrapCooldown;
    }
}
