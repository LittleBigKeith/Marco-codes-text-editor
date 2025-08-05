package com.example.texteditor;

import java.util.Arrays;
import java.util.List;

/**
 * Main class for a terminal-based text editor.
 * Coordinates file handling, terminal operations, and cursor movement.
 */
public class TextEditor {
    
    public static final int ARROW_UP = 1000;
    public static final int ARROW_DOWN = 1001;
    public static final int ARROW_LEFT = 1002;
    public static final int ARROW_RIGHT = 1003;
    public static final int PAGE_UP = 1004;
    public static final int PAGE_DOWN = 1005;
    public static final int HOME = 1006;
    public static final int END = 1007;
    public static final int DEL = 1008;
    
    private final Terminal terminal;
    private final FileHandler fileHandler;
    private final Cursor cursor;
    private List<String> content;
    private int keyPressed;

    /**
     * Constructs a new TextEditor instance.
     */
    public TextEditor() {
        this.terminal = new Terminal();
        this.fileHandler = new FileHandler();
        this.cursor = new Cursor();
        this.content = Arrays.asList();
    }

    /**
     * Main entry point for the text editor.
     *
     * @param args Command-line arguments containing the filename.
     */
    public static void main(String[] args) {
        TextEditor editor = new TextEditor();
        editor.run(args);
    }

    /**
     * Runs the main editor loop, handling file loading, terminal setup, and user input.
     *
     * @param args Command-line arguments containing the filename.
     */
    public void run (String[] args) {
        fileHandler.openFile(args);
        content = fileHandler.getContent();
        // Enable raw mode to capture keypresses directly without buffering
        terminal.enableRawMode();
        terminal.initWindowSize();

        while (true) {
            cursor.scroll(keyPressed, content, terminal.getRows(), terminal.getUsedRows(), terminal.getColumns());
            terminal.refreshScreen(content, cursor);
            keyPressed = terminal.getKey();
            terminal.handleKey(keyPressed, cursor, content);
        }     
    }
}