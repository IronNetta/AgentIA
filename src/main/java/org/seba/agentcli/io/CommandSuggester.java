package org.seba.agentcli.io;

import org.seba.agentcli.tool.Tool;
import org.seba.agentcli.tool.ToolRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Suggests similar commands when user makes typos
 * Uses Levenshtein distance algorithm
 */
@Component
public class CommandSuggester {

    private final ToolRegistry toolRegistry;
    private static final int MAX_SUGGESTIONS = 3;
    private static final int MAX_DISTANCE = 3; // Maximum edit distance to consider

    public CommandSuggester(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * Finds similar commands for an unknown command
     */
    public List<String> findSimilar(String unknownCommand) {
        if (unknownCommand == null || unknownCommand.isEmpty()) {
            return List.of();
        }

        String input = unknownCommand.trim().toLowerCase();

        // Get all available commands
        List<String> allCommands = new ArrayList<>();

        // Add tool commands
        for (Tool tool : toolRegistry.getAllTools()) {
            allCommands.add(tool.getName().toLowerCase());
        }

        // Add common commands
        allCommands.add("clear");
        allCommands.add("exit");
        allCommands.add("quit");
        allCommands.add("history");
        allCommands.add("help");

        // Calculate distances
        List<CommandMatch> matches = new ArrayList<>();
        for (String command : allCommands) {
            int distance = levenshteinDistance(input, command);
            if (distance <= MAX_DISTANCE) {
                matches.add(new CommandMatch(command, distance));
            }
        }

        // Sort by distance (closest first)
        matches.sort((a, b) -> Integer.compare(a.distance, b.distance));

        // Return top N suggestions
        return matches.stream()
            .limit(MAX_SUGGESTIONS)
            .map(m -> m.command)
            .collect(Collectors.toList());
    }

    /**
     * Formats suggestions as a user-friendly message
     */
    public String formatSuggestions(String unknownCommand) {
        List<String> suggestions = findSimilar(unknownCommand);

        if (suggestions.isEmpty()) {
            return AnsiColors.error("Unknown command: " + unknownCommand) + "\n" +
                   AnsiColors.info("Type @help to see available commands");
        }

        StringBuilder output = new StringBuilder();
        output.append(AnsiColors.error("Unknown command: " + unknownCommand)).append("\n\n");
        output.append(AnsiColors.colorize("Did you mean:", AnsiColors.BRIGHT_BLACK)).append("\n");

        for (String suggestion : suggestions) {
            output.append("  • ").append(AnsiColors.colorize(suggestion, AnsiColors.GREEN)).append("\n");
        }

        output.append("\n");
        output.append(AnsiColors.info("Type @help to see all commands"));

        return output.toString();
    }

    /**
     * Checks if a command is valid
     */
    public boolean isValidCommand(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }

        String trimmed = command.trim();

        // Check built-in commands
        if (trimmed.equalsIgnoreCase("clear") ||
            trimmed.equalsIgnoreCase("exit") ||
            trimmed.equalsIgnoreCase("quit") ||
            trimmed.equalsIgnoreCase("history")) {
            return true;
        }

        // Check tool commands
        if (trimmed.startsWith("@")) {
            String commandName = trimmed.split("\\s+")[0];
            return toolRegistry.getAllTools().stream()
                .anyMatch(tool -> tool.getName().equalsIgnoreCase(commandName));
        }

        // Free-form queries are valid
        return true;
    }

    /**
     * Calculates Levenshtein distance between two strings
     * (minimum number of edits to transform one string into another)
     */
    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(
                        dp[i - 1][j],      // deletion
                        Math.min(
                            dp[i][j - 1],  // insertion
                            dp[i - 1][j - 1] // substitution
                        )
                    );
                }
            }
        }

        return dp[len1][len2];
    }

    /**
     * Helper class to store command matches with distances
     */
    private static class CommandMatch {
        final String command;
        final int distance;

        CommandMatch(String command, int distance) {
            this.command = command;
            this.distance = distance;
        }
    }

    /**
     * Suggests command completions (for partial commands)
     */
    public List<String> suggestCompletions(String partial) {
        if (partial == null || partial.isEmpty() || !partial.startsWith("@")) {
            return List.of();
        }

        String prefix = partial.toLowerCase();
        List<String> completions = new ArrayList<>();

        for (Tool tool : toolRegistry.getAllTools()) {
            String toolName = tool.getName().toLowerCase();
            if (toolName.startsWith(prefix)) {
                completions.add(tool.getName());
            }
        }

        return completions.stream()
            .limit(MAX_SUGGESTIONS)
            .collect(Collectors.toList());
    }

    /**
     * Gets help for common mistakes
     */
    public String getCommonMistakeHelp(String command) {
        String lower = command.toLowerCase().trim();

        if (lower.equals("ls") || lower.equals("dir")) {
            return AnsiColors.info("Use @tree to see project structure");
        }

        if (lower.equals("cat") || lower.equals("type")) {
            return AnsiColors.info("Use @file <path> to read files");
        }

        if (lower.equals("grep") || lower.equals("find")) {
            return AnsiColors.info("Use @search <term> to search in files");
        }

        if (lower.equals("git")) {
            return AnsiColors.info("Use @git <command> for Git operations");
        }

        if (lower.equals("test") || lower.equals("run")) {
            return AnsiColors.info("Use @execute test or @execute run");
        }

        return null;
    }
}
