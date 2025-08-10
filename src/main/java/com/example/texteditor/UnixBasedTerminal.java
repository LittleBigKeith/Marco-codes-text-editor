package com.example.texteditor;

/**
 * Handles terminal operations, including raw mode configuration, screen rendering, and keypress handling.
 */
public class UnixBasedTerminal extends IOHandler implements Terminal {

    private static LibC.Termios originalAttributes; // Original terminal attributes to restore on exit

    /**
     * Configures the terminal to raw mode for direct keypress handling.
     */
    @Override
    public void enableRawMode() {
        // Create a new Termios structure to store terminal settings
        LibC.Termios termios = new LibC.Termios();

        // Get current terminal attributes
        int rc = LibC.INSTANCE.tcgetattr(LibC.SYSTEM_OUT_FD, termios);
        if (rc != 0) {
            System.err.println("An error occured while calling tcgetattr: " + rc);
            System.exit(rc);
        }
        // Save a copy of the original terminal attributes
        originalAttributes = LibC.Termios.t_copy(termios);
        // Disable:
        // - ECHO: echoing input characters
        // - ICANON: canonical mode (line buffering)
        // - IEXTEN: extended input processing
        // - ISIG: signal-generating characters (e.g., Ctrl+C)
        termios.c_lflag &= ~(LibC.ECHO | LibC.ICANON | LibC.IEXTEN | LibC.ISIG);
        // Disable input processing:
        // - IXON: software flow control (Ctrl+S, Ctrl+Q)
        // - ICRNL: carriage return to newline conversion
        termios.c_iflag &= ~(LibC.IXON | LibC.ICRNL);
        // Disable output processing (e.g., newline conversions)
        termios.c_oflag &= ~(LibC.OPOST);

        // Apply the modified terminal settings
        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, termios);
    }
    
    public void disableRawMode() {
        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, originalAttributes);  // Restore original terminal attributes before exiting
    }

    /**
     * Initialize terminal window size.
     */ 
    @Override
    public void initWindowSize() {
        LibC.Winsize winSize = getWinSize();
        setRows(winSize.ws_row - 2);
        setColumns(winSize.ws_col);
    }

    /**
     * Retrieves the terminal window size using ioctl.
     * 
     * @return The Winsize structure containing terminal dimensions.
     */
    private static LibC.Winsize getWinSize() {
        LibC.Winsize winSize = new LibC.Winsize();
        int rc = LibC.INSTANCE.ioctl(LibC.SYSTEM_OUT_FD, LibC.TIOCGWINSZ, winSize);
        if (rc != 0) {
            System.err.println("An error occured while calling ioctl: " + rc);
            System.exit(rc);
        }
        return winSize;
    }
    
    /**
     * Exits the editor and restores original terminal settings.
     */
    // Exits the editor and restores terminal settings
    @Override
    public void exit() {
        System.out.print("\033[2J");    // Clear the screen
        System.out.print("\033[H");     // Move cursor to top-left
        disableRawMode();
        System.exit(0);
    }
}