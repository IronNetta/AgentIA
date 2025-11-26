package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.files.FileEditorService;
import org.seba.agentcli.files.FileReaderService;
import org.seba.agentcli.io.ConsoleReader;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

/**
 * Tool pour éditer des parties spécifiques de fichiers
 * Comme l'outil Edit de Claude Code
 */
@Component
public class EditFileTool extends AbstractTool {

    private final FileEditorService fileEditorService;
    private final FileReaderService fileReaderService;
    private final ConsoleReader consoleReader;

    public EditFileTool(CliService cliService,
                       FileEditorService fileEditorService,
                       FileReaderService fileReaderService,
                       ConsoleReader consoleReader) {
        super(cliService);
        this.fileEditorService = fileEditorService;
        this.fileReaderService = fileReaderService;
        this.consoleReader = consoleReader;
    }

    @Override
    public String getName() {
        return "@edit";
    }

    @Override
    public String getDescription() {
        return "Édite une partie spécifique d'un fichier (recherche/remplacement)";
    }

    @Override
    public String getUsage() {
        return "@edit <fichier> --old \"ancien texte\" --new \"nouveau texte\" [--all]\n" +
               "Ou mode interactif: @edit <fichier>";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args.isEmpty()) {
            return formatError("Arguments requis. Usage: " + getUsage());
        }

        try {
            String[] parts = args.split("\\s+", 2);
            String filePath = parts[0];

            // Vérifier si le fichier existe
            if (!fileReaderService.fileExists(filePath)) {
                return formatError("Fichier inexistant: " + filePath);
            }

            // Si mode interactif (pas de --old/--new)
            if (parts.length == 1 || (!args.contains("--old") && !args.contains("--new"))) {
                return executeInteractiveMode(filePath);
            }

            // Mode direct avec --old et --new
            String oldText = extractQuotedValue(args, "--old");
            String newText = extractQuotedValue(args, "--new");
            boolean replaceAll = args.contains("--all");

            if (oldText == null || newText == null) {
                return formatError("--old et --new requis. Usage: " + getUsage());
            }

            FileEditorService.EditResult result = fileEditorService.replaceString(
                filePath, oldText, newText, replaceAll, true, consoleReader
            );

            return result.getFormattedMessage();

        } catch (Exception e) {
            return formatError("Erreur lors de l'édition: " + e.getMessage());
        }
    }

    /**
     * Mode interactif: demande au LLM de suggérer les modifications
     */
    private String executeInteractiveMode(String filePath) {
        try {
            // Lire le fichier
            FileReaderService.FileContent content = fileReaderService.readFile(filePath);

            // Afficher le contenu
            System.out.println(fileReaderService.displayFile(content));
            System.out.println();

            // Demander à l'utilisateur ce qu'il veut modifier
            String modification = consoleReader.readLine("Que voulez-vous modifier ? : ");

            // Demander au LLM de suggérer old_string et new_string
            String prompt = String.format(
                "Fichier: %s\n\nContenu actuel:\n%s\n\n" +
                "L'utilisateur veut: %s\n\n" +
                "Réponds au format EXACT suivant (sans rien d'autre):\n" +
                "OLD: [le texte exact à remplacer]\n" +
                "NEW: [le nouveau texte]",
                filePath, content.getContent(), modification
            );

            String llmResponse = cliService.query(prompt);

            // Parser la réponse
            String oldText = extractLineValue(llmResponse, "OLD:");
            String newText = extractLineValue(llmResponse, "NEW:");

            if (oldText == null || newText == null) {
                return formatError("Impossible de parser la réponse du LLM");
            }

            // Appliquer la modification
            FileEditorService.EditResult result = fileEditorService.replaceString(
                filePath, oldText, newText, false, true, consoleReader
            );

            return result.getFormattedMessage();

        } catch (Exception e) {
            return formatError("Erreur en mode interactif: " + e.getMessage());
        }
    }

    /**
     * Extrait une valeur entre guillemets après une clé
     */
    private String extractQuotedValue(String text, String key) {
        int keyIndex = text.indexOf(key);
        if (keyIndex == -1) return null;

        int startQuote = text.indexOf("\"", keyIndex);
        if (startQuote == -1) return null;

        int endQuote = text.indexOf("\"", startQuote + 1);
        if (endQuote == -1) return null;

        return text.substring(startQuote + 1, endQuote);
    }

    /**
     * Extrait une valeur après une ligne commençant par une clé
     */
    private String extractLineValue(String text, String prefix) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith(prefix)) {
                String value = line.substring(line.indexOf(prefix) + prefix.length()).trim();
                // Enlever les guillemets si présents
                if (value.startsWith("[") && value.endsWith("]")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return null;
    }
}
