package org.seba.agentcli.context;

import org.seba.agentcli.CliService;
import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.seba.agentcli.io.ConsoleReader;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.model.TaskPlan;
import org.seba.agentcli.recovery.ErrorRecoveryManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Executes plan tasks automatically with LLM assistance
 * Handles errors, retries, and progress tracking
 */
@Component
public class PlanExecutor {

    private final CliService cliService;
    private final PlanManager planManager;
    private final ErrorRecoveryManager errorRecoveryManager;
    private final ConsoleReader consoleReader;

    private boolean executionInProgress = false;
    private volatile boolean shouldStop = false;

    public PlanExecutor(CliService cliService,
                       PlanManager planManager,
                       ErrorRecoveryManager errorRecoveryManager,
                       ConsoleReader consoleReader) {
        this.cliService = cliService;
        this.planManager = planManager;
        this.errorRecoveryManager = errorRecoveryManager;
        this.consoleReader = consoleReader;
    }

    /**
     * Executes the current plan automatically
     */
    public ExecutionResult executePlan(ProjectContext projectContext) {
        if (!planManager.hasPlan()) {
            return new ExecutionResult(false, "No active plan to execute");
        }

        if (executionInProgress) {
            return new ExecutionResult(false, "Execution already in progress");
        }

        try {
            executionInProgress = true;
            shouldStop = false;

            return executeAllTasks(projectContext);

        } finally {
            executionInProgress = false;
        }
    }

    /**
     * Executes all pending tasks in the plan
     */
    private ExecutionResult executeAllTasks(ProjectContext projectContext) {
        TaskPlan plan = planManager.getCurrentPlan();
        List<TaskPlan.TaskItem> tasks = plan.getTasks();

        System.out.println();
        System.out.println(BoxDrawer.drawSeparator("AUTOMATIC PLAN EXECUTION", 70, AnsiColors.CYAN));
        System.out.println();
        System.out.println("Starting automatic execution of " + tasks.size() + " task(s)");
        System.out.println(AnsiColors.colorize("Press Ctrl+C to stop", AnsiColors.BRIGHT_BLACK));
        System.out.println();

        int completed = 0;
        int failed = 0;
        int skipped = 0;

        for (TaskPlan.TaskItem task : tasks) {
            if (shouldStop) {
                skipped = (int) tasks.stream()
                    .filter(t -> t.getStatus() == TaskPlan.TaskStatus.PENDING)
                    .count();
                break;
            }

            // Skip already completed or failed tasks
            if (task.getStatus() == TaskPlan.TaskStatus.COMPLETED) {
                completed++;
                continue;
            }
            if (task.getStatus() == TaskPlan.TaskStatus.FAILED) {
                failed++;
                continue;
            }

            // Execute task
            System.out.println(BoxDrawer.drawSeparator(
                "Task #" + task.getNumber() + ": " + task.getDescription(),
                70,
                AnsiColors.YELLOW
            ));
            System.out.println();

            TaskExecutionResult result = executeTask(task, projectContext);

            if (result.success) {
                planManager.completeTask(task.getNumber());
                completed++;
                System.out.println();
                System.out.println(AnsiColors.success("✓ Task completed successfully"));
                System.out.println();
            } else {
                planManager.failTask(task.getNumber(), result.error);
                failed++;
                System.out.println();
                System.out.println(AnsiColors.error("✗ Task failed: " + result.error));
                System.out.println();

                // Ask user what to do
                String choice = askForErrorAction(result);

                if (choice.equals("stop")) {
                    shouldStop = true;
                    skipped = (int) tasks.stream()
                        .filter(t -> t.getStatus() == TaskPlan.TaskStatus.PENDING)
                        .count();
                    break;
                } else if (choice.equals("skip")) {
                    // Continue to next task
                    continue;
                } else if (choice.equals("retry")) {
                    // Retry the task
                    planManager.startTask(task.getNumber());
                    TaskExecutionResult retryResult = executeTask(task, projectContext);

                    if (retryResult.success) {
                        planManager.completeTask(task.getNumber());
                        completed++;
                        failed--; // Cancel the previous failure
                        System.out.println();
                        System.out.println(AnsiColors.success("✓ Task completed on retry"));
                        System.out.println();
                    } else {
                        System.out.println();
                        System.out.println(AnsiColors.error("✗ Task failed again: " + retryResult.error));
                        System.out.println();
                        shouldStop = true;
                        break;
                    }
                }
            }

            // Delay between tasks
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                shouldStop = true;
                break;
            }
        }

        // Summary
        System.out.println();
        System.out.println(BoxDrawer.drawSeparator("EXECUTION SUMMARY", 70, AnsiColors.CYAN));
        System.out.println();

        List<String[]> summary = List.of(
            new String[]{"Total Tasks", String.valueOf(tasks.size())},
            new String[]{"Completed", AnsiColors.colorize(String.valueOf(completed), AnsiColors.GREEN)},
            new String[]{"Failed", failed > 0 ? AnsiColors.colorize(String.valueOf(failed), AnsiColors.RED) : "0"},
            new String[]{"Skipped", skipped > 0 ? AnsiColors.colorize(String.valueOf(skipped), AnsiColors.YELLOW) : "0"}
        );

