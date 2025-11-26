package org.seba.agentcli;

import org.seba.agentcli.detector.ProjectDetector;
import org.seba.agentcli.files.FileOperationExecutor;
import org.seba.agentcli.files.FileOperationParser;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.Tool;
import org.seba.agentcli.tool.ToolRegistry;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

@Component
public class CommandProcessor {

    private final CliService cliService;
    private final ToolRegistry toolRegistry;
    private final ProjectDetector projectDetector;
    private final FileOperationParser fileOperationParser;
    private final FileOperationExecutor fileOperationExecutor;
    private final org.seba.agentcli.context.TaskComplexityAnalyzer complexityAnalyzer;
    private final org.seba.agentcli.context.PlanManager planManager;
    private ProjectContext projectContext;

    public CommandProcessor(CliService cliService,
                          ToolRegistry toolRegistry,
                          ProjectDetector projectDetector,
                          FileOperationParser fileOperationParser,
                          FileOperationExecutor fileOperationExecutor,
                          org.seba.agentcli.context.TaskComplexityAnalyzer complexityAnalyzer,
                          org.seba.agentcli.context.PlanManager planManager) {
        this.cliService = cliService;
        this.toolRegistry = toolRegistry;
        this.projectDetector = projectDetector;
        this.fileOperationParser = fileOperationParser;
        this.fileOperationExecutor = fileOperationExecutor;
        this.complexityAnalyzer = complexityAnalyzer;
        this.planManager = planManager;
        initializeProject();
    }

    private void initializeProject() {
        try {
            this.projectContext = projectDetector.detectProject(Paths.get("."));
            System.out.println("Project detected: " + projectContext.getProjectType().getDisplayName());
            System.out.println("Files indexed: " + projectContext.getSourceFiles().size());
            if (!projectContext.getFrameworks().isEmpty()) {
                System.out.println("Frameworks: " + String.join(", ", projectContext.getFrameworks()));
            }

            // Pass project context to CliService
            cliService.setProjectContext(projectContext);
        } catch (IOException e) {
            System.err.println("Warning: Error during project detection: " + e.getMessage());
            System.err.println("Running in degraded mode");
        }
    }

    public String processCommand(String input) {
        String trimmed = input.trim();

        // Check if it's a tool command
        Optional<Tool> tool = toolRegistry.findTool(trimmed);
        if (tool.isPresent()) {
            try {
                String args = tool.get().extractArgs(trimmed);
                String result = tool.get().execute(args, projectContext);

                // For certain tools, we might want to capture insights about the project
                if ("@analyze-project".equals(tool.get().getName())) {
                    cliService.addProjectInsight("Architecture du projet analysée: " + result.substring(0, Math.min(100, result.length())) + "...");
                }

                return result;
            } catch (Exception e) {
                return "❌ Erreur lors de l'exécution: " + e.getMessage();
            }
        }

        // Analyze task complexity before sending to LLM
        org.seba.agentcli.context.TaskComplexityAnalyzer.ComplexityResult complexity =
                complexityAnalyzer.analyze(input);

        // If task is complex and no plan exists, suggest creating one
        if (complexityAnalyzer.shouldSuggestPlan(complexity) && !planManager.hasPlan()) {
            System.out.println("\n");
            System.out.println(org.seba.agentcli.io.AnsiColors.colorize(
                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    org.seba.agentcli.io.AnsiColors.CYAN));
            System.out.println(org.seba.agentcli.io.AnsiColors.colorize(
                    "  COMPLEX TASK DETECTED",
                    org.seba.agentcli.io.AnsiColors.BOLD_CYAN));
            System.out.println(org.seba.agentcli.io.AnsiColors.colorize(
                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    org.seba.agentcli.io.AnsiColors.CYAN));
            System.out.println();
            System.out.println("This task appears to be complex and may require multiple steps.");
            System.out.println("Complexity: " + org.seba.agentcli.io.AnsiColors.colorize(
                    complexity.getLevel().toString(),
                    org.seba.agentcli.io.AnsiColors.YELLOW));
            System.out.println();
            System.out.println("RECOMMENDATION:");
            System.out.println("  Create a detailed plan first to organize the work");
            System.out.println();
            System.out.println("Would you like me to:");
            System.out.println("  " + org.seba.agentcli.io.AnsiColors.colorize("a)", org.seba.agentcli.io.AnsiColors.GREEN) +
                             " Create a plan first (recommended)");
            System.out.println("  " + org.seba.agentcli.io.AnsiColors.colorize("b)", org.seba.agentcli.io.AnsiColors.YELLOW) +
                             " Proceed directly without plan");
            System.out.println();

            // Read user choice immediately
            String choice;
            try {
                System.out.print(org.seba.agentcli.io.AnsiColors.colorize("Your choice [a/b]: ",
                               org.seba.agentcli.io.AnsiColors.BOLD_WHITE));

                // We need to read the choice directly from console
                choice = System.console().readLine().trim().toLowerCase();
            } catch (Exception e) {
                // Fallback if System.console() is not available
                choice = "b"; // Default to proceeding without plan
                System.out.println("\nDefaulting to proceed without plan (b)");
            }

            if (choice.equals("a") || choice.equals("a)")) {
                // Create a plan by asking the LLM to generate one for the current input
                System.out.println("\nCreating detailed plan with LLM assistance...\n");

                // Use PlanTool directly to create a plan
                Optional<Tool> planTool = toolRegistry.findTool("@plan");
                if (planTool.isPresent()) {
                    String planResult = planTool.get().execute("create " + input, projectContext);

                    if (planResult.contains("Plan created successfully!")) {
                        // Ask the LLM to explain the plan and ask for confirmation
                        String explanationPrompt = "Explain this plan and ask for confirmation before proceeding: " + input;
                        return planResult + "\n\n" + cliService.query(explanationPrompt);
                    } else {
                        // If plan creation fails, proceed without plan
                        System.out.println("Plan creation failed. Proceeding with original request...\n");
                        return cliService.query(input);
                    }
                } else {
                    // If plan tool is not found, proceed without plan
                    return cliService.query(input);
                }
            } else {
                // Default behavior: proceed directly
                return cliService.query(input);
            }
        }

        // Normal query to LLM
        String response = cliService.query(input);

        // Parse la réponse pour détecter les opérations de fichiers
        FileOperationParser.ParseResult parseResult = fileOperationParser.parse(response);

        // Si des opérations sont détectées, les exécuter
        if (parseResult.hasOperations()) {
            fileOperationExecutor.execute(parseResult.getOperations());

            // Retourner la réponse nettoyée (sans les balises)
            return parseResult.getCleanedResponse();
        }

        return response;
    }

    public ProjectContext getProjectContext() {
        return projectContext;
    }

    public void reindexProject() {
        System.out.println("Reindexing project...");
        initializeProject();
    }
}