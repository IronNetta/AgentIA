package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.files.FileWriterService;
import org.seba.agentcli.io.ConsoleReader;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

/**
 * Tool pour créer/écrire des fichiers
 * Usage interactif avec le LLM
 */
@Component
public class WriteFileTool extends AbstractTool {

    private final FileWriterService fileWriterService;
    private final ConsoleReader consoleReader;

    public WriteFileTool(CliService cliService,
                        FileWriterService fileWriterService,
                        ConsoleReader consoleReader) {
        super(cliService);
        this.fileWriterService = fileWriterService;
        this.consoleReader = consoleReader;
    }

    @Override
    public String getName() {
        return "@write";
    }

    @Override
    public String getDescription() {
        return "Crée ou écrase un fichier avec du contenu";
    }

    @Override
    public String getUsage() {
        return "@write <chemin/vers/fichier>\n" +
               "Exemple: @write src/Main.java\n" +
               "Le contenu sera demandé après via le LLM";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args.isEmpty()) {
            return formatError("Chemin de fichier requis. Usage: " + getUsage());
        }

        try {
            String filePath = args.trim();

            // Ask LLM to generate content
            String prompt = String.format(
                "Génère le contenu complet pour le fichier '%s'. " +
                "Réponds UNIQUEMENT avec le code, sans explications, sans markdown, sans ```.",
                filePath
            );

            System.out.println("🤖 Génération du contenu par le LLM...\n");
            String content = cliService.query(prompt);

            // Write with confirmation
            FileWriterService.WriteResult result = fileWriterService.writeFile(
                filePath, content, true, consoleReader
            );

            return result.getFormattedMessage();

        } catch (Exception e) {
            return formatError("Erreur lors de l'écriture: " + e.getMessage());
        }
    }
}