        System.out.println(BoxDrawer.drawInfoPanel("RESULTS", summary, 66));
        System.out.println();

        boolean allCompleted = completed == tasks.size();
        String message = allCompleted
            ? "All tasks completed successfully!"
            : String.format("Execution incomplete: %d completed, %d failed, %d skipped",
                           completed, failed, skipped);

        return new ExecutionResult(allCompleted, message);
    }

    /**
     * Executes a single task using LLM
     */
    private TaskExecutionResult executeTask(TaskPlan.TaskItem task, ProjectContext projectContext) {
        planManager.startTask(task.getNumber());

        try {
            // Build prompt for LLM
            String prompt = buildTaskPrompt(task, projectContext);

            // Query LLM
            System.out.println(AnsiColors.colorize("Analyzing task...", AnsiColors.CYAN));
            String response = cliService.query(prompt);

            // Check if LLM indicated success or failure
            if (containsErrorIndicators(response)) {
                return new TaskExecutionResult(false, extractErrorMessage(response));
            }

            return new TaskExecutionResult(true, null);

        } catch (Exception e) {
            // Record error for recovery
            ErrorRecoveryManager.RecoveryContext recovery = errorRecoveryManager.recordError(
                "Plan execution - Task #" + task.getNumber(),
                e,
                Map.of("task", task.getDescription())
            );

            return new TaskExecutionResult(false, e.getMessage());
        }
    }

    /**
     * Builds the prompt for a task execution
     */
    private String buildTaskPrompt(TaskPlan.TaskItem task, ProjectContext projectContext) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Execute the following task:\n\n");
        prompt.append("Task: ").append(task.getDescription()).append("\n\n");

        prompt.append("Context:\n");
        prompt.append("- This is task #").append(task.getNumber())
              .append(" in a multi-step plan\n");

        // Add previously completed tasks as context
        TaskPlan plan = planManager.getCurrentPlan();
        List<TaskPlan.TaskItem> completed = plan.getTasks().stream()
            .filter(t -> t.getStatus() == TaskPlan.TaskStatus.COMPLETED)
            .filter(t -> t.getNumber() < task.getNumber())
            .toList();

        if (!completed.isEmpty()) {
            prompt.append("- Previously completed tasks:\n");
            for (TaskPlan.TaskItem completedTask : completed) {
                prompt.append("  ✓ Task #").append(completedTask.getNumber())
                      .append(": ").append(completedTask.getDescription()).append("\n");
            }
        }

        prompt.append("\n");
        prompt.append("Instructions:\n");
        prompt.append("- Perform the task as described\n");
        prompt.append("- Use appropriate tools (@read, @write, @edit, etc.)\n");
        prompt.append("- If you encounter errors, report them clearly\n");
        prompt.append("- Confirm completion when done\n");

        return prompt.toString();
    }

    /**
     * Checks if response contains error indicators
     */
    private boolean containsErrorIndicators(String response) {
        String lower = response.toLowerCase();
        return lower.contains("error:") ||
               lower.contains("failed:") ||
               lower.contains("exception:") ||
               lower.contains("cannot") ||
               lower.contains("unable to");
    }

    /**
     * Extracts error message from response
     */
    private String extractErrorMessage(String response) {
        // Simple extraction - look for lines containing error indicators
        String[] lines = response.split("\n");
        for (String line : lines) {
            String lower = line.toLowerCase();
            if (lower.contains("error") || lower.contains("failed")) {
                return line.trim();
            }
        }
        return "Task execution failed";
    }

    /**
     * Asks user what to do after an error
     */
    private String askForErrorAction(TaskExecutionResult result) {
        System.out.println();
        System.out.println(AnsiColors.colorize("What would you like to do?", AnsiColors.BOLD_WHITE));
        System.out.println("  " + AnsiColors.colorize("r)", AnsiColors.GREEN) + " Retry this task");
        System.out.println("  " + AnsiColors.colorize("s)", AnsiColors.YELLOW) + " Skip and continue to next task");
        System.out.println("  " + AnsiColors.colorize("q)", AnsiColors.RED) + " Stop execution");
        System.out.println();

        while (true) {
            String choice = consoleReader.readLine(
                AnsiColors.colorize("Your choice [r/s/q]: ", AnsiColors.BOLD_WHITE)
            ).trim().toLowerCase();

            switch (choice) {
                case "r", "retry":
                    return "retry";
                case "s", "skip":
                    return "skip";
                case "q", "quit", "stop":
                    return "stop";
                default:
                    System.out.println(AnsiColors.warning("Invalid choice. Please enter r, s, or q."));
            }
        }
    }

    /**
     * Stops execution (can be called from signal handlers)
     */
    public void stopExecution() {
        shouldStop = true;
    }

    /**
     * Checks if execution is in progress
     */
    public boolean isExecutionInProgress() {
        return executionInProgress;
    }

    // === Data Classes ===

    /**
     * Result of task execution
     */
    private static class TaskExecutionResult {
        final boolean success;
        final String error;

        TaskExecutionResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }
    }

    /**
     * Result of plan execution
     */
    public static class ExecutionResult {
        public final boolean success;
        public final String message;

        public ExecutionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
