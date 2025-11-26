package org.seba.agentcli.io;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Command history manager with persistence
 * Stores history in ~/.agentcli/history.txt
 */
@Component
public class CommandHistory {

    private static final int MAX_HISTORY_SIZE = 1000;
    private static final String HISTORY_FILE = ".agentcli/history.txt";

    private final List<String> history = new ArrayList<>();
    private final Path historyPath;

    public CommandHistory() {
        String home = System.getProperty("user.home");
        this.historyPath = Paths.get(home, HISTORY_FILE);
        loadHistory();
    }

    /**
     * Adds a command to history
     */
    public void add(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }

        String trimmed = command.trim();

        // Don't add duplicates of the last command
        if (!history.isEmpty() && history.get(history.size() - 1).equals(trimmed)) {
            return;
        }

        // Don't store sensitive commands
        if (isSensitive(trimmed)) {
            return;
        }

        history.add(trimmed);

        // Trim if too large
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }

        saveHistory();
    }

    /**
     * Gets command at index (0 = oldest, size-1 = newest)
     */
    public String get(int index) {
        if (index < 0 || index >= history.size()) {
            return null;
        }
        return history.get(index);
    }

    /**
     * Gets the last N commands
     */
    public List<String> getRecent(int count) {
        if (history.isEmpty()) {
            return Collections.emptyList();
        }

        int start = Math.max(0, history.size() - count);
        return new ArrayList<>(history.subList(start, history.size()));
    }

    /**
     * Gets all history
     */
    public List<String> getAll() {
        return new ArrayList<>(history);
    }

    /**
     * Gets history size
     */
    public int size() {
        return history.size();
    }

    /**
     * Clears all history
     */
    public void clear() {
        history.clear();
        saveHistory();
    }

    /**
     * Searches history for commands containing pattern
     */
    public List<String> search(String pattern) {
        String lowerPattern = pattern.toLowerCase();
        List<String> results = new ArrayList<>();

        for (String cmd : history) {
            if (cmd.toLowerCase().contains(lowerPattern)) {
                results.add(cmd);
            }
        }

        return results;
    }

    /**
     * Gets the previous command relative to current index
     */
    public String getPrevious(int currentIndex) {
        if (currentIndex <= 0 || history.isEmpty()) {
            return null;
        }
        return history.get(Math.max(0, currentIndex - 1));
    }

    /**
     * Gets the next command relative to current index
     */
    public String getNext(int currentIndex) {
        if (currentIndex >= history.size() - 1) {
            return null;
        }
        return history.get(currentIndex + 1);
    }

    /**
     * Loads history from file
     */
    private void loadHistory() {
        try {
            if (!Files.exists(historyPath)) {
                // Create parent directory if needed
                Path parent = historyPath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                return;
            }

            List<String> lines = Files.readAllLines(historyPath);
            history.addAll(lines);

            // Trim if too large
            while (history.size() > MAX_HISTORY_SIZE) {
                history.remove(0);
            }

        } catch (IOException e) {
            System.err.println("Warning: Could not load command history: " + e.getMessage());
        }
    }

    /**
     * Saves history to file
     */
    private void saveHistory() {
        try {
            // Create parent directory if needed
            Path parent = historyPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.write(historyPath, history);

        } catch (IOException e) {
            System.err.println("Warning: Could not save command history: " + e.getMessage());
        }
    }

    /**
     * Checks if command contains sensitive data
     */
    private boolean isSensitive(String command) {
        String lower = command.toLowerCase();
        return lower.contains("api-key") ||
               lower.contains("password") ||
               lower.contains("token") ||
               lower.contains("secret");
    }

    /**
     * Formats history for display
     */
    public String formatHistory(int count) {
        List<String> recent = getRecent(count);
        if (recent.isEmpty()) {
            return AnsiColors.info("No command history");
        }

        StringBuilder output = new StringBuilder();
        output.append(AnsiColors.colorize("RECENT COMMANDS", AnsiColors.BOLD_WHITE));
        output.append("\n\n");

        int startIndex = Math.max(0, history.size() - count);
        for (int i = 0; i < recent.size(); i++) {
            int actualIndex = startIndex + i;
            output.append(AnsiColors.colorize(String.format("%4d  ", actualIndex + 1), AnsiColors.BRIGHT_BLACK));
            output.append(recent.get(i));
            output.append("\n");
        }

        return output.toString();
    }

    /**
     * Exports history to a file
     */
    public void export(Path destination) throws IOException {
        Files.write(destination, history);
    }

    /**
     * Imports history from a file
     */
    public void importHistory(Path source) throws IOException {
        List<String> imported = Files.readAllLines(source);
        for (String cmd : imported) {
            add(cmd);
        }
    }
}
