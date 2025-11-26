package org.seba.agentcli.context;

import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.seba.agentcli.model.TaskPlan;
import org.springframework.stereotype.Component;

/**
 * Gère le plan de tâches actif
 * Similaire au système TodoWrite de Claude Code
 */
@Component
public class PlanManager {

    private TaskPlan currentPlan;

    public void createPlan(String description) {
        this.currentPlan = new TaskPlan(description);
    }

    public void addTask(String taskDescription) {
        if (currentPlan != null) {
            currentPlan.addTask(taskDescription);
        }
    }

    public void startTask(int taskNumber) {
        if (currentPlan != null) {
            currentPlan.markTaskInProgress(taskNumber);
        }
    }

    public void completeTask(int taskNumber) {
        if (currentPlan != null) {
            currentPlan.markTaskCompleted(taskNumber);
        }
    }

    public void failTask(int taskNumber, String error) {
        if (currentPlan != null) {
            currentPlan.markTaskFailed(taskNumber, error);
        }
    }

    public TaskPlan getCurrentPlan() {
        return currentPlan;
    }

    public boolean hasPlan() {
        return currentPlan != null;
    }

    public void clearPlan() {
        this.currentPlan = null;
    }

    /**
     * Affiche le plan actuel de manière formatée
     */
    public String displayPlan() {
        if (currentPlan == null) {
            return "No active plan.";
        }

        StringBuilder output = new StringBuilder();
        output.append("\n");
        output.append(BoxDrawer.drawSeparator("TASK PLAN", 70, AnsiColors.CYAN));
        output.append("\n\n");

        if (currentPlan.getDescription() != null) {
            output.append(AnsiColors.colorize("Goal: ", AnsiColors.BOLD_WHITE))
                  .append(currentPlan.getDescription())
                  .append("\n\n");
        }

        // Progress bar
        int progress = (int) currentPlan.getProgressPercentage();
        output.append(AnsiColors.colorize("Progress: ", AnsiColors.BOLD_WHITE))
              .append(createProgressBar(progress, 40))
              .append(String.format(" %d%%", progress))
              .append(String.format(" (%d/%d tasks)",
                      currentPlan.getCompletedCount(),
                      currentPlan.getTotalCount()))
              .append("\n\n");

        // Tasks list
        output.append(AnsiColors.colorize("Tasks:\n", AnsiColors.BOLD_WHITE));
        for (TaskPlan.TaskItem task : currentPlan.getTasks()) {
            output.append(formatTask(task)).append("\n");
        }

        output.append("\n");
        output.append(BoxDrawer.drawSeparator("", 70, AnsiColors.CYAN));

        return output.toString();
    }

    /**
     * Affiche un résumé compact du plan
     */
    public String displayCompactPlan() {
        if (currentPlan == null) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        int completed = currentPlan.getCompletedCount();
        int total = currentPlan.getTotalCount();

        output.append(AnsiColors.colorize(String.format("[Plan: %d/%d] ", completed, total),
                                         AnsiColors.BRIGHT_BLACK));

        TaskPlan.TaskItem current = currentPlan.getCurrentTask();
        if (current != null) {
            output.append(AnsiColors.colorize("→ ", AnsiColors.CYAN))
                  .append(current.getDescription());
        } else if (currentPlan.isComplete()) {
            output.append(AnsiColors.colorize("✓ Complete", AnsiColors.GREEN));
        } else {
            TaskPlan.TaskItem next = currentPlan.getNextPendingTask();
            if (next != null) {
                output.append(AnsiColors.colorize("Next: ", AnsiColors.YELLOW))
                      .append(next.getDescription());
            }
        }

        return output.toString();
    }

    /**
     * Formate une tâche individuelle avec couleurs
     */
    private String formatTask(TaskPlan.TaskItem task) {
        String prefix;
        String color;

        switch (task.getStatus()) {
            case PENDING:
                prefix = "[ ]";
                color = AnsiColors.BRIGHT_BLACK;
                break;
            case IN_PROGRESS:
                prefix = "[→]";
                color = AnsiColors.CYAN;
                break;
            case COMPLETED:
                prefix = "[✓]";
                color = AnsiColors.GREEN;
                break;
            case FAILED:
                prefix = "[✗]";
                color = AnsiColors.RED;
                break;
            default:
                prefix = "[ ]";
                color = AnsiColors.WHITE;
        }

        String line = String.format("%s %d. %s", prefix, task.getNumber(), task.getDescription());

        if (task.getError() != null) {
            line += "\n    " + AnsiColors.colorize("Error: " + task.getError(), AnsiColors.RED);
        }

        return AnsiColors.colorize(line, color);
    }

    /**
     * Crée une barre de progression
     */
    private String createProgressBar(int percentage, int width) {
        int filled = (int) (width * percentage / 100.0);
        int empty = width - filled;

        String bar = AnsiColors.colorize("█".repeat(filled), AnsiColors.GREEN) +
                     AnsiColors.colorize("░".repeat(empty), AnsiColors.BRIGHT_BLACK);

        return "[" + bar + "]";
    }

    /**
     * Retourne un résumé textuel pour le contexte LLM
     */
    public String getPlanSummaryForLLM() {
        if (currentPlan == null) {
            return "";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("CURRENT PLAN:\n");
        summary.append("Goal: ").append(currentPlan.getDescription()).append("\n");
        summary.append("Progress: ").append(currentPlan.getCompletedCount())
               .append("/").append(currentPlan.getTotalCount()).append(" tasks completed\n");

        summary.append("Tasks:\n");
        for (TaskPlan.TaskItem task : currentPlan.getTasks()) {
            summary.append("  ").append(task.getNumber()).append(". ");
            summary.append("[").append(task.getStatus()).append("] ");
            summary.append(task.getDescription()).append("\n");
        }

        return summary.toString();
    }
}
