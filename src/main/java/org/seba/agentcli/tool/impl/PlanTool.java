package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.context.PlanManager;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.model.TaskPlan;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

/**
 * Outil pour créer et gérer des plans de tâches
 * Similaire au TodoWrite de Claude Code
 */
@Component
public class PlanTool extends AbstractTool {

    private final PlanManager planManager;
    private final org.seba.agentcli.context.PlanExecutor planExecutor;

    public PlanTool(CliService cliService, PlanManager planManager,
                   org.seba.agentcli.context.PlanExecutor planExecutor) {
        super(cliService);
        this.planManager = planManager;
        this.planExecutor = planExecutor;
    }

    @Override
    public String getName() {
        return "@plan";
    }

    @Override
    public String getDescription() {
        return "Create, view and manage task plans for complex operations";
    }

    @Override
    public String getUsage() {
        return """
               @plan create <description>  - Create a new plan with LLM assistance
               @plan show                   - Display the current plan
               @plan execute                - Execute the plan automatically
               @plan start <task_number>    - Mark a task as in progress
               @plan complete <task_number> - Mark a task as completed
               @plan fail <task_number>     - Mark a task as failed
               @plan clear                  - Clear the current plan

               Examples:
                 @plan create Add JWT authentication
                 @plan show
                 @plan execute
                 @plan start 1
                 @plan complete 1
               """;
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args.isEmpty()) {
            return planManager.hasPlan() ? planManager.displayPlan() : formatInfo(getUsage());
        }

        String[] parts = args.trim().split("\\s+", 2);
        String subCommand = parts[0].toLowerCase();

