package com.example.texteditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages file operations for the text editor, including opening and reading files.
 */
public class FileHandler {

    private Path path;
    private List<String> content;

    /**
     * Constructs a new FileHandler with an empty content list.
     */
    public FileHandler() {
        this.content = new ArrayList<>();
    }

    /**
     * Opens and reads the file specified in the arguments.
     * If no valid file is provided, prints usage instructions.
     *
     * @param args Command-line arguments containing the filename.
     * @throws IllegalArgumentException if the filename is invalid or not provided.
     */
    public void openFile(String[] args) {
        if (args.length > 1) {
            System.out.println("Usage: mvn exec:java [-Dexec.args=\"<filename>\"]");
            return;
        }

        getPath(args);
        
        if (args.length == 1) {
            readFile();
        } else {
            content = new ArrayList<>();
            content.add("");
        }
    }

    private void getPath(String[] args) {
        String filename = args.length == 1 ? args[0].replace("~", System.getProperty("user.home")) : 
                                             createFileName();
        path = Paths.get(filename);
    }

    private void readFile() {
        if (!Files.exists(path)) {
            System.err.println("Error: File '" + path.normalize().toString() + "' does not exist.");
            System.exit(-1);
        }

        try (Stream<String> stream = Files.lines(path)){
            content = stream.collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading file '" + path.normalize().toString() + "': " + e.getMessage());
        }
    }

    private String createFileName() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        return now.format(formatter) + ".txt";
    }
    
    public void saveFile(Cursor cursor, Terminal terminal) {
        try {
            Files.write(path, content);
            terminal.updateStatusBarMessage("Saved file successfully!", cursor, content, 34);
        } catch (IOException e) {
            System.err.println("Error saving file '" + path.normalize().toString() + "': " + e.getMessage());
        }
    }

    /**
     * Gets the content of the opened file.
     *
     * @return A list of strings representing the file's lines.
     */
    public List<String> getContent() {
        return content;
    }
}