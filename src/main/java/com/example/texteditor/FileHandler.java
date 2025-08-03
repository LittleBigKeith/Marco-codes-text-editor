package com.example.texteditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Manages file operations
public class FileHandler {

    private static List<String> content = Arrays.asList();

    // Opens and reads the file provided as an argument
    public void openFile(String[] args) {
        if (args.length == 1) {
            
            String filename = args[0];
            filename = filename.replace("~", System.getProperty("user.home"));
            Path path = Paths.get(filename);

            if (Files.exists(path)) {
                try (Stream<String> stream = Files.lines(path)){
                    content = stream.collect(Collectors.toList());
                } catch (IOException e) {
                    System.err.println("Files.lines threw an exception: " + e);
                }
            }
        } else {
            System.out.println("Usage: mvn exec:java -Dexec.args=\"<filename>\"");
        }
    }

    public List<String> getContent() {
        return content;
    }
}