package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.model.ProjectType;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class ExecuteTool extends AbstractTool {

    public ExecuteTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@execute";
    }

    @Override
    public String getDescription() {
        return "Exécute les tests ou build du projet";
    }

    @Override
    public String getUsage() {
        return "@execute [test|build|run]";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        String command = args.isEmpty() ? "test" : args.trim().toLowerCase();

        try {
            String[] cmdArray = buildCommand(command, context);
            if (cmdArray == null) {
                return formatError("Type de projet non supporté pour l'exécution: " +
                                  context.getProjectType().getDisplayName());
            }

            return formatInfo("Exécution: " + String.join(" ", cmdArray) + "\n\n") +
                   executeCommand(cmdArray, context.getRootPath());

        } catch (Exception e) {
            return formatError("Erreur lors de l'exécution: " + e.getMessage());
        }
    }

    private String[] buildCommand(String action, ProjectContext context) {
        ProjectType type = context.getProjectType();

        return switch (type) {
            case JAVA_MAVEN -> switch (action) {
                case "test" -> new String[]{"mvn", "test"};
                case "build" -> new String[]{"mvn", "clean", "package"};
                case "run" -> new String[]{"mvn", "spring-boot:run"};
                default -> null;
            };
            case JAVA_GRADLE -> switch (action) {
                case "test" -> new String[]{"./gradlew", "test"};
                case "build" -> new String[]{"./gradlew", "build"};
                case "run" -> new String[]{"./gradlew", "bootRun"};
                default -> null;
            };
            case PYTHON -> switch (action) {
                case "test" -> new String[]{"pytest"};
                case "run" -> new String[]{"python", "main.py"};
                default -> null;
            };
            case NODE_JS, TYPESCRIPT -> switch (action) {
                case "test" -> new String[]{"npm", "test"};
                case "build" -> new String[]{"npm", "run", "build"};
                case "run" -> new String[]{"npm", "start"};
                default -> null;
            };
            case GO -> switch (action) {
                case "test" -> new String[]{"go", "test", "./..."};
                case "build" -> new String[]{"go", "build"};
                case "run" -> new String[]{"go", "run", "."};
                default -> null;
            };
            case RUST -> switch (action) {
                case "test" -> new String[]{"cargo", "test"};
                case "build" -> new String[]{"cargo", "build"};
                case "run" -> new String[]{"cargo", "run"};
                default -> null;
            };
            default -> null;
        };
    }

    private String executeCommand(String[] command, Path workingDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 100) {
                output.append(line).append("\n");
                lineCount++;
            }
            if (lineCount >= 100) {
                output.append("\n... (sortie tronquée, trop de lignes)\n");
            }
        }

        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return formatError("Timeout: commande interrompue après 60s\n\n" + output);
        }

        int exitCode = process.exitValue();
        String result = output.toString();

        if (exitCode == 0) {
            return formatSuccess("Exécution réussie (exit code: 0)\n\n") + result;
        } else {
            return formatError("Exécution échouée (exit code: " + exitCode + ")\n\n") + result;
        }
    }
}
