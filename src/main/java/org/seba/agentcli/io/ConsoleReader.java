package org.seba.agentcli.io;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple console reader - No external dependencies!
 * Homemade replacement for JLine
 */
@Component
public class ConsoleReader {

    private final BufferedReader reader;
    private final List<String> history;
    private static final int MAX_HISTORY = 100;

    public ConsoleReader() {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.history = new ArrayList<>();
    }

    /**
     * Read a line from the console with a prompt
     */
    public String readLine(String prompt) {
        try {
            System.out.print(prompt);
            System.out.flush();

            String line = reader.readLine();

            if (line != null) {
                // Handle backspace characters that may have been received as ^H
                line = cleanInput(line);

                if (!line.trim().isEmpty()) {
                    addToHistory(line);
                }
            }

            return line;
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
            return null;
        }
    }

    /**
     * Read a line with masked input (for passwords)
     */
    public String readLine(String prompt, char mask) {
        // On Java 17, we can use Console for masking
        java.io.Console console = System.console();

        if (console != null) {
            char[] password = console.readPassword(prompt);
            return password != null ? new String(password) : null;
        } else {
            // Fallback: no masking if no console available
            System.out.print(prompt);
            return readLine("");
        }
    }

    /**
     * Clean input string by handling backspace characters and other control characters
     */
    private String cleanInput(String input) {
        // Handle backspace characters that may have been received as ^H
        if (input.contains("^H") || input.contains("\b")) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (c == '^' && i + 1 < input.length() && input.charAt(i + 1) == 'H') {
                    // Handle ^H (backspace sequence)
                    if (result.length() > 0) {
                        result.deleteCharAt(result.length() - 1);
                    }
                    i++; // Skip the next character 'H'
                } else if (c == '\b') {
                    // Handle \b (backspace character)
                    if (result.length() > 0) {
                        result.deleteCharAt(result.length() - 1);
                    }
                } else {
                    result.append(c);
                }
            }
            return result.toString();
        }
        return input;
    }

    /**
     * Add command to history
     */
    private void addToHistory(String command) {
        history.add(command);

        // Keep history size limited
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    /**
     * Get command history
     */
    public List<String> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * Clear history
     */
    public void clearHistory() {
        history.clear();
    }

    /**
     * Close the reader
     */
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            // Ignore
        }
    }
}
