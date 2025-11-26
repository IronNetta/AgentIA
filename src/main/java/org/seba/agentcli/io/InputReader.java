package org.seba.agentcli.io;

import org.seba.agentcli.tool.ToolRegistry;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enhanced input reader with auto-completion suggestions
 * Homemade implementation without external libraries
 */
@Component
public class InputReader {

    private final BufferedReader reader;
    private final CommandHistory history;
    private final ToolRegistry toolRegistry;

    public InputReader(CommandHistory history, ToolRegistry toolRegistry) {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.history = history;
        this.toolRegistry = toolRegistry;
    }

    /**
     * Reads a line with enhanced features
     */
    public String readLine(String prompt) throws IOException {
        System.out.print(prompt);
        String input = reader.readLine();

        if (input == null) {
            return null;
        }

        // Handle backspace characters that may have been received as ^H
        input = cleanInput(input);

        // Handle special history commands
        if (input.equals("!!")) {
            // Repeat last command
            if (history.size() > 0) {
                String lastCmd = history.get(history.size() - 1);
                System.out.println(AnsiColors.colorize("↻ " + lastCmd, AnsiColors.BRIGHT_BLACK));
                return lastCmd;
            }
            return "";
        }

        // Handle !N (run command N from history)
        if (input.matches("!\\d+")) {
            try {
                int index = Integer.parseInt(input.substring(1)) - 1;
                String cmd = history.get(index);
                if (cmd != null) {
                    System.out.println(AnsiColors.colorize("↻ " + cmd, AnsiColors.BRIGHT_BLACK));
                    return cmd;
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Show suggestions if incomplete command
        if (input.trim().startsWith("@") && input.length() < 20) {
            showSuggestions(input.trim());
        }

        return input;
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
     * Reads a line without any enhancements
     */
    public String readLineSimple() throws IOException {
        String input = reader.readLine();
        if (input != null) {
            input = cleanInput(input);
        }
        return input;
    }

    /**
     * Reads a multi-line input (ends with empty line)
     */
    public String readMultiLine(String prompt) throws IOException {
        System.out.println(prompt + AnsiColors.colorize(" (empty line to finish)", AnsiColors.BRIGHT_BLACK));
        StringBuilder content = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                break;
            }
            content.append(line).append("\n");
        }

        return content.toString();
    }

    /**
     * Asks a yes/no question
     */
    public boolean askYesNo(String question, boolean defaultYes) throws IOException {
        String options = defaultYes ? "[Y/n]" : "[y/N]";
        System.out.print(question + " " + AnsiColors.colorize(options, AnsiColors.BRIGHT_BLACK) + ": ");

        String input = reader.readLine();
        if (input == null || input.trim().isEmpty()) {
            return defaultYes;
        }

        String lower = input.trim().toLowerCase();
        return lower.equals("y") || lower.equals("yes") || lower.equals("o") || lower.equals("oui");
    }

    /**
     * Asks for a choice from a list
     */
    public int askChoice(String question, List<String> options) throws IOException {
        System.out.println(question);
        System.out.println();

        for (int i = 0; i < options.size(); i++) {
            System.out.println(AnsiColors.colorize((i + 1) + ". ", AnsiColors.CYAN) + options.get(i));
        }

        System.out.println();
        System.out.print("Your choice [1-" + options.size() + "]: ");

        try {
            String input = reader.readLine();
            if (input == null) {
                return -1;
            }

            int choice = Integer.parseInt(input.trim());
            if (choice >= 1 && choice <= options.size()) {
                return choice - 1;
            }
        } catch (NumberFormatException e) {
            // Invalid input
        }

        return -1;
    }

    /**
     * Shows command suggestions based on partial input
     */
    private void showSuggestions(String partial) {
        List<String> suggestions = findCompletions(partial);

        if (suggestions.isEmpty()) {
            return;
        }

        // Show up to 5 suggestions
        int count = Math.min(5, suggestions.size());
        System.out.println();
        System.out.println(AnsiColors.colorize("💡 Suggestions:", AnsiColors.BRIGHT_BLACK));

        for (int i = 0; i < count; i++) {
            String suggestion = suggestions.get(i);
            System.out.println("   " + AnsiColors.colorize(suggestion, AnsiColors.GREEN));
        }

        if (suggestions.size() > 5) {
            System.out.println(AnsiColors.colorize("   ... and " + (suggestions.size() - 5) + " more", AnsiColors.BRIGHT_BLACK));
        }

        System.out.println();
    }

    /**
     * Finds completions for partial command
     */
    private List<String> findCompletions(String partial) {
        List<String> completions = new ArrayList<>();

        if (!partial.startsWith("@")) {
            return completions;
        }

        String prefix = partial.toLowerCase();

        // Get all tool names
        List<String> toolNames = toolRegistry.getAllTools().stream()
            .map(tool -> tool.getName())
            .collect(Collectors.toList());

        // Find matching tools
        for (String toolName : toolNames) {
            if (toolName.toLowerCase().startsWith(prefix)) {
                completions.add(toolName);
            }
        }

        // Add common commands
        if ("@help".startsWith(prefix)) completions.add("@help");
        if ("@config".startsWith(prefix)) completions.add("@config");
        if ("@llm".startsWith(prefix)) completions.add("@llm");

        return completions;
    }

    /**
     * Reads a password (doesn't echo to screen if Console available)
     */
    public String readPassword(String prompt) throws IOException {
        System.out.print(prompt);

        // Try to use Console for password (no echo)
        java.io.Console console = System.console();
        if (console != null) {
            char[] pwd = console.readPassword();
            return pwd != null ? new String(pwd) : "";
        }

        // Fallback: normal read (will show characters)
        System.out.print(AnsiColors.colorize(" (warning: visible) ", AnsiColors.YELLOW));
        return reader.readLine();
    }

    /**
     * Waits for user to press Enter
     */
    public void waitForEnter(String message) throws IOException {
        System.out.print(message + AnsiColors.colorize(" (press Enter)", AnsiColors.BRIGHT_BLACK));
        reader.readLine();
    }

    /**
     * Shows completion hints for commands
     */
    public void showCompletionHints() {
        System.out.println(AnsiColors.colorize("💡 Tips:", AnsiColors.CYAN));
        System.out.println("  • Type " + AnsiColors.colorize("@", AnsiColors.GREEN) + " to see command suggestions");
        System.out.println("  • Use " + AnsiColors.colorize("!!", AnsiColors.GREEN) + " to repeat last command");
        System.out.println("  • Use " + AnsiColors.colorize("!N", AnsiColors.GREEN) + " to run command N from history");
        System.out.println("  • Type " + AnsiColors.colorize("history", AnsiColors.GREEN) + " to see recent commands");
        System.out.println();
    }

    /**
     * Closes the reader
     */
    public void close() throws IOException {
        reader.close();
    }
}
