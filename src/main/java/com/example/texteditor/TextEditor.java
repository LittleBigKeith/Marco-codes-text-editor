package com.example.texteditor;

import java.util.Arrays;
import java.util.List;

// Main class for a simple terminal-based text editor
public class TextEditor {
    
    static final int ARROW_UP = 1000;
    static final int ARROW_DOWN = 1001;
    static final int ARROW_LEFT = 1002;
    static final int ARROW_RIGHT = 1003;
    static final int PAGE_UP = 1004, PAGE_DOWN = 1005, HOME = 1006, END = 1007, DEL = 1008;
    
    private final Terminal terminal;
    private final FileHandler fileHandler;
    private final Cursor cursor;

    private List<String> content;
    private int keyPressed;

    public TextEditor() {
        this.terminal = new Terminal();
        this.fileHandler = new FileHandler();
        this.cursor = new Cursor();
        this.content = Arrays.asList();
    }
    public static void main(String[] args) {
        TextEditor editor = new TextEditor();
        editor.run(args);
    }

    // Runs the main editor loop
    public void run (String[] args) {
        fileHandler.openFile(args);
        content = fileHandler.getContent();
        // Enable raw mode to capture keypresses directly without buffering
        terminal.enableRawMode();
        terminal.initWindowSize();

        // Main loop to continuously refresh screen and handle input
        while (true) {
            cursor.scroll(keyPressed, content, terminal.getRows(), terminal.getColumns());
            // Clear and refresh the terminal screen
            terminal.refreshScreen(content, cursor);
            // Read a single keypress from the user
            keyPressed = terminal.getKey();
            // Process the keypress (e.g., exit on 'q')
            terminal.handleKey(keyPressed, cursor, content);
        }
        
    }
}