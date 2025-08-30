package com.example.texteditor;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import com.sun.jna.Platform;

/**
 * Main class for a terminal-based text editor.
 * Coordinates file handling, terminal operations, and cursor movement.
 */
public class TextEditor {

    public static final int ENTER = 13;
    public static final int ESC = 27;
    public static final int BACKSPACE = 127;
    public static final int ARROW_UP = 1000;
    public static final int ARROW_DOWN = 1001;
    public static final int ARROW_LEFT = 1002;
    public static final int ARROW_RIGHT = 1003;
    public static final int PAGE_UP = 1004;
    public static final int PAGE_DOWN = 1005;
    public static final int HOME = 1006;
    public static final int END = 1007;
    public static final int DEL = 1008;
    public static final int FIND = 1009;

    private final Terminal terminal;
    private final FileHandler fileHandler;
    private final Cursor cursor;
    private List<String> content;
    private int keyPressed;

    public static final String DEFAULT_FIND_PROMPT = "Find %s (use Arrow/Enter/ESC)";
    private enum SearchDir {
        FORWARD, BACKWRAD;
    }
    boolean matchFound = false;
    int matchX = 0, matchY = 0;

    /**
     * Constructs a new TextEditor instance.
     */
    public TextEditor() {
        this.terminal = (Platform.isMac() || Platform.isLinux()) ? new UnixBasedTerminal() : new WindowsTerminal();
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
        terminal.setLocale();
        
        while (true) {
            terminal.refreshScreen(content, cursor);
            keyPressed = terminal.getKey();
            terminal.updateStatusBarMessage("", cursor, content);
            keyPressed = handleActions(keyPressed);
            terminal.handleKey(keyPressed, cursor, content);
        }    
    }

    /**
     * Handles specific key actions like find (Ctrl+F) or quit (Ctrl+Q).
     *
     * @param keyPressed The key code of the pressed key.
     */
    private int handleActions(int keyPressed) {
        if (keyPressed == ctrl('f')) {
            find((defaultMsg, userMsg) -> {
                StringBuilder builder = new StringBuilder();
                builder.append(userMsg.isEmpty() ? defaultMsg : userMsg);
                terminal.updateStatusBarMessage(builder.toString(), cursor, content);
            }, terminal.getByteBuffer());
        } else if (keyPressed == ctrl('q')) {
            if (!cursor.isContentChanged()) {
                terminal.exit();
            } else {
                terminal.updateStatusBarMessage("Cannot quit with unsaved changes [Ctrl+s to save]", cursor, content, 31);
            }
        } else if (keyPressed == ctrl('s')) {
            fileHandler.saveFile(cursor, terminal);
            cursor.resetContentChanged();
        } else if (keyPressed == ctrl('h')) {
            keyPressed = TextEditor.BACKSPACE;
        } else if (keyPressed == ctrl('v')) {
            
        }
        return keyPressed;
    }

    /**
     * Implements the find functionality, allowing the user to input a search string
     * and navigate matches using arrow keys or Enter.
     *
     * @param prompt BiConsumer to update the status bar with the search prompt.
     */
    private void find(BiConsumer<String, String> prompt, ByteBuffer byteBuffer) {
        StringBuilder builder = new StringBuilder();

        while (true) {
            prompt.accept(DEFAULT_FIND_PROMPT, builder.toString());
            int keyRead = terminal.getKey();
            switch (keyRead) {
                case TextEditor.BACKSPACE:
                case TextEditor.DEL:
                    builder.setLength(Math.max(builder.length() - 1, 0));
                    findStringInText(builder);
                    break;
                case TextEditor.ENTER:
                case TextEditor.ARROW_DOWN:
                case TextEditor.ARROW_RIGHT:
                    findNext(SearchDir.FORWARD, builder);
                    break;
                case TextEditor.ARROW_UP:
                case TextEditor.ARROW_LEFT:
                    findNext(SearchDir.BACKWRAD, builder);
                    break;
                case TextEditor.ESC:
                    escapeFind(builder);
                    return;
                case TextEditor.HOME:
                case TextEditor.END:
                case TextEditor.PAGE_UP:
                case TextEditor.PAGE_DOWN:
                case TextEditor.FIND:
                    break;
                default:
                    if (keyRead == ctrl('q')) {
                        escapeFind(builder);
                        return;
                    } else if (!Character.isISOControl(keyRead)) {
                        builder.append(new String(byteBuffer.getFilteredBuffer()));
                        findStringInText(builder);
                    } 
            }
        }
    }

    /**
     * Searches for a string in the content and updates cursor if a match is found.
     *
     * @param builder The search string to find.
     */
    private void findStringInText (StringBuilder builder) {
        matchFound = false;
        String searchString = builder.toString();
        int i = 0;
        while (i < content.size()) {
            String contentLine = content.get(i);
            if (contentLine.contains(searchString)) {
                matchFound = true;
                matchX = contentLine.indexOf(searchString);
                matchY = i;
                terminal.handleKey(TextEditor.FIND, cursor, content, matchY, matchX);
                terminal.refreshScreen(content, cursor);
                break;
            }
            i++;
        }
    }

    /**
     * Finds the next or previous match of the search string in the specified direction.
     *
     * @param dir The search direction (FORWARD or BACKWRAD).
     * @param builder The search string to find.
     */
    private void findNext(SearchDir dir, StringBuilder builder) {
        String searchString = builder.toString();
        if (!matchFound || content.size() <= 0 || searchString.length() <= 0) {
            return;
        }
        int startY = matchY;
        int searchY = startY;
        int startX = matchX;
        int searchX = startX + (dir == SearchDir.FORWARD ? searchString.length() : -1);

        while (searchY != startY || searchX != startX) {
            String contentLine = (dir == SearchDir.FORWARD ? content.get(searchY).substring(searchX) : content.get(searchY).substring(0, searchX + 1));
            int nextMatch = (dir == SearchDir.FORWARD ? contentLine.indexOf(searchString) : contentLine.lastIndexOf(searchString));

            if (nextMatch != -1) {
                matchY = searchY;
                matchX = (dir == SearchDir.FORWARD? searchX + nextMatch : nextMatch);
                terminal.handleKey(TextEditor.FIND, cursor, content, matchY, matchX);
                terminal.refreshScreen(content, cursor);
                break;
            } else {
                searchY =  wrapSearchY(searchY + (dir == SearchDir.FORWARD ? 1 : -1));
                searchX = (dir == SearchDir.FORWARD ? 0 : content.get(searchY).length() - 1);
            }
        }
    }

    /**
     * Wraps the search line index to loop around content boundaries.
     *
     * @param searchY The current line index.
     * @return The wrapped line index.
     */
    private int wrapSearchY(int searchY) {
        if (searchY >= content.size()) {
            searchY = 0;
        } else if (searchY < 0) {
            searchY = content.size() - 1;
        }
        return searchY;
    }

    /**
     * Clears the search string and updates the status bar when exiting find mode.
     *
     * @param builder The search string to clear.
     */
    private void escapeFind(StringBuilder builder) {
        builder.setLength(0);
        terminal.updateStatusBarMessage(builder.toString(), cursor, content);
    }

    /**
     * Converts a key to its Ctrl-modified equivalent (e.g., Ctrl+F).
     *
     * @param key The key code to modify.
     * @return The Ctrl-modified key code.
     */
    private static int ctrl(int key){
        return key & 0x1f;
    }
}