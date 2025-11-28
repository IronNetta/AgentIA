package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Git integration tool for version control operations
 * Provides safe git commands with validation and formatting
 */
@Component
public class GitTool extends AbstractTool {

    public GitTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@git";
    }

    @Override
    public String getDescription() {
        return "Git version control operations";
    }

    @Override
    public String getUsage() {
        return """
                GIT INTEGRATION

                Usage: @git <command> [args]

                Commands:
                  status              Show working tree status
                  diff [file]         Show changes (optionally for specific file)
                  log [n]            Show commit history (last n commits, default 10)
                  branch             List branches
                  staged             Show staged changes
                  unstaged           Show unstaged changes
                  files              List tracked files
                  blame <file>       Show who changed each line

                Examples:
                  @git status
                  @git diff src/Main.java
                  @git log 5
                  @git blame README.md

                Note: This tool is read-only. Use your terminal for commits/pushes.
                """;
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (!isGitRepository()) {
            return formatError("Not a git repository. Initialize with: git init");
        }

        String[] parts = args.trim().split("\\s+", 2);
        String command = parts.length > 0 ? parts[0].toLowerCase() : "";
        String subArgs = parts.length > 1 ? parts[1] : "";

        return switch (command) {
            case "status" -> showStatus();
            case "diff" -> showDiff(subArgs);
            case "log" -> showLog(subArgs);
            case "branch" -> showBranches();
            case "staged" -> showStagedChanges();
            case "unstaged" -> showUnstagedChanges();
            case "files" -> showTrackedFiles();
            case "blame" -> showBlame(subArgs);
            case "" -> formatError("No command specified. Use: @git help");
            default -> formatError("Unknown command: " + command + ". Use: @git help");
        };
    }

    /**
     * Shows git status with formatted output
     */
    private String showStatus() {
        try {
            GitStatus status = getGitStatus();

            StringBuilder output = new StringBuilder();
            output.append(BoxDrawer.drawSeparator("GIT STATUS", 70, AnsiColors.CYAN));
            output.append("\n\n");

            // Branch info
            String branch = getCurrentBranch();
            output.append("Branch: ").append(AnsiColors.colorize(branch, AnsiColors.CYAN)).append("\n");

            // Ahead/behind info
            String tracking = getTrackingInfo();
            if (!tracking.isEmpty()) {
                output.append(tracking).append("\n");
            }
            output.append("\n");

            // Changes summary
            if (status.hasChanges()) {
                List<String[]> summary = new ArrayList<>();
                if (!status.staged.isEmpty()) {
                    summary.add(new String[]{"Staged", AnsiColors.colorize(String.valueOf(status.staged.size()), AnsiColors.GREEN)});
                }
                if (!status.modified.isEmpty()) {
                    summary.add(new String[]{"Modified", AnsiColors.colorize(String.valueOf(status.modified.size()), AnsiColors.YELLOW)});
                }
                if (!status.untracked.isEmpty()) {
                    summary.add(new String[]{"Untracked", AnsiColors.colorize(String.valueOf(status.untracked.size()), AnsiColors.RED)});
                }
                if (!status.deleted.isEmpty()) {
                    summary.add(new String[]{"Deleted", AnsiColors.colorize(String.valueOf(status.deleted.size()), AnsiColors.RED)});
                }

                output.append(BoxDrawer.drawInfoPanel("CHANGES SUMMARY", summary, 66));
                output.append("\n\n");

                // Staged files
                if (!status.staged.isEmpty()) {
                    output.append(AnsiColors.colorize("Staged changes:", AnsiColors.BOLD_GREEN)).append("\n");
                    for (String file : status.staged) {
                        output.append("  ").append(AnsiColors.colorize("✓ ", AnsiColors.GREEN)).append(file).append("\n");
                    }
                    output.append("\n");
                }

                // Modified files
                if (!status.modified.isEmpty()) {
                    output.append(AnsiColors.colorize("Modified (not staged):", AnsiColors.BOLD_YELLOW)).append("\n");
                    for (String file : status.modified) {
                        output.append("  ").append(AnsiColors.colorize("M ", AnsiColors.YELLOW)).append(file).append("\n");
                    }
                    output.append("\n");
                }

                // Untracked files
                if (!status.untracked.isEmpty()) {
                    output.append(AnsiColors.colorize("Untracked files:", AnsiColors.BOLD_RED)).append("\n");
                    for (String file : status.untracked) {
                        output.append("  ").append(AnsiColors.colorize("? ", AnsiColors.RED)).append(file).append("\n");
                    }
                    output.append("\n");
                }

                // Deleted files
                if (!status.deleted.isEmpty()) {
                    output.append(AnsiColors.colorize("Deleted:", AnsiColors.BOLD_RED)).append("\n");
                    for (String file : status.deleted) {
                        output.append("  ").append(AnsiColors.colorize("D ", AnsiColors.RED)).append(file).append("\n");
                    }
                    output.append("\n");
                }
            } else {
                output.append(AnsiColors.success("✓ Working tree clean")).append("\n\n");
            }

            return output.toString();

        } catch (Exception e) {
            return formatError("Failed to get git status: " + e.getMessage());
        }
    }

    /**
     * Shows git diff
     */
    private String showDiff(String file) {
        try {
            String diff = file.isEmpty() ?
                executeGitCommand("diff") :
                executeGitCommand("diff", file);

            if (diff.trim().isEmpty()) {
                return formatSuccess("No changes to show");
            }

            StringBuilder output = new StringBuilder();
            output.append(BoxDrawer.drawSeparator("GIT DIFF" + (file.isEmpty() ? "" : ": " + file), 70, AnsiColors.CYAN));
            output.append("\n\n");
            output.append(formatDiff(diff));

            return output.toString();

        } catch (Exception e) {
            return formatError("Failed to show diff: " + e.getMessage());
        }
    }

    /**
     * Shows commit log
     */
    private String showLog(String args) {
        try {
            int limit = 10;
            if (!args.isEmpty()) {
                try {
                    limit = Integer.parseInt(args);
                } catch (NumberFormatException e) {
                    return formatError("Invalid number: " + args);
                }
            }

            String log = executeGitCommand("log", "--oneline", "--decorate", "--graph", "-n", String.valueOf(limit));

            StringBuilder output = new StringBuilder();
            output.append(BoxDrawer.drawSeparator("COMMIT HISTORY (last " + limit + ")", 70, AnsiColors.CYAN));
            output.append("\n\n");

            String[] lines = log.split("\n");
            for (String line : lines) {
                output.append(formatLogLine(line)).append("\n");
            }

            return output.toString();

        } catch (Exception e) {
            return formatError("Failed to show log: " + e.getMessage());
        }
    }

    /**
     * Shows branches
     */
    private String showBranches() {
        try {
            String branches = executeGitCommand("branch", "-a");

            StringBuilder output = new StringBuilder();
            output.append(BoxDrawer.drawSeparator("BRANCHES", 70, AnsiColors.CYAN));
            output.append("\n\n");

            String[] lines = branches.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith("*")) {
                    output.append(AnsiColors.colorize(line, AnsiColors.BOLD_GREEN)).append("\n");
                } else if (line.contains("remotes/")) {
                    output.append(AnsiColors.colorize(line, AnsiColors.CYAN)).append("\n");
                } else {
                    output.append(line).append("\n");
                }
            }

            return output.toString();

        } catch (Exception e) {
            return formatError("Failed to show branches: " + e.getMessage());
        }
    }

    /**
     * Shows staged changes
     */
    private String showStagedChanges() {
        try {
            String diff = executeGitCommand("diff", "--cached");

            if (diff.trim().isEmpty()) {
                return formatSuccess("No staged changes");
            }

            StringBuilder output = new StringBuilder();
            output.append(BoxDrawer.drawSeparator("STAGED CHANGES", 70, AnsiColors.GREEN));
            output.append("\n\n");
            output.append(formatDiff(diff));

            return output.toString();

        } catch (Exception e) {
            return formatError("Failed to show staged changes: " + e.getMessage());
        }
    }

    /**
     * Shows unstaged changes
     */
    private String showUnstagedChanges() {
        try {
            String diff = executeGitCommand("diff");

            if (diff.trim().isEmpty()) {
                return formatSuccess("No unstaged changes");
            }

            StringBuilder output = new StringBuilder();
            output.append(BoxDrawer.drawSeparator("UNSTAGED CHANGES", 70, AnsiColors.YELLOW));
            output.append("\n\n");
            output.append(formatDiff(diff));

            return output.toString();

        } catch (Exception e) {
            return formatError("Failed to show unstaged changes: " + e.getMessage());
        }
    }

    /**
     * Shows tracked files
     */
    private String showTrackedFiles() {
        try {
            String files = executeGitCommand("ls-files");
            String[] fileList = files.split("\n");

            StringBuilder output = new StringBuilder();
            output.append(BoxDrawer.drawSeparator("TRACKED FILES (" + fileList.length + ")", 70, AnsiColors.CYAN));
            output.append("\n\n");

            for (String file : fileList) {
                output.append("  ").append(file).append("\n");
            }

            return output.toString();

        } catch (Exception e) {
            return formatError("Failed to list tracked files: " + e.getMessage());
        }
    }

    /**
     * Shows git blame for a file
     */
    private String showBlame(String file) {
        if (file.isEmpty()) {
            return formatError("File path required. Usage: @git blame <file>");
        }

        try {
            Path filePath = Paths.get(file);
            if (!Files.exists(filePath)) {
                return formatError("File not found: " + file);
            }

            String blame = executeGitCommand("blame", file);

            StringBuilder output = new StringBuilder();
            output.append(BoxDrawer.drawSeparator("GIT BLAME: " + file, 70, AnsiColors.CYAN));
            output.append("\n\n");
            output.append(blame);

            return output.toString();

        } catch (Exception e) {
            return formatError("Failed to show blame: " + e.getMessage());
        }
    }

    // === Helper Methods ===

    private boolean isGitRepository() {
        return Files.exists(Paths.get(".git"));
    }

    private String getCurrentBranch() {
        try {
            return executeGitCommand("rev-parse", "--abbrev-ref", "HEAD").trim();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getTrackingInfo() {
        try {
            String upstream = executeGitCommand("rev-parse", "--abbrev-ref", "@{upstream}").trim();
            String ahead = executeGitCommand("rev-list", "--count", "@{upstream}..HEAD").trim();
            String behind = executeGitCommand("rev-list", "--count", "HEAD..@{upstream}").trim();

            List<String> info = new ArrayList<>();
            if (!ahead.equals("0")) {
                info.add(AnsiColors.colorize(ahead + " ahead", AnsiColors.GREEN));
            }
            if (!behind.equals("0")) {
                info.add(AnsiColors.colorize(behind + " behind", AnsiColors.RED));
            }

            if (info.isEmpty()) {
                return "Up to date with " + AnsiColors.colorize(upstream, AnsiColors.CYAN);
            } else {
                return String.join(", ", info) + " of " + AnsiColors.colorize(upstream, AnsiColors.CYAN);
            }
        } catch (Exception e) {
            return "";
        }
    }

    private GitStatus getGitStatus() throws Exception {
        String status = executeGitCommand("status", "--porcelain");
        GitStatus result = new GitStatus();

        for (String line : status.split("\n")) {
            if (line.length() < 3) continue;

            String statusCode = line.substring(0, 2);
            String file = line.substring(3);

            if (statusCode.charAt(0) == 'M' || statusCode.charAt(0) == 'A') {
                result.staged.add(file);
            } else if (statusCode.charAt(1) == 'M') {
                result.modified.add(file);
            } else if (statusCode.equals("??")) {
                result.untracked.add(file);
            } else if (statusCode.charAt(0) == 'D' || statusCode.charAt(1) == 'D') {
                result.deleted.add(file);
            }
        }

        return result;
    }

    private String formatDiff(String diff) {
        String[] lines = diff.split("\n");
        StringBuilder formatted = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("+") && !line.startsWith("+++")) {
                formatted.append(AnsiColors.colorize(line, AnsiColors.GREEN));
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                formatted.append(AnsiColors.colorize(line, AnsiColors.RED));
            } else if (line.startsWith("@@")) {
                formatted.append(AnsiColors.colorize(line, AnsiColors.CYAN));
            } else if (line.startsWith("diff") || line.startsWith("index")) {
                formatted.append(AnsiColors.colorize(line, AnsiColors.BOLD_WHITE));
            } else {
                formatted.append(line);
            }
            formatted.append("\n");
        }

        return formatted.toString();
    }

    private String formatLogLine(String line) {
        // Colorize commit hashes
        if (line.matches(".*[a-f0-9]{7}.*")) {
            line = line.replaceAll("([a-f0-9]{7})", AnsiColors.colorize("$1", AnsiColors.YELLOW));
        }
        // Colorize branch names
        if (line.contains("HEAD")) {
            line = line.replace("HEAD", AnsiColors.colorize("HEAD", AnsiColors.CYAN));
        }
        return line;
    }

    /**
     * Executes a git command safely using parameterized arguments
     * to prevent command injection vulnerabilities.
     *
     * @param args Git command arguments (e.g., "status", "diff", "file.txt")
     * @return Command output
     * @throws Exception if command fails
     */
    private String executeGitCommand(String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        for (String arg : args) {
            if (arg != null && !arg.isEmpty()) {
                command.add(arg);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String result = reader.lines().collect(Collectors.joining("\n"));

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Git command failed with exit code " + exitCode);
        }

        return result;
    }

    private static class GitStatus {
        List<String> staged = new ArrayList<>();
        List<String> modified = new ArrayList<>();
        List<String> untracked = new ArrayList<>();
        List<String> deleted = new ArrayList<>();

        boolean hasChanges() {
            return !staged.isEmpty() || !modified.isEmpty() ||
                   !untracked.isEmpty() || !deleted.isEmpty();
        }
    }
}
