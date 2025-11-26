package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.review.CodeReviewService;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Code review tool for automated code quality analysis
 */
@Component
public class ReviewTool extends AbstractTool {

    private final CodeReviewService reviewService;

    public ReviewTool(CliService cliService, CodeReviewService reviewService) {
        super(cliService);
        this.reviewService = reviewService;
    }

    @Override
    public String getName() {
        return "@review";
    }

    @Override
    public String getDescription() {
        return "Automated code review for quality and best practices";
    }

    @Override
    public String getUsage() {
        return """
                CODE REVIEW ASSISTANT

                Usage: @review <file_or_pattern>

                Examples:
                  @review src/Main.java              Review a single file
                  @review src/**/*.java              Review all Java files
                  @review .                          Review all source files in project

                The review checks for:
                  • Code smells and anti-patterns
                  • Best practice violations
                  • Potential bugs and issues
                  • Code complexity
                  • Security concerns
                  • Style and formatting

                Each file receives a quality score (0-100).
                """;
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args.isEmpty()) {
            return formatError("File or pattern required. Usage: @review <file_or_pattern>");
        }

        try {
            List<Path> filesToReview = resolveFiles(args, context);

            if (filesToReview.isEmpty()) {
                return formatError("No files found matching: " + args);
            }

            // Review files
            CodeReviewService.MultiFileReviewResult result = reviewService.reviewFiles(filesToReview);

            return formatReviewResult(result, context);

        } catch (Exception e) {
            return formatError("Review failed: " + e.getMessage());
        }
    }

    /**
     * Resolves files from pattern
     */
    private List<Path> resolveFiles(String pattern, ProjectContext context) {
        List<Path> files = new ArrayList<>();

        // Single file
        Path singleFile = Paths.get(pattern);
        if (Files.isRegularFile(singleFile)) {
            return List.of(singleFile);
        }

        // Pattern or directory
        if (context != null) {
            String lowerPattern = pattern.toLowerCase();

            for (Path sourceFile : context.getSourceFiles()) {
                String relativePath = context.getRootPath().relativize(sourceFile).toString().toLowerCase();

                if (pattern.equals(".") || relativePath.contains(lowerPattern)) {
                    files.add(sourceFile);
                }
            }
        }

        return files;
    }

    /**
     * Formats review results
     */
    private String formatReviewResult(CodeReviewService.MultiFileReviewResult result, ProjectContext context) {
        StringBuilder output = new StringBuilder();

        output.append(BoxDrawer.drawSeparator("CODE REVIEW REPORT", 70, AnsiColors.CYAN));
        output.append("\n\n");

        // Summary
        int totalFiles = result.results.size();
        int totalFindings = result.getTotalFindings();
        double avgScore = result.getAverageScore();

        String scoreColor = avgScore >= 80 ? AnsiColors.GREEN :
                           avgScore >= 60 ? AnsiColors.YELLOW :
                           AnsiColors.RED;

        List<String[]> summary = List.of(
            new String[]{"Files Reviewed", String.valueOf(totalFiles)},
            new String[]{"Total Findings", String.valueOf(totalFindings)},
            new String[]{"Average Score", AnsiColors.colorize(String.format("%.1f/100", avgScore), scoreColor)}
        );

        output.append(BoxDrawer.drawInfoPanel("SUMMARY", summary, 66));
        output.append("\n\n");

        // File results
        output.append(AnsiColors.colorize("FILE RESULTS:", AnsiColors.BOLD_WHITE));
        output.append("\n\n");

        for (CodeReviewService.ReviewResult fileResult : result.results) {
            String relativePath = context != null ?
                context.getRootPath().relativize(fileResult.file).toString() :
                fileResult.file.toString();

            output.append("📄 ").append(AnsiColors.colorize(relativePath, AnsiColors.CYAN));
            output.append("\n");

            // Score
            String fileScoreColor = fileResult.qualityScore >= 80 ? AnsiColors.GREEN :
                                   fileResult.qualityScore >= 60 ? AnsiColors.YELLOW :
                                   AnsiColors.RED;

            output.append("   Score: ")
                  .append(AnsiColors.colorize(String.format("%.1f/100", fileResult.qualityScore), fileScoreColor))
                  .append("\n");

            // Issue counts
            int errors = fileResult.getErrorCount();
            int warnings = fileResult.getWarningCount();
            int infos = fileResult.getInfoCount();

            output.append("   Issues: ");

            if (errors > 0) {
                output.append(AnsiColors.colorize(errors + " error(s)", AnsiColors.RED)).append("  ");
            }
            if (warnings > 0) {
                output.append(AnsiColors.colorize(warnings + " warning(s)", AnsiColors.YELLOW)).append("  ");
            }
            if (infos > 0) {
                output.append(AnsiColors.colorize(infos + " info", AnsiColors.CYAN));
            }
            if (errors == 0 && warnings == 0 && infos == 0) {
                output.append(AnsiColors.colorize("No issues found", AnsiColors.GREEN));
            }

            output.append("\n\n");

            // Show findings (grouped by severity)
            if (!fileResult.findings.isEmpty()) {
                // Errors first
                List<CodeReviewService.ReviewFinding> errors_list = fileResult.findings.stream()
                    .filter(f -> f.severity == CodeReviewService.Severity.ERROR)
                    .collect(Collectors.toList());

                if (!errors_list.isEmpty()) {
                    output.append("   ").append(AnsiColors.colorize("Errors:", AnsiColors.BOLD_RED)).append("\n");
                    for (CodeReviewService.ReviewFinding finding : errors_list) {
                        output.append(formatFinding(finding));
                    }
                }

                // Warnings
                List<CodeReviewService.ReviewFinding> warnings_list = fileResult.findings.stream()
                    .filter(f -> f.severity == CodeReviewService.Severity.WARNING)
                    .limit(5) // Limit warnings shown
                    .collect(Collectors.toList());

                if (!warnings_list.isEmpty()) {
                    output.append("   ").append(AnsiColors.colorize("Warnings:", AnsiColors.BOLD_YELLOW)).append("\n");
                    for (CodeReviewService.ReviewFinding finding : warnings_list) {
                        output.append(formatFinding(finding));
                    }

                    int remainingWarnings = warnings - warnings_list.size();
                    if (remainingWarnings > 0) {
                        output.append("   ").append(AnsiColors.colorize(
                            String.format("... and %d more warning(s)", remainingWarnings),
                            AnsiColors.BRIGHT_BLACK
                        )).append("\n");
                    }
                }

                output.append("\n");
            }
        }

        // Overall recommendation
        output.append(BoxDrawer.drawSeparator("RECOMMENDATION", 70, AnsiColors.CYAN));
        output.append("\n\n");

        if (avgScore >= 80) {
            output.append(AnsiColors.success("✓ Code quality is good. Ready to commit."));
        } else if (avgScore >= 60) {
            output.append(AnsiColors.warning("⚠ Code quality is acceptable but has room for improvement."));
            output.append("\n  Consider addressing warnings before commit.");
        } else {
            output.append(AnsiColors.error("✗ Code quality needs improvement."));
            output.append("\n  Please address errors and critical warnings before commit.");
        }

        output.append("\n");

        return output.toString();
    }

    /**
     * Formats a single finding
     */
    private String formatFinding(CodeReviewService.ReviewFinding finding) {
        String icon = switch (finding.severity) {
            case ERROR -> "✗";
            case WARNING -> "⚠";
            case INFO -> "ℹ";
        };

        String color = switch (finding.severity) {
            case ERROR -> AnsiColors.RED;
            case WARNING -> AnsiColors.YELLOW;
            case INFO -> AnsiColors.CYAN;
        };

        return String.format("     %s Line %d: [%s] %s\n",
            AnsiColors.colorize(icon, color),
            finding.lineNumber,
            finding.category,
            finding.message
        );
    }
}
