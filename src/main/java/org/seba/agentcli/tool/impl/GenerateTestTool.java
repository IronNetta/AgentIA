package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.model.ProjectType;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.nio.file.*;
import java.util.Optional;

@Component
public class GenerateTestTool extends AbstractTool {

    public GenerateTestTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@generate-test";
    }

    @Override
    public String getDescription() {
        return "Génère des tests unitaires pour une classe/fonction";
    }

    @Override
    public String getUsage() {
        return "@generate-test <fichier_ou_classe>";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args.isEmpty()) {
            return formatError("Fichier ou classe requis. Usage: " + getUsage());
        }

        try {
            // Find the source file
            Optional<Path> sourceFile = findSourceFile(args, context);
            if (sourceFile.isEmpty()) {
                return formatError("Fichier introuvable: " + args);
            }

            Path file = sourceFile.get();
            String content = Files.readString(file);
            String testFramework = getTestFramework(context);

            String prompt = buildTestPrompt(file, content, testFramework, context.getProjectType());
            return cliService.query(prompt);

        } catch (Exception e) {
            return formatError("Erreur lors de la génération: " + e.getMessage());
        }
    }

    private Optional<Path> findSourceFile(String input, ProjectContext context) {
        // Try as direct path first
        Path directPath = Paths.get(input);
        if (Files.exists(directPath)) {
            return Optional.of(directPath);
        }

        // Search in project files
        return context.getSourceFiles().stream()
            .filter(p -> p.toString().contains(input) ||
                        p.getFileName().toString().equals(input))
            .findFirst();
    }

    private String getTestFramework(ProjectContext context) {
        if (context.getFrameworks().contains("JUnit")) return "JUnit 5";
        if (context.getFrameworks().contains("pytest")) return "pytest";
        if (context.getFrameworks().contains("Jest")) return "Jest";

        // Default by project type
        return switch (context.getProjectType()) {
            case JAVA_MAVEN, JAVA_GRADLE -> "JUnit 5";
            case PYTHON -> "pytest";
            case NODE_JS, TYPESCRIPT -> "Jest";
            case GO -> "testing package";
            case RUST -> "Rust test";
            default -> "framework de test standard";
        };
    }

    private String buildTestPrompt(Path file, String content, String framework, ProjectType type) {
        return String.format(
            "Génère des tests unitaires complets avec %s pour ce fichier %s:\n\n" +
            "```%s\n%s\n```\n\n" +
            "Inclus:\n" +
            "- Tests des cas normaux\n" +
            "- Tests des cas limites\n" +
            "- Tests des erreurs\n" +
            "- Mocks si nécessaire\n" +
            "- Documentation des tests\n\n" +
            "Respecte les conventions de %s.",
            framework,
            file.getFileName(),
            getLanguageForMarkdown(type),
            content,
            type.getDisplayName()
        );
    }

    private String getLanguageForMarkdown(ProjectType type) {
        return switch (type) {
            case JAVA_MAVEN, JAVA_GRADLE -> "java";
            case PYTHON -> "python";
            case NODE_JS -> "javascript";
            case TYPESCRIPT -> "typescript";
            case GO -> "go";
            case RUST -> "rust";
            case CSHARP -> "csharp";
            case PHP -> "php";
            case RUBY -> "ruby";
            default -> "";
        };
    }
}
