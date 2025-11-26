package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AnalyzeProjectTool extends AbstractTool {

    public AnalyzeProjectTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@analyze-project";
    }

    @Override
    public String getDescription() {
        return "Analyse complète de la structure et de l'architecture du projet";
    }

    @Override
    public String getUsage() {
        return "@analyze-project";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        try {
            StringBuilder analysis = new StringBuilder();
            analysis.append("=== Analyse du Projet ===\n\n");
            analysis.append("Type: ").append(context.getProjectType().getDisplayName()).append("\n");
            analysis.append("Fichiers sources: ").append(context.getSourceFiles().size()).append("\n");
            analysis.append("Frameworks détectés: ").append(
                context.getFrameworks().isEmpty() ? "Aucun" : String.join(", ", context.getFrameworks())
            ).append("\n\n");

            // Group files by directory
            Map<String, Long> filesByDir = context.getSourceFiles().stream()
                .collect(Collectors.groupingBy(
                    p -> p.getParent() != null ?
                        context.getRootPath().relativize(p.getParent()).toString() : "root",
                    Collectors.counting()
                ));

            analysis.append("Structure des répertoires:\n");
            filesByDir.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> analysis.append("  - ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append(" fichiers\n"));

            // File list for AI analysis
            String fileList = context.getSourceFiles().stream()
                .limit(50)
                .map(p -> context.getRootPath().relativize(p).toString())
                .collect(Collectors.joining("\n"));

            String prompt = String.format(
                "Analyse cette structure de projet %s:\n\n%s\n\nFichiers principaux:\n%s\n\n" +
                "Frameworks: %s\n\nDonne une analyse de l'architecture, suggestions d'amélioration et points d'attention.",
                context.getProjectType().getDisplayName(),
                analysis.toString(),
                fileList,
                String.join(", ", context.getFrameworks())
            );

            return analysis.toString() + "\n" + cliService.query(prompt);

        } catch (Exception e) {
            return formatError("Erreur lors de l'analyse: " + e.getMessage());
        }
    }
}
