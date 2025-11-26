package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.recovery.ErrorRecoveryManager;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tool to view error history and statistics
 */
@Component
public class ErrorsTool extends AbstractTool {

    private final ErrorRecoveryManager errorRecoveryManager;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ErrorsTool(CliService cliService, ErrorRecoveryManager errorRecoveryManager) {
        super(cliService);
        this.errorRecoveryManager = errorRecoveryManager;
    }

    @Override
    public String getName() {
        return "@errors";
    }

    @Override
    public String getDescription() {
        return "View error history and statistics";
    }

    @Override
    public String getUsage() {
        return """
                ERROR HISTORY AND STATISTICS

                Usage: @errors [command]

                Commands:
                  list [n]     Show last n errors (default: 10)
                  stats        Show error statistics
                  insights     Show learning insights from past errors
                  clear        Clear error history
                  clearlearn   Clear learned patterns

                Examples:
                  @errors              Show recent errors
                  @errors list 20      Show last 20 errors
                  @errors stats        Show error statistics
                  @errors insights     Show what the system has learned
                """;
    }

    @Override
    public String execute(String args, ProjectContext context) {
        String[] parts = args.trim().split("\\s+");
        String command = parts.length > 0 && !parts[0].isEmpty() ? parts[0].toLowerCase() : "list";

        return switch (command) {
            case "list" -> {
                int limit = 10;
                if (parts.length > 1) {
                    try {
                        limit = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        // Use default
                    }
                }
                yield showErrorList(limit);
            }
            case "stats" -> showStatistics();
            case "insights" -> showLearningInsights();
            case "clear" -> clearErrors();
            case "clearlearn" -> clearLearning();
            default -> formatError("Unknown command: " + command + "\n\n" + getUsage());
        };
    }

    /**
     * Shows list of recent errors
     */
    private String showErrorList(int limit) {
        List<ErrorRecoveryManager.ErrorRecord> errors = errorRecoveryManager.getRecentErrors(limit);

        if (errors.isEmpty()) {
            return formatSuccess("No errors recorded");
        }

        StringBuilder output = new StringBuilder();
        output.append(BoxDrawer.drawSeparator("ERROR HISTORY (last " + errors.size() + ")", 70, AnsiColors.CYAN));
        output.append("\n\n");

        for (int i = 0; i < errors.size(); i++) {
            ErrorRecoveryManager.ErrorRecord error = errors.get(i);

            output.append(AnsiColors.colorize(String.format("#%d", i + 1), AnsiColors.BOLD_WHITE))
                  .append(" - ")
                  .append(AnsiColors.colorize(error.timestamp.format(FORMATTER), AnsiColors.BRIGHT_BLACK))
                  .append("\n");

            output.append("  Operation: ")
                  .append(AnsiColors.colorize(error.operation, AnsiColors.CYAN))
                  .append("\n");

            output.append("  Type: ")
                  .append(AnsiColors.colorize(error.type, AnsiColors.RED))
                  .append("\n");

            if (error.message != null && !error.message.isEmpty()) {
                String shortMessage = error.message.length() > 100
                    ? error.message.substring(0, 100) + "..."
                    : error.message;
                output.append("  Message: ").append(shortMessage).append("\n");
            }

            if (!error.context.isEmpty()) {
                output.append("  Context: ").append(error.context.size()).append(" item(s)\n");
            }

            output.append("\n");
        }

        return output.toString();
    }

    /**
     * Shows error statistics
     */
    private String showStatistics() {
        ErrorRecoveryManager.ErrorStatistics stats = errorRecoveryManager.getStatistics();

        if (stats.totalErrors == 0) {
            return formatSuccess("No errors recorded");
        }

        return stats.format();
    }

    /**
     * Clears error history
     */
    private String clearErrors() {
        errorRecoveryManager.clearHistory();
        return formatSuccess("Error history cleared");
    }

    /**
     * Shows learning insights
     */
    private String showLearningInsights() {
        org.seba.agentcli.recovery.ErrorLearningSystem.LearningInsights insights =
            errorRecoveryManager.getLearningInsights();

        if (insights.totalPatterns == 0) {
            return formatSuccess("No learned patterns yet. The system will learn from errors as they occur.");
        }

        return insights.format();
    }

    /**
     * Clears learned patterns
     */
    private String clearLearning() {
        errorRecoveryManager.clearLearning();
        return formatSuccess("Learned patterns cleared");
    }
}
