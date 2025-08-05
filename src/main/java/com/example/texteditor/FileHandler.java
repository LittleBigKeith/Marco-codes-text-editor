package com.example.texteditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages file operations for the text editor, including opening and reading files.
 */
public class FileHandler {

    private List<String> content = Arrays.asList();

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
        if (args.length != 1) {
            System.out.println("Usage: mvn exec:java -Dexec.args=\"<filename>\"");
            return;
        }
            
        String filename = args[0].replace("~", System.getProperty("user.home"));;
        Path path = Paths.get(filename);

        if (!Files.exists(path)) {
            System.err.println("Error: File '" + filename + "' does not exist.");
            return;
        }

        try (Stream<String> stream = Files.lines(path)){
            content = stream.collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading file '" + filename + "': " + e.getMessage());
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