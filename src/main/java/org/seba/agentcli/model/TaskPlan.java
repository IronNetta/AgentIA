package org.seba.agentcli.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente un plan de tâches pour une opération complexe
 * Similaire au système TodoWrite de Claude Code
 */
public class TaskPlan {

    private String description;
    private List<TaskItem> tasks;
    private LocalDateTime createdAt;
    private TaskStatus overallStatus;

    public TaskPlan() {
        this.tasks = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.overallStatus = TaskStatus.PENDING;
    }

    public TaskPlan(String description) {
        this();
        this.description = description;
    }

    public void addTask(String taskDescription) {
        tasks.add(new TaskItem(tasks.size() + 1, taskDescription));
    }

    public void addTask(TaskItem task) {
        tasks.add(task);
    }

    public TaskItem getCurrentTask() {
        return tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);
    }

    public TaskItem getNextPendingTask() {
        return tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING)
                .findFirst()
                .orElse(null);
    }

    public void markTaskCompleted(int taskNumber) {
        if (taskNumber > 0 && taskNumber <= tasks.size()) {
            tasks.get(taskNumber - 1).setStatus(TaskStatus.COMPLETED);
            updateOverallStatus();
        }
    }

    public void markTaskInProgress(int taskNumber) {
        if (taskNumber > 0 && taskNumber <= tasks.size()) {
            tasks.get(taskNumber - 1).setStatus(TaskStatus.IN_PROGRESS);
        }
    }

    public void markTaskFailed(int taskNumber, String error) {
        if (taskNumber > 0 && taskNumber <= tasks.size()) {
            TaskItem task = tasks.get(taskNumber - 1);
            task.setStatus(TaskStatus.FAILED);
            task.setError(error);
            updateOverallStatus();
        }
    }

    private void updateOverallStatus() {
        long completed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long failed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.FAILED).count();
        long inProgress = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();

        if (failed > 0) {
            this.overallStatus = TaskStatus.FAILED;
        } else if (completed == tasks.size()) {
            this.overallStatus = TaskStatus.COMPLETED;
        } else if (inProgress > 0) {
            this.overallStatus = TaskStatus.IN_PROGRESS;
        } else {
            this.overallStatus = TaskStatus.PENDING;
        }
    }

    public int getCompletedCount() {
        return (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
    }

    public int getTotalCount() {
        return tasks.size();
    }

    public double getProgressPercentage() {
        if (tasks.isEmpty()) return 0.0;
        return (getCompletedCount() * 100.0) / getTotalCount();
    }

    public boolean isComplete() {
        return overallStatus == TaskStatus.COMPLETED;
    }

    public boolean hasFailed() {
        return overallStatus == TaskStatus.FAILED;
    }

    // Getters and setters
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TaskItem> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskItem> tasks) {
        this.tasks = tasks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TaskStatus getOverallStatus() {
        return overallStatus;
    }

    /**
     * Représente une tâche individuelle dans le plan
     */
    public static class TaskItem {
        private int number;
        private String description;
        private TaskStatus status;
        private String error;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;

        public TaskItem(int number, String description) {
            this.number = number;
            this.description = description;
            this.status = TaskStatus.PENDING;
        }

        public void setStatus(TaskStatus status) {
            this.status = status;
            if (status == TaskStatus.IN_PROGRESS) {
                this.startedAt = LocalDateTime.now();
            } else if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
                this.completedAt = LocalDateTime.now();
            }
        }

        // Getters and setters
        public int getNumber() {
            return number;
        }

        public String getDescription() {
            return description;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public LocalDateTime getStartedAt() {
            return startedAt;
        }

        public LocalDateTime getCompletedAt() {
            return completedAt;
        }

        @Override
        public String toString() {
            String statusSymbol = switch (status) {
                case PENDING -> "⋯";
                case IN_PROGRESS -> "→";
                case COMPLETED -> "✓";
                case FAILED -> "✗";
            };
            return String.format("[%s] %d. %s", statusSymbol, number, description);
        }
    }

    /**
     * Statut d'une tâche ou du plan global
     */
    public enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}