        return switch (subCommand) {
            case "create" -> createPlan(parts.length > 1 ? parts[1] : "", context);
            case "show" -> showPlan();
            case "execute" -> executePlan(context);
            case "start" -> startTask(parts.length > 1 ? parts[1] : "");
            case "complete" -> completeTask(parts.length > 1 ? parts[1] : "");
            case "fail" -> failTask(parts.length > 1 ? parts[1] : "");
            case "clear" -> clearPlan();
            default -> formatError("Unknown subcommand. Usage:\n" + getUsage());
        };
    }

    /**
     * Crée un plan avec l'aide du LLM
     */
    private String createPlan(String description, ProjectContext context) {
        if (description.isEmpty()) {
            return formatError("Description required. Usage: @plan create <description>");
        }

        // Demander au LLM de créer un plan détaillé
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a detailed task plan for the following goal:\n\n");
        prompt.append("Goal: ").append(description).append("\n\n");

        if (context != null) {
            prompt.append("Project Context:\n");
            prompt.append("- Type: ").append(context.getProjectType().getDisplayName()).append("\n");
            if (!context.getFrameworks().isEmpty()) {
                prompt.append("- Frameworks: ").append(String.join(", ", context.getFrameworks())).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("""
                Create a step-by-step plan with 3-8 specific, actionable tasks.
                Each task should be clear and focused on one thing.

                Format your response EXACTLY as follows (nothing else):
                PLAN: <one-line summary>
                1. <task description>
                2. <task description>
                3. <task description>
                ...

                Example:
                PLAN: Add JWT authentication to Spring Boot application
                1. Add Spring Security and JWT dependencies to pom.xml
                2. Create JwtUtil class for token generation and validation
                3. Create SecurityConfig to configure authentication
                4. Create AuthController with login endpoint
                5. Add authentication filters
                6. Create unit tests for JWT functionality
                """);

        System.out.println("Creating plan with LLM assistance...\n");
        String llmResponse = cliService.query(prompt.toString());

        // Parser la réponse
        try {
            String[] lines = llmResponse.split("\n");
            String planDescription = description;

            // Chercher la ligne PLAN:
            for (String line : lines) {
                if (line.trim().startsWith("PLAN:")) {
                    planDescription = line.substring(line.indexOf(":") + 1).trim();
                    break;
                }
            }

            planManager.createPlan(planDescription);

            // Ajouter les tâches
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.matches("^\\d+\\.\\s+.+")) {
                    // Ligne de type "1. Task description"
                    String taskDescription = trimmed.substring(trimmed.indexOf('.') + 1).trim();
                    planManager.addTask(taskDescription);
                }
            }

            if (planManager.getCurrentPlan().getTotalCount() == 0) {
                planManager.clearPlan();
                return formatError("Failed to parse LLM response. No tasks extracted.");
            }

            return formatSuccess("Plan created successfully!\n\n") + planManager.displayPlan();

        } catch (Exception e) {
            planManager.clearPlan();
            return formatError("Failed to create plan: " + e.getMessage());
        }
    }

    /**
     * Affiche le plan actuel
     */
    private String showPlan() {
        if (!planManager.hasPlan()) {
            return formatInfo("No active plan. Create one with: @plan create <description>");
        }

        return planManager.displayPlan();
    }

    /**
     * Démarre une tâche
     */
    private String startTask(String taskNumberStr) {
        if (!planManager.hasPlan()) {
            return formatError("No active plan");
        }

        try {
            int taskNumber = Integer.parseInt(taskNumberStr);
            TaskPlan plan = planManager.getCurrentPlan();

            if (taskNumber < 1 || taskNumber > plan.getTotalCount()) {
                return formatError("Invalid task number. Valid range: 1-" + plan.getTotalCount());
            }

            planManager.startTask(taskNumber);
            return formatSuccess("Task " + taskNumber + " started\n\n") + planManager.displayCompactPlan();

        } catch (NumberFormatException e) {
            return formatError("Invalid task number");
        }
    }

    /**
     * Marque une tâche comme terminée
     */
    private String completeTask(String taskNumberStr) {
        if (!planManager.hasPlan()) {
            return formatError("No active plan");
        }

        try {
            int taskNumber = Integer.parseInt(taskNumberStr);
            TaskPlan plan = planManager.getCurrentPlan();

            if (taskNumber < 1 || taskNumber > plan.getTotalCount()) {
                return formatError("Invalid task number. Valid range: 1-" + plan.getTotalCount());
            }

            planManager.completeTask(taskNumber);

            String result = formatSuccess("Task " + taskNumber + " completed!\n\n");
            result += planManager.displayCompactPlan();

            if (plan.isComplete()) {
                result += "\n\n" + formatSuccess("All tasks completed! Plan finished.");
            }

            return result;

        } catch (NumberFormatException e) {
            return formatError("Invalid task number");
        }
    }

    /**
     * Marque une tâche comme échouée
     */
    private String failTask(String args) {
        if (!planManager.hasPlan()) {
            return formatError("No active plan");
        }

        String[] parts = args.split("\\s+", 2);
        if (parts.length < 1) {
            return formatError("Usage: @plan fail <task_number> [error_message]");
        }

        try {
            int taskNumber = Integer.parseInt(parts[0]);
            String error = parts.length > 1 ? parts[1] : "Task failed";

            TaskPlan plan = planManager.getCurrentPlan();
            if (taskNumber < 1 || taskNumber > plan.getTotalCount()) {
                return formatError("Invalid task number. Valid range: 1-" + plan.getTotalCount());
            }

            planManager.failTask(taskNumber, error);
            return formatError("Task " + taskNumber + " marked as failed\n\n") + planManager.displayPlan();

        } catch (NumberFormatException e) {
            return formatError("Invalid task number");
        }
    }

    /**
     * Efface le plan actuel
     */
    private String clearPlan() {
        if (!planManager.hasPlan()) {
            return formatInfo("No active plan to clear");
        }

        planManager.clearPlan();
        return formatSuccess("Plan cleared");
    }

    /**
     * Executes the plan automatically
     */
    private String executePlan(ProjectContext context) {
        if (!planManager.hasPlan()) {
            return formatError("No active plan to execute");
        }

        if (planExecutor.isExecutionInProgress()) {
            return formatError("Plan execution already in progress");
        }

        org.seba.agentcli.context.PlanExecutor.ExecutionResult result = planExecutor.executePlan(context);

        if (result.success) {
            return formatSuccess(result.message);
        } else {
            return formatError(result.message);
        }
    }
}
