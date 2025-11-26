package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.model.ProjectType;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Component
public class RefactorTool extends AbstractTool {

    public RefactorTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@refactor";
    }

    @Override
    public String getDescription() {
        return "Suggère des améliorations et refactorings pour un fichier";
    }

    @Override
    public String getUsage() {
        return "@refactor <fichier>";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args.isEmpty()) {
            return formatError("Fichier requis. Usage: " + getUsage());
        }

        try {
            // Find the source file
            Optional<Path> sourceFile = findSourceFile(args, context);
            if (sourceFile.isEmpty()) {
                return formatError("Fichier introuvable: " + args);
            }

            Path file = sourceFile.get();
            String content = Files.readString(file);

            String prompt = buildRefactorPrompt(file, content, context.getProjectType());
            return cliService.query(prompt);

        } catch (Exception e) {
            return formatError("Erreur lors de l'analyse: " + e.getMessage());
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

    private String buildRefactorPrompt(Path file, String content, ProjectType type) {
        return String.format(
            "Analyse ce code %s et suggère des améliorations de refactoring:\n\n" +
            "Fichier: %s\n" +
            "```%s\n%s\n```\n\n" +
            "Fournis:\n" +
            "1. 🔍 Analyse de la qualité du code\n" +
            "2. 💡 Suggestions de refactoring avec exemples\n" +
            "3. ⚡ Optimisations possibles\n" +
            "4. 📝 Améliorations de lisibilité\n" +
            "5. 🛡️ Problèmes de sécurité ou bugs potentiels\n" +
            "6. 🎯 Respect des best practices de %s\n\n" +
            "Pour chaque suggestion, explique le pourquoi et donne un exemple de code amélioré.",
            type.getDisplayName(),
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
