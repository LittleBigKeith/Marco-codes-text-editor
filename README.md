# Simple Terminal-Based Text Editor

A lightweight, Vim-inspired text editor for viewing text files in the terminal.

## Prerequisites
- Java (version 1.8 or higher)
- Maven

## Running the Editor
Run the editor with:
```bash
mvn exec:java -Dexec.args="filename"
```

## Features

Line-based navigation: View text files with a Vim-like interface.
Scroll vertically: Arrow-up and arrow-down keys (supports text wrapping), page-up and page-down.
Scroll horizontally: Use arrow-left and arrow-right keys
Del: delete one character at a time
Exit: Press q to quit.

## Planned Features

- Add basic editing and saving capabilities.
- Add display tab as a character capability.

## Notes

The editor currently functions as a read-only viewer.
Ensure the specified file exists; otherwise, an error message will be displayed.
