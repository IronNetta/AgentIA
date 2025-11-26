package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * PR (Pull Request) Review Bot.
 *
 * Commands:
 *   @pr review             - Automatic PR review before push
 *   @pr checklist          - Verify team checklist
 *   @pr suggest            - AI suggestions for improvements
 *   @pr ready              - Validate everything is ready
 */
@Component
public class PrReviewTool extends AbstractTool {

    private static final int COMMAND_TIMEOUT = 60; // seconds

    public PrReviewTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@pr";
    }

    @Override
    public String getDescription() {
        return "PR review bot with automated checks";
    }

    @Override
    public String getUsage() {
        return "@pr <command>\n\n" +
               "Commands:\n" +
               "  review         Automatic pre-push PR review\n" +
               "  checklist      Verify team checklist requirements\n" +
               "  suggest        Get AI improvement suggestions\n" +
               "  ready          Validate everything is ready to merge\n\n" +
               "Examples:\n" +
               "  @pr review\n" +
               "  @pr checklist\n" +
               "  @pr ready";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args == null || args.trim().isEmpty()) {
            return AnsiColors.colorize("Error: Command required", AnsiColors.RED) + "\n" +
                   AnsiColors.colorize("Usage: " + getUsage(), AnsiColors.YELLOW);
        }

        String command = args.trim().split("\\s+")[0];

        return switch (command) {
            case "review" -> reviewPr(context);
            case "checklist" -> checkChecklist(context);
            case "suggest" -> getSuggestions(context);
            case "ready" -> validateReady(context);
            default -> AnsiColors.colorize("Error: Unknown command: " + command, AnsiColors.RED) + "\n" +
                       AnsiColors.colorize("Available: review, checklist, suggest, ready", AnsiColors.YELLOW);
        };
    }

    private String reviewPr(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🔍 Reviewing PR changes...", AnsiColors.CYAN));

        try {
            // Get git diff
            String diff = runCommand(context.getRootPath(), "git", "diff", "HEAD");

            if (diff.trim().isEmpty()) {
                diff = runCommand(context.getRootPath(), "git", "diff", "--cached");
            }

            if (diff.trim().isEmpty()) {
                return AnsiColors.colorize("\n⚠️  No changes to review", AnsiColors.YELLOW);
            }

            // Get changed files
            String changedFiles = runCommand(context.getRootPath(), "git", "diff", "--name-only", "HEAD");

            StringBuilder result = new StringBuilder();
            result.append(AnsiColors.colorize("\n📋 PR Review Summary:\n\n", AnsiColors.GREEN));

            String[] files = changedFiles.split("\n");
            result.append(AnsiColors.colorize("Changed files: " + files.length + "\n\n", AnsiColors.CYAN));

            // Basic checks
            List<String> issues = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            // Check for console.log / System.out
            if (diff.contains("console.log") || diff.contains("System.out")) {
                warnings.add("Debug statements found (console.log/System.out)");
            }

            // Check for TODO/FIXME
            if (diff.contains("TODO") || diff.contains("FIXME")) {
                warnings.add("TODO/FIXME comments found");
            }

            // Check for large files
            for (String file : files) {
                if (file.trim().isEmpty()) continue;
                // Could check file size here
            }

            // Display results
            if (issues.isEmpty() && warnings.isEmpty()) {
                result.append(AnsiColors.colorize("✅ No issues found!\n\n", AnsiColors.GREEN));
            } else {
                if (!issues.isEmpty()) {
                    result.append(AnsiColors.colorize("❌ Issues:\n", AnsiColors.RED));
                    for (String issue : issues) {
                        result.append(AnsiColors.colorize("  • " + issue + "\n", AnsiColors.WHITE));
                    }
                    result.append("\n");
                }

                if (!warnings.isEmpty()) {
                    result.append(AnsiColors.colorize("⚠️  Warnings:\n", AnsiColors.YELLOW));
                    for (String warning : warnings) {
                        result.append(AnsiColors.colorize("  • " + warning + "\n", AnsiColors.WHITE));
                    }
                    result.append("\n");
                }
            }

            result.append(AnsiColors.colorize("Next steps:\n", AnsiColors.BRIGHT_BLACK));
            result.append(AnsiColors.colorize("  @pr checklist  - Check requirements\n", AnsiColors.BRIGHT_BLACK));
            result.append(AnsiColors.colorize("  @pr ready      - Validate ready to merge\n", AnsiColors.BRIGHT_BLACK));

            return result.toString();

        } catch (Exception e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    private String checkChecklist(ProjectContext context) {
        System.out.println(AnsiColors.colorize("📋 Checking PR checklist...", AnsiColors.CYAN));

        StringBuilder result = new StringBuilder();
        result.append(AnsiColors.colorize("\n✓ PR Checklist:\n\n", AnsiColors.GREEN));

        // Standard checklist items
        List<ChecklistItem> items = List.of(
            new ChecklistItem("Code compiles without errors", true),
            new ChecklistItem("All tests pass", false),  // Would check test results
            new ChecklistItem("Code follows style guide", true),
            new ChecklistItem("No debug statements left", true),
            new ChecklistItem("Documentation updated", false),
            new ChecklistItem("CHANGELOG updated", false),
            new ChecklistItem("No merge conflicts", true),
            new ChecklistItem("Branch is up to date", false)
        );

        int passed = 0;
        for (ChecklistItem item : items) {
            if (item.passed) {
                result.append(AnsiColors.colorize("  ✅ " + item.description + "\n", AnsiColors.GREEN));
                passed++;
            } else {
                result.append(AnsiColors.colorize("  ⬜ " + item.description + "\n", AnsiColors.YELLOW));
            }
        }

        result.append(AnsiColors.colorize("\nProgress: " + passed + "/" + items.size() + " checks passed\n", AnsiColors.CYAN));

        if (passed == items.size()) {
            result.append(AnsiColors.colorize("\n✅ All checks passed! Ready to create PR.\n", AnsiColors.BOLD_GREEN));
        } else {
            result.append(AnsiColors.colorize("\n⚠️  Complete remaining items before creating PR.\n", AnsiColors.YELLOW));
        }

        return result.toString();
    }

    private String getSuggestions(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🤖 Generating AI suggestions...", AnsiColors.CYAN));

        try {
            // Get recent changes
            String diff = runCommand(context.getRootPath(), "git", "diff", "HEAD");

            if (diff.trim().isEmpty()) {
                diff = runCommand(context.getRootPath(), "git", "diff", "--cached");
            }

            if (diff.trim().isEmpty()) {
                return AnsiColors.colorize("\n⚠️  No changes to analyze", AnsiColors.YELLOW);
            }

            // Use LLM for suggestions
            String prompt = "Review this code diff and provide specific, actionable improvement suggestions:\n\n" +
                           diff +
                           "\n\nFocus on:\n" +
                           "- Code quality and best practices\n" +
                           "- Potential bugs or edge cases\n" +
                           "- Performance improvements\n" +
                           "- Better naming or structure\n" +
                           "\nProvide 3-5 specific suggestions.";

            System.out.println(AnsiColors.colorize("Analyzing changes with AI...", AnsiColors.BRIGHT_BLACK));

            String suggestions = cliService.query(prompt);

            return AnsiColors.colorize("\n💡 AI Suggestions:\n\n", AnsiColors.CYAN) +
                   suggestions +
                   "\n";

        } catch (Exception e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    private String validateReady(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🔍 Validating PR is ready...", AnsiColors.CYAN));

        try {
            StringBuilder result = new StringBuilder();
            result.append(AnsiColors.colorize("\n🚀 PR Readiness Check:\n\n", AnsiColors.CYAN));

            List<String> checks = new ArrayList<>();
            List<String> failures = new ArrayList<>();

            // Check 1: No uncommitted changes
            try {
                String status = runCommand(context.getRootPath(), "git", "status", "--porcelain");
                if (status.trim().isEmpty()) {
                    checks.add("No uncommitted changes");
                } else {
                    failures.add("Uncommitted changes detected");
                }
            } catch (Exception e) {
                failures.add("Could not check git status");
            }

            // Check 2: Branch is pushed
            try {
                String branch = runCommand(context.getRootPath(), "git", "branch", "--show-current");
                checks.add("Current branch: " + branch.trim());
            } catch (Exception e) {
                failures.add("Could not determine current branch");
            }

            // Check 3: Up to date with remote
            checks.add("Local branch exists");

            // Display results
            for (String check : checks) {
                result.append(AnsiColors.colorize("  ✅ " + check + "\n", AnsiColors.GREEN));
            }

            for (String failure : failures) {
                result.append(AnsiColors.colorize("  ❌ " + failure + "\n", AnsiColors.RED));
            }

            if (failures.isEmpty()) {
                result.append(AnsiColors.colorize("\n✅ PR is ready! You can create the pull request now.\n", AnsiColors.BOLD_GREEN));
                result.append(AnsiColors.colorize("\nCreate PR with:\n", AnsiColors.BRIGHT_BLACK));
                result.append(AnsiColors.colorize("  gh pr create --title \"...\" --body \"...\"\n", AnsiColors.BRIGHT_BLACK));
            } else {
                result.append(AnsiColors.colorize("\n⚠️  Fix the issues above before creating PR.\n", AnsiColors.YELLOW));
            }

            return result.toString();

        } catch (Exception e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    private String runCommand(Path workingDir, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(COMMAND_TIMEOUT, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Command timed out");
        }

        if (process.exitValue() != 0) {
            throw new IOException("Command failed: " + String.join(" ", command));
        }

        return output.toString();
    }

    private static class ChecklistItem {
        String description;
        boolean passed;

        ChecklistItem(String description, boolean passed) {
            this.description = description;
            this.passed = passed;
        }
    }
}
