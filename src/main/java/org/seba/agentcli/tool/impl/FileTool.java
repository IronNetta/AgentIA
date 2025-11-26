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
}
