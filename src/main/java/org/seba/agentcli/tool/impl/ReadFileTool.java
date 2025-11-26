package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.files.FileReaderService;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

/**
 * Tool pour lire et afficher des fichiers avec numérotation
 */
@Component
public class ReadFileTool extends AbstractTool {

    private final FileReaderService fileReaderService;

    public ReadFileTool(CliService cliService, FileReaderService fileReaderService) {
        super(cliService);
        this.fileReaderService = fileReaderService;
    }

    @Override
    public String getName() {
        return "@read";
    }

    @Override
    public String getDescription() {
        return "Lit et affiche un fichier avec numérotation de lignes";
    }

    @Override
    public String getUsage() {
        return "@read <chemin/vers/fichier> [--lines N] [--offset M]";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args.isEmpty()) {
            return formatError("Chemin de fichier requis. Usage: " + getUsage());
        }

        try {
            // Parse arguments
            String[] parts = args.split("\\s+");
            String filePath = parts[0];
            int lines = -1;
            int offset = 0;

            // Parse options
            for (int i = 1; i < parts.length; i++) {
                if (parts[i].equals("--lines") && i + 1 < parts.length) {
                    lines = Integer.parseInt(parts[i + 1]);
                    i++;
                } else if (parts[i].equals("--offset") && i + 1 < parts.length) {
                    offset = Integer.parseInt(parts[i + 1]);
                    i++;
                }
            }

            // Read file
            FileReaderService.FileContent content;
            if (lines > 0) {
                content = fileReaderService.readFileRange(filePath, offset, lines);
            } else {
                content = fileReaderService.readFile(filePath);
            }

            // Display
            return fileReaderService.displayFile(content);

        } catch (NumberFormatException e) {
            return formatError("Format de nombre invalide");
        } catch (Exception e) {
            return formatError("Erreur lors de la lecture: " + e.getMessage());
        }
    }
}
