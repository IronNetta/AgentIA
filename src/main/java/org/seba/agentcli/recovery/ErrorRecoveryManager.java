package org.seba.agentcli.recovery;

import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Manages error recovery with automatic suggestions and retry logic
 * Learns from errors to provide better solutions over time
 */
@Component
public class ErrorRecoveryManager {

    private static final int MAX_ERROR_HISTORY = 100;
    private final LinkedList<ErrorRecord> errorHistory = new LinkedList<>();
    private final Map<String, RecoverySuggestion> knownErrors = new HashMap<>();
    private final ErrorLearningSystem learningSystem;

    public ErrorRecoveryManager(ErrorLearningSystem learningSystem) {
        this.learningSystem = learningSystem;
        initializeKnownErrors();
    }

    /**
     * Records an error and provides recovery suggestions
     */
    public RecoveryContext recordError(String operation, Exception error, Map<String, Object> context) {
        ErrorRecord record = new ErrorRecord(
            operation,
            error.getClass().getSimpleName(),
            error.getMessage(),
            LocalDateTime.now(),
            context
        );

        errorHistory.addFirst(record);
        if (errorHistory.size() > MAX_ERROR_HISTORY) {
            errorHistory.removeLast();
        }

        // Find matching recovery suggestions
        List<RecoverySuggestion> suggestions = findRecoverySuggestions(record);

        return new RecoveryContext(record, suggestions);
    }

    /**
     * Finds recovery suggestions for an error
     */
    private List<RecoverySuggestion> findRecoverySuggestions(ErrorRecord error) {
        List<RecoverySuggestion> suggestions = new ArrayList<>();

        // Check learned solutions FIRST (highest priority)
        List<ErrorLearningSystem.LearnedSolution> learnedSolutions = learningSystem.getLearnedSolutions(error);
        if (!learnedSolutions.isEmpty()) {
            List<String> actions = new ArrayList<>();
            for (ErrorLearningSystem.LearnedSolution learned : learnedSolutions) {
                actions.add(String.format("%s (confidence: %.0f%%, used %d times)",
                    learned.solution, learned.confidence * 100, learned.usageCount));
            }
            suggestions.add(new RecoverySuggestion(
                "Learned Solutions",
                "Based on previous successful resolutions:",
                actions,
                RecoverySuggestion.RecoveryAction.RETRY
            ));
        }

        // Check known error patterns
        for (Map.Entry<String, RecoverySuggestion> entry : knownErrors.entrySet()) {
            if (error.message != null &&
                Pattern.compile(entry.getKey(), Pattern.CASE_INSENSITIVE).matcher(error.message).find()) {
                suggestions.add(entry.getValue());
            }
        }

        // Check error history for similar errors
        RecoverySuggestion historicalSuggestion = findHistoricalSuggestion(error);
        if (historicalSuggestion != null) {
            suggestions.add(historicalSuggestion);
        }

        // If no specific suggestions, provide generic ones
        if (suggestions.isEmpty()) {
            suggestions.add(getGenericSuggestion(error));
        }

        return suggestions;
    }

    /**
     * Finds suggestions from error history
     */
    private RecoverySuggestion findHistoricalSuggestion(ErrorRecord currentError) {
        // Look for similar errors in history
        int similarCount = 0;
        ErrorRecord lastSimilar = null;

        for (ErrorRecord historical : errorHistory) {
            if (historical == currentError) continue;

            if (isSimilarError(currentError, historical)) {
                similarCount++;
                if (lastSimilar == null) {
                    lastSimilar = historical;
                }
            }
        }

        if (similarCount >= 2) {
            return new RecoverySuggestion(
                "Recurring Issue Detected",
                String.format("This error has occurred %d times. Consider addressing the root cause.", similarCount),
                List.of(
                    "Review the operation logs for patterns",
                    "Check if the issue is configuration-related",
                    "Consider refactoring the problematic code"
                ),
                RecoverySuggestion.RecoveryAction.MANUAL_INTERVENTION
            );
        }

        return null;
    }

    /**
     * Checks if two errors are similar
     */
    private boolean isSimilarError(ErrorRecord e1, ErrorRecord e2) {
        if (!e1.type.equals(e2.type)) return false;
        if (e1.operation.equals(e2.operation)) return true;

        // Check message similarity (simple keyword matching)
        if (e1.message != null && e2.message != null) {
            String[] words1 = e1.message.toLowerCase().split("\\s+");
            String[] words2 = e2.message.toLowerCase().split("\\s+");

            Set<String> set1 = new HashSet<>(Arrays.asList(words1));
            Set<String> set2 = new HashSet<>(Arrays.asList(words2));

            set1.retainAll(set2); // Intersection
            return set1.size() >= 3; // At least 3 common words
        }

        return false;
    }

