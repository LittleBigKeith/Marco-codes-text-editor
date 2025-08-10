package com.example.texteditor;

import java.util.List;

public interface Terminal {

    /**
     * Configures the terminal to raw mode for direct keypress handling.
     */
    void enableRawMode();

    void disableRawMode();

    /**
     * Initialize terminal window size.
     */
    void initWindowSize();

    /**
     * Refreshes the terminal screen with content and cursor position.
     *
     * @param content The list of text lines to display.
     * @param cursor  The cursor object managing position and scrolling.
     */
    void refreshScreen(List<String> content, Cursor cursor);

    /**
     * Exits the editor and restores original terminal settings.
     */
    // Exits the editor and restores terminal settings
    void exit();

    /**
     * Reads a single keypress from standard input.
     *
     * @return The key code or -1 if an error occurs.
     */
    int getKey();

    /**
     * Processes user keypresses to update cursor and content.
     *
     * @param keyPressed The key code to process.
     * @param cursor     The cursor object managing position and scrolling.
     * @param content    The list of text lines in the editor.
     */
    void handleKey(int keyPressed, TextEditor textEditor, Cursor cursor, List<String> content);

    default void handleTerminalAction(int keyPressed) {
        switch (keyPressed) {
            case 'q':
                exit();
                break;
        }
    }

    // Getters
    int getRows();
    int getUsedRows();
    int getColumns();

}