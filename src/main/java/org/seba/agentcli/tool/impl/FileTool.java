package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileTool extends AbstractTool {

    public FileTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@file";
    }

    @Override
    public String getDescription() {
        return "Lit et analyse un fichier";
    }

    @Override
    public String getUsage() {
        return "@file <chemin/vers/fichier>";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args.isEmpty()) {
            return formatError("Chemin de fichier requis. Usage: " + getUsage());
        }

        try {
            Path filePath = Paths.get(args);

            // Security: Prevent path traversal attacks
            if (!isPathSafe(filePath)) {
                return formatError("Accès refusé: le fichier doit être dans le répertoire du projet");
            }

            if (!Files.exists(filePath)) {
                return formatError("Fichier introuvable: " + args);
            }

            if (!Files.isRegularFile(filePath)) {
                return formatError("Pas un fichier régulier: " + args);
            }

            long size = Files.size(filePath);
            if (size > 1_000_000) { // 1MB limit
                return formatError("Fichier trop volumineux (> 1MB): " + args);
            }

            String content = Files.readString(filePath);
            String prompt = String.format(
                "Voici le contenu du fichier %s (%d lignes):\n```\n%s\n```\n\nAnalyse ce fichier et dis-moi ce qu'il fait.",
                filePath.getFileName(), content.lines().count(), content
            );

            return cliService.query(prompt);

        } catch (Exception e) {
            return formatError("Erreur lors de la lecture: " + e.getMessage());
        }
    }

    /**
     * Validates that a file path is within the project directory
     * to prevent path traversal attacks.
     *
     * @param filePath The file path to validate
     * @return true if the path is safe, false otherwise
     */
    private boolean isPathSafe(Path filePath) {
        try {
            Path projectRoot = Paths.get(".").toAbsolutePath().normalize();
            Path normalizedFile = filePath.toAbsolutePath().normalize();

            // File must be within project directory
            if (!normalizedFile.startsWith(projectRoot)) {
                return false;
            }

            // Deny access to .git directory and other sensitive files
            String pathString = normalizedFile.toString();
            if (pathString.contains("/.git/") || pathString.endsWith("/.git")) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