    /**
     * Provides a generic suggestion based on error type
     */
    private RecoverySuggestion getGenericSuggestion(ErrorRecord error) {
        return switch (error.type) {
            case "IOException" -> new RecoverySuggestion(
                "File I/O Error",
                "An error occurred while reading or writing a file.",
                List.of(
                    "Check if the file path is correct",
                    "Verify file permissions",
                    "Ensure the file is not locked by another process"
                ),
                RecoverySuggestion.RecoveryAction.RETRY
            );

            case "NullPointerException" -> new RecoverySuggestion(
                "Null Reference Error",
                "Attempted to access a null object.",
                List.of(
                    "Check if required parameters were provided",
                    "Verify initialization order",
                    "Add null checks before accessing objects"
                ),
                RecoverySuggestion.RecoveryAction.MANUAL_INTERVENTION
            );

            case "IllegalArgumentException" -> new RecoverySuggestion(
                "Invalid Argument",
                "An invalid argument was provided.",
                List.of(
                    "Check argument format and type",
                    "Verify argument constraints",
                    "Review the usage documentation"
                ),
                RecoverySuggestion.RecoveryAction.PROVIDE_DIFFERENT_INPUT
            );

            default -> new RecoverySuggestion(
                "Unexpected Error",
                "An unexpected error occurred: " + error.type,
                List.of(
                    "Review error logs for details",
                    "Try the operation again",
                    "Report if the issue persists"
                ),
                RecoverySuggestion.RecoveryAction.RETRY
            );
        };
    }

    /**
     * Initializes known error patterns and their recovery suggestions
     */
    private void initializeKnownErrors() {
        // File not found errors
        knownErrors.put("(no such file|file not found|cannot find)",
            new RecoverySuggestion(
                "File Not Found",
                "The specified file does not exist.",
                List.of(
                    "Verify the file path is correct",
                    "Check if the file was moved or deleted",
                    "Use @search to find the file",
                    "Create the file if it should exist"
                ),
                RecoverySuggestion.RecoveryAction.PROVIDE_DIFFERENT_INPUT
            )
        );

        // Permission denied errors
        knownErrors.put("(permission denied|access denied)",
            new RecoverySuggestion(
                "Permission Denied",
                "Insufficient permissions to perform the operation.",
                List.of(
                    "Check file/directory permissions",
                    "Run with appropriate user privileges",
                    "Verify file is not read-only"
                ),
                RecoverySuggestion.RecoveryAction.MANUAL_INTERVENTION
            )
        );

        // Compilation errors
        knownErrors.put("(compilation error|cannot compile|syntax error)",
            new RecoverySuggestion(
                "Compilation Error",
                "Code failed to compile.",
                List.of(
                    "Review syntax errors in the code",
                    "Check for missing imports or dependencies",
                    "Verify variable and method names",
                    "Use @validate to check code before writing"
                ),
                RecoverySuggestion.RecoveryAction.FIX_CODE
            )
        );

        // Connection errors
        knownErrors.put("(connection refused|timeout|unable to connect)",
            new RecoverySuggestion(
                "Connection Error",
                "Failed to establish connection.",
                List.of(
                    "Check if the service is running",
                    "Verify network connectivity",
                    "Check firewall settings",
                    "Verify the endpoint URL is correct"
                ),
                RecoverySuggestion.RecoveryAction.RETRY
            )
        );

        // Git errors
        knownErrors.put("(git|merge conflict|not a git repository)",
            new RecoverySuggestion(
                "Git Operation Error",
                "Git operation failed.",
                List.of(
                    "Check if you're in a git repository",
                    "Verify repository state with @git status",
                    "Resolve any merge conflicts",
                    "Check branch state"
                ),
                RecoverySuggestion.RecoveryAction.MANUAL_INTERVENTION
            )
        );

        // Out of memory errors
        knownErrors.put("(out of memory|heap space)",
            new RecoverySuggestion(
                "Memory Error",
                "Operation exceeded available memory.",
                List.of(
                    "Process smaller batches of data",
                    "Increase JVM heap size",
                    "Check for memory leaks",
                    "Optimize data structures"
                ),
                RecoverySuggestion.RecoveryAction.MANUAL_INTERVENTION
            )
        );
    }

    /**
     * Gets error statistics
     */
    public ErrorStatistics getStatistics() {
        Map<String, Integer> errorsByType = new HashMap<>();
        Map<String, Integer> errorsByOperation = new HashMap<>();

        for (ErrorRecord error : errorHistory) {
            errorsByType.merge(error.type, 1, Integer::sum);
            errorsByOperation.merge(error.operation, 1, Integer::sum);
        }

        return new ErrorStatistics(
            errorHistory.size(),
            errorsByType,
            errorsByOperation
        );
    }

    /**
     * Clears error history
     */
    public void clearHistory() {
        errorHistory.clear();
    }

    /**
     * Gets recent errors
     */
    public List<ErrorRecord> getRecentErrors(int limit) {
        return errorHistory.stream()
            .limit(limit)
            .toList();
    }

    /**
     * Records a successful resolution for learning
     */
    public void recordSuccessfulResolution(ErrorRecord error, String solution, String outcome) {
        learningSystem.recordSuccessfulResolution(error, solution, outcome);
    }

    /**
     * Records a failed resolution attempt for learning
     */
    public void recordFailedResolution(ErrorRecord error, String attemptedSolution, String reason) {
        learningSystem.recordFailedResolution(error, attemptedSolution, reason);
    }

