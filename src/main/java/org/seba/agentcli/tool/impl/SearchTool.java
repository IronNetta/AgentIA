package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Smart file search with regex, filters, and preview
 * Similar to ripgrep/ag but integrated in the CLI
 */
@Component
public class SearchTool extends AbstractTool {

    private static final int DEFAULT_MAX_RESULTS = 50;
    private static final int DEFAULT_CONTEXT_LINES = 2;

    public SearchTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@search";
    }

    @Override
    public String getDescription() {
        return "Smart search in project files with regex, filters, and preview";
    }

    @Override
    public String getUsage() {
        return """
                SMART FILE SEARCH

                Usage: @search <pattern> [options]

                Options:
                  --regex, -r         Use regex pattern matching
                  --case, -c          Case-sensitive search
                  --ext <extensions>  Filter by file extensions (e.g., java,py,js)
                  --context <n>       Show n lines of context (default: 2)
                  --limit <n>         Max results to show (default: 50)
                  --files             Search in filenames instead of content

                Examples:
                  @search "TODO"                    Simple text search
                  @search "function.*export" -r     Regex search
                  @search "API" --ext java,xml      Search only in Java and XML files
                  @search "config" --files          Search in filenames
                  @search "error" --context 3       Show 3 lines of context
                """;
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args.isEmpty()) {
            return formatError("Search pattern required. Usage: @search <pattern> [options]");
        }

        try {
            SearchOptions options = parseOptions(args);

            if (options.searchInFilenames) {
                return searchFilenames(options, context);
            } else {
                return searchContent(options, context);
            }

        } catch (PatternSyntaxException e) {
            return formatError("Invalid regex pattern: " + e.getMessage());
        } catch (Exception e) {
            return formatError("Search error: " + e.getMessage());
        }
    }

    /**
     * Search in file contents
     */
    private String searchContent(SearchOptions options, ProjectContext context) throws IOException {
        List<SearchResult> results = new ArrayList<>();
        Pattern pattern = compilePattern(options);

        for (Path file : context.getSourceFiles()) {
            // Filter by extension
            if (!options.extensions.isEmpty()) {
                String ext = getFileExtension(file);
                if (!options.extensions.contains(ext)) {
                    continue;
                }
            }

            try {
                List<String> lines = Files.readAllLines(file);
                searchInFile(file, lines, pattern, options, results, context);

                if (results.size() >= options.maxResults) {
                    break;
                }
            } catch (IOException e) {
                // Skip files that can't be read
            }
        }

        return formatResults(results, options);
    }

    /**
     * Search in filenames
     */
    private String searchFilenames(SearchOptions options, ProjectContext context) {
        List<FileMatch> matches = new ArrayList<>();
        Pattern pattern = compilePattern(options);

        for (Path file : context.getSourceFiles()) {
            String filename = file.getFileName().toString();
            String relativePath = context.getRootPath().relativize(file).toString();

            Matcher matcher = pattern.matcher(options.caseSensitive ? filename : filename.toLowerCase());
            if (matcher.find()) {
                matches.add(new FileMatch(relativePath, file));

                if (matches.size() >= options.maxResults) {
                    break;
                }
            }
        }

        return formatFilenameResults(matches, options);
    }

    /**
     * Search within a single file
     */
    private void searchInFile(Path file, List<String> lines, Pattern pattern,
                              SearchOptions options, List<SearchResult> results,
                              ProjectContext context) {

        String relativePath = context.getRootPath().relativize(file).toString();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
                // Extract context lines
                List<String> contextBefore = new ArrayList<>();
                List<String> contextAfter = new ArrayList<>();

                for (int j = Math.max(0, i - options.contextLines); j < i; j++) {
                    contextBefore.add(lines.get(j));
                }

                for (int j = i + 1; j <= Math.min(lines.size() - 1, i + options.contextLines); j++) {
                    contextAfter.add(lines.get(j));
                }

                results.add(new SearchResult(
                    relativePath,
                    i + 1,
                    line,
                    contextBefore,
                    contextAfter
                ));

                if (results.size() >= options.maxResults) {
                    break;
                }
            }
        }
    }

    /**
     * Compile search pattern
     */
    private Pattern compilePattern(SearchOptions options) {
        String pattern = options.pattern;

        if (!options.useRegex) {
            // Escape special regex characters for literal search
            pattern = Pattern.quote(pattern);
        }

        int flags = options.caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        return Pattern.compile(pattern, flags);
    }

    /**
     * Format search results
     */
    private String formatResults(List<SearchResult> results, SearchOptions options) {
        if (results.isEmpty()) {
            return formatInfo("No results found for: " + options.pattern);
        }

        StringBuilder output = new StringBuilder();
        output.append(BoxDrawer.drawSeparator("SEARCH RESULTS", 70, AnsiColors.CYAN));
        output.append("\n\n");

        // Summary
        List<String[]> summary = List.of(
            new String[]{"Pattern", options.pattern},
            new String[]{"Mode", options.useRegex ? "Regex" : "Literal"},
            new String[]{"Case", options.caseSensitive ? "Sensitive" : "Insensitive"},
            new String[]{"Results", String.valueOf(results.size())}
        );

        output.append(BoxDrawer.drawInfoPanel("SEARCH SUMMARY", summary, 66));
        output.append("\n\n");

        // Group results by file
        String currentFile = null;

        for (SearchResult result : results) {
            if (!result.file.equals(currentFile)) {
                currentFile = result.file;
                output.append(AnsiColors.colorize("\n📄 " + currentFile, AnsiColors.BOLD_CYAN));
                output.append("\n");
                output.append(AnsiColors.colorize("─".repeat(70), AnsiColors.BRIGHT_BLACK));
                output.append("\n");
            }

            // Context before
            if (options.contextLines > 0 && !result.contextBefore.isEmpty()) {
                for (int i = 0; i < result.contextBefore.size(); i++) {
                    int lineNum = result.lineNumber - result.contextBefore.size() + i;
                    output.append(AnsiColors.colorize(String.format("%4d│ ", lineNum), AnsiColors.BRIGHT_BLACK));
                    output.append(AnsiColors.colorize(result.contextBefore.get(i), AnsiColors.BRIGHT_BLACK));
                    output.append("\n");
                }
            }

            // Matched line (highlighted)
            output.append(AnsiColors.colorize(String.format("%4d│ ", result.lineNumber), AnsiColors.YELLOW));
            output.append(highlightMatch(result.line, options));
            output.append("\n");

            // Context after
            if (options.contextLines > 0 && !result.contextAfter.isEmpty()) {
                for (int i = 0; i < result.contextAfter.size(); i++) {
                    int lineNum = result.lineNumber + i + 1;
                    output.append(AnsiColors.colorize(String.format("%4d│ ", lineNum), AnsiColors.BRIGHT_BLACK));
                    output.append(AnsiColors.colorize(result.contextAfter.get(i), AnsiColors.BRIGHT_BLACK));
                    output.append("\n");
                }
            }

            output.append("\n");
        }

        if (results.size() >= options.maxResults) {
            output.append(AnsiColors.warning("Result limit reached. Use --limit to see more."));
            output.append("\n");
        }

        return output.toString();
    }

    /**
     * Format filename search results
     */
    private String formatFilenameResults(List<FileMatch> matches, SearchOptions options) {
        if (matches.isEmpty()) {
            return formatInfo("No files found matching: " + options.pattern);
        }

        StringBuilder output = new StringBuilder();
        output.append(BoxDrawer.drawSeparator("FILE MATCHES", 70, AnsiColors.CYAN));
        output.append("\n\n");
        output.append(AnsiColors.colorize("Found " + matches.size() + " file(s) matching: " + options.pattern, AnsiColors.BOLD_WHITE));
        output.append("\n\n");

        for (FileMatch match : matches) {
            output.append("  📄 ").append(AnsiColors.colorize(match.relativePath, AnsiColors.CYAN));
            output.append("\n");
        }

        return output.toString();
    }

    /**
     * Highlight matched text in line
     */
    private String highlightMatch(String line, SearchOptions options) {
        try {
            Pattern pattern = compilePattern(options);
            Matcher matcher = pattern.matcher(line);
            StringBuffer highlighted = new StringBuffer();

            while (matcher.find()) {
                String match = matcher.group();
                String coloredMatch = AnsiColors.colorize(match, AnsiColors.BOLD_YELLOW);
                matcher.appendReplacement(highlighted, Matcher.quoteReplacement(coloredMatch));
            }
            matcher.appendTail(highlighted);

            return highlighted.toString();
        } catch (Exception e) {
            return line; // Fallback to unhighlighted
        }
    }

    /**
     * Parse search options from arguments
     */
    private SearchOptions parseOptions(String args) {
        SearchOptions options = new SearchOptions();
        String[] tokens = args.split("\\s+");

        int i = 0;

        // First non-option token is the pattern
        while (i < tokens.length && !tokens[i].startsWith("-")) {
            if (options.pattern.isEmpty()) {
                options.pattern = tokens[i].replaceAll("^\"|\"$", ""); // Remove quotes
            } else {
                options.pattern += " " + tokens[i];
            }
            i++;
        }

        // Parse options
        while (i < tokens.length) {
            String token = tokens[i];

            switch (token) {
                case "--regex", "-r":
                    options.useRegex = true;
                    i++;
                    break;

                case "--case", "-c":
                    options.caseSensitive = true;
                    i++;
                    break;

                case "--files":
                    options.searchInFilenames = true;
                    i++;
                    break;

                case "--ext":
                    if (i + 1 < tokens.length) {
                        String[] exts = tokens[i + 1].split(",");
                        for (String ext : exts) {
                            options.extensions.add(ext.trim());
                        }
                        i += 2;
                    } else {
                        i++;
                    }
                    break;

                case "--context":
                    if (i + 1 < tokens.length) {
                        try {
                            options.contextLines = Integer.parseInt(tokens[i + 1]);
                            i += 2;
                        } catch (NumberFormatException e) {
                            i++;
                        }
                    } else {
                        i++;
                    }
                    break;

                case "--limit":
                    if (i + 1 < tokens.length) {
                        try {
                            options.maxResults = Integer.parseInt(tokens[i + 1]);
                            i += 2;
                        } catch (NumberFormatException e) {
                            i++;
                        }
                    } else {
                        i++;
                    }
                    break;

                default:
                    i++;
                    break;
            }
        }

        return options;
    }

    private String getFileExtension(Path file) {
        String filename = file.getFileName().toString();
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    // === Data Classes ===

    private static class SearchOptions {
        String pattern = "";
        boolean useRegex = false;
        boolean caseSensitive = false;
        boolean searchInFilenames = false;
        List<String> extensions = new ArrayList<>();
        int contextLines = DEFAULT_CONTEXT_LINES;
        int maxResults = DEFAULT_MAX_RESULTS;
    }

    private static class SearchResult {
        String file;
        int lineNumber;
        String line;
        List<String> contextBefore;
        List<String> contextAfter;

        SearchResult(String file, int lineNumber, String line,
                    List<String> contextBefore, List<String> contextAfter) {
            this.file = file;
            this.lineNumber = lineNumber;
            this.line = line;
            this.contextBefore = contextBefore;
            this.contextAfter = contextAfter;
        }
    }

    private static class FileMatch {
        String relativePath;
        Path file;

        FileMatch(String relativePath, Path file) {
            this.relativePath = relativePath;
            this.file = file;
        }
    }
}