    /**
     * Gets learning insights
     */
    public ErrorLearningSystem.LearningInsights getLearningInsights() {
        return learningSystem.getInsights();
    }

    /**
     * Clears learned knowledge
     */
    public void clearLearning() {
        learningSystem.clearKnowledge();
    }

    // === Data Classes ===

    /**
     * Record of an error occurrence
     */
    public static class ErrorRecord {
        public final String operation;
        public final String type;
        public final String message;
        public final LocalDateTime timestamp;
        public final Map<String, Object> context;

        public ErrorRecord(String operation, String type, String message,
                          LocalDateTime timestamp, Map<String, Object> context) {
            this.operation = operation;
            this.type = type;
            this.message = message;
            this.timestamp = timestamp;
            this.context = context != null ? context : Map.of();
        }
    }

    /**
     * Recovery context with error and suggestions
     */
    public static class RecoveryContext {
        public final ErrorRecord error;
        public final List<RecoverySuggestion> suggestions;

        public RecoveryContext(ErrorRecord error, List<RecoverySuggestion> suggestions) {
            this.error = error;
            this.suggestions = suggestions;
        }

        /**
         * Formats the recovery context for display
         */
        public String format() {
            StringBuilder output = new StringBuilder();

            output.append(BoxDrawer.drawSeparator("ERROR RECOVERY", 70, AnsiColors.RED));
            output.append("\n\n");

            // Error details
            output.append(AnsiColors.colorize("Operation: ", AnsiColors.BOLD_WHITE))
                  .append(error.operation).append("\n");
            output.append(AnsiColors.colorize("Error Type: ", AnsiColors.BOLD_WHITE))
                  .append(error.type).append("\n");
            output.append(AnsiColors.colorize("Message: ", AnsiColors.BOLD_WHITE))
                  .append(error.message).append("\n\n");

            // Recovery suggestions
            if (!suggestions.isEmpty()) {
                output.append(AnsiColors.colorize("RECOVERY SUGGESTIONS:", AnsiColors.BOLD_CYAN))
                      .append("\n\n");

                for (int i = 0; i < suggestions.size(); i++) {
                    RecoverySuggestion suggestion = suggestions.get(i);
                    output.append(String.format("%d. ", i + 1))
                          .append(AnsiColors.colorize(suggestion.title, AnsiColors.BOLD_WHITE))
                          .append("\n");
                    output.append("   ").append(suggestion.description).append("\n\n");

                    output.append("   Suggested actions:\n");
                    for (String action : suggestion.actions) {
                        output.append("   • ").append(action).append("\n");
                    }

                    output.append("\n   Recommended: ")
                          .append(AnsiColors.colorize(
                              suggestion.recommendedAction.getDisplayName(),
                              AnsiColors.YELLOW))
                          .append("\n\n");
                }
            }

            return output.toString();
        }
    }

    /**
     * Recovery suggestion
     */
    public static class RecoverySuggestion {
        public final String title;
        public final String description;
        public final List<String> actions;
        public final RecoveryAction recommendedAction;

        public RecoverySuggestion(String title, String description,
                                 List<String> actions, RecoveryAction recommendedAction) {
            this.title = title;
            this.description = description;
            this.actions = actions;
            this.recommendedAction = recommendedAction;
        }

        public enum RecoveryAction {
            RETRY("Retry the operation"),
            PROVIDE_DIFFERENT_INPUT("Provide different input"),
            FIX_CODE("Fix the code"),
            MANUAL_INTERVENTION("Manual intervention required"),
            SKIP("Skip this step");

            private final String displayName;

            RecoveryAction(String displayName) {
                this.displayName = displayName;
            }

            public String getDisplayName() {
                return displayName;
            }
        }
    }

    /**
     * Error statistics
     */
    public static class ErrorStatistics {
        public final int totalErrors;
        public final Map<String, Integer> errorsByType;
        public final Map<String, Integer> errorsByOperation;

        public ErrorStatistics(int totalErrors, Map<String, Integer> errorsByType,
                              Map<String, Integer> errorsByOperation) {
            this.totalErrors = totalErrors;
            this.errorsByType = errorsByType;
            this.errorsByOperation = errorsByOperation;
        }

        public String format() {
            StringBuilder output = new StringBuilder();
            output.append(BoxDrawer.drawSeparator("ERROR STATISTICS", 70, AnsiColors.CYAN));
            output.append("\n\n");

            output.append("Total Errors: ").append(totalErrors).append("\n\n");

            if (!errorsByType.isEmpty()) {
                output.append(AnsiColors.colorize("By Type:", AnsiColors.BOLD_WHITE)).append("\n");
                errorsByType.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> output.append(String.format("  %-30s: %d\n", e.getKey(), e.getValue())));
                output.append("\n");
            }

            if (!errorsByOperation.isEmpty()) {
                output.append(AnsiColors.colorize("By Operation:", AnsiColors.BOLD_WHITE)).append("\n");
                errorsByOperation.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .forEach(e -> output.append(String.format("  %-30s: %d\n", e.getKey(), e.getValue())));
            }

            return output.toString();
        }
    }
}
