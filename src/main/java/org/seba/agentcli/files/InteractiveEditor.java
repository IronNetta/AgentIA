package org.seba.agentcli.files;

import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.seba.agentcli.io.ConsoleReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Éditeur interactif multi-ligne - 100% homemade!
 * Permet d'éditer du contenu ligne par ligne avant de l'écrire
 */
@Component
public class InteractiveEditor {

    private final ConsoleReader consoleReader;

    public InteractiveEditor(ConsoleReader consoleReader) {
        this.consoleReader = consoleReader;
    }

    /**
     * Édite du contenu de manière interactive
     */
    public EditResult edit(String initialContent, String fileName) {
        List<String> lines = new ArrayList<>(Arrays.asList(initialContent.split("\n")));

        System.out.println("\n");
        System.out.println(BoxDrawer.drawSeparator(
                "✏️  ÉDITEUR INTERACTIF - " + fileName,
                70,
                AnsiColors.PURPLE
        ));
        System.out.println();

        printHelp();
        System.out.println();

        boolean editing = true;
        while (editing) {
            displayContent(lines);
            System.out.println();

            String command = consoleReader.readLine(
                    AnsiColors.colorize("Commande [h pour aide] : ", AnsiColors.YELLOW)
            ).trim();

            if (command.isEmpty()) continue;

            String[] parts = command.split("\\s+", 2);
            String action = parts[0].toLowerCase();

            switch (action) {
                case "done":
                case "save":
                case "s":
                    return new EditResult(true, String.join("\n", lines));

                case "cancel":
                case "q":
                case "quit":
                    return new EditResult(false, null);

                case "h":
                case "help":
                    printHelp();
                    break;

                case "i":
                case "insert":
                    insertLine(lines, parts.length > 1 ? parts[1] : "");
                    break;

                case "a":
                case "append":
                    appendLine(lines, parts.length > 1 ? parts[1] : "");
                    break;

                case "d":
                case "delete":
                    deleteLine(lines, parts.length > 1 ? parts[1] : "");
                    break;

                case "e":
                case "edit":
                    editLine(lines, parts.length > 1 ? parts[1] : "");
                    break;

                case "r":
                case "replace":
                    replaceLine(lines);
                    break;

                case "clear":
                    lines.clear();
                    System.out.println(AnsiColors.success("✓ Contenu effacé"));
                    break;

                default:
                    System.out.println(AnsiColors.error("Commande inconnue. Tapez 'h' pour l'aide."));
            }

            System.out.println();
        }

        return new EditResult(false, null);
    }

    /**
     * Affiche le contenu avec numérotation
     */
    private void displayContent(List<String> lines) {
        System.out.println(AnsiColors.colorize("╭─ Contenu ", AnsiColors.CYAN) +
                          AnsiColors.colorize("─".repeat(59), AnsiColors.CYAN) +
                          AnsiColors.colorize("╮", AnsiColors.CYAN));

        if (lines.isEmpty()) {
            System.out.println(AnsiColors.dim("│ (vide)"));
        } else {
            for (int i = 0; i < lines.size(); i++) {
                System.out.println(
                        AnsiColors.colorize(String.format("%4d│ ", i + 1), AnsiColors.BRIGHT_BLACK) +
                        truncate(lines.get(i), 60)
                );
            }
        }

        System.out.println(AnsiColors.colorize("╰" + "─".repeat(70) + "╯", AnsiColors.CYAN));
    }

    /**
     * Affiche l'aide
     */
    private void printHelp() {
        System.out.println(AnsiColors.colorize("📚 Commandes disponibles:", AnsiColors.BOLD_WHITE));
        System.out.println();
        System.out.println("  " + AnsiColors.colorize("i <n>", AnsiColors.CYAN) + "         → Insert line at position n");
        System.out.println("  " + AnsiColors.colorize("a", AnsiColors.CYAN) + "            → Append line at end");
        System.out.println("  " + AnsiColors.colorize("e <n>", AnsiColors.CYAN) + "         → Edit line n");
        System.out.println("  " + AnsiColors.colorize("d <n>", AnsiColors.CYAN) + "         → Delete line n");
        System.out.println("  " + AnsiColors.colorize("r", AnsiColors.CYAN) + "            → Replace all content");
        System.out.println("  " + AnsiColors.colorize("clear", AnsiColors.CYAN) + "        → Clear all content");
        System.out.println("  " + AnsiColors.colorize("save/done", AnsiColors.GREEN) + "   → Save and exit");
        System.out.println("  " + AnsiColors.colorize("cancel/quit", AnsiColors.RED) + " → Cancel and exit");
        System.out.println("  " + AnsiColors.colorize("h", AnsiColors.CYAN) + "            → Show this help");
    }

    /**
     * Insère une ligne à une position
     */
    private void insertLine(List<String> lines, String posStr) {
        try {
            int pos = posStr.isEmpty() ? 1 : Integer.parseInt(posStr);

            if (pos < 1 || pos > lines.size() + 1) {
                System.out.println(AnsiColors.error("Position invalide (1-" + (lines.size() + 1) + ")"));
                return;
            }

            String content = consoleReader.readLine("Nouvelle ligne : ");
            lines.add(pos - 1, content);

            System.out.println(AnsiColors.success("✓ Ligne insérée à la position " + pos));

        } catch (NumberFormatException e) {
            System.out.println(AnsiColors.error("Position invalide"));
        }
    }

    /**
     * Ajoute une ligne à la fin
     */
    private void appendLine(List<String> lines, String content) {
        if (content.isEmpty()) {
            content = consoleReader.readLine("Nouvelle ligne : ");
        }

        lines.add(content);
        System.out.println(AnsiColors.success("✓ Ligne ajoutée"));
    }

    /**
     * Supprime une ligne
     */
    private void deleteLine(List<String> lines, String posStr) {
        if (lines.isEmpty()) {
            System.out.println(AnsiColors.warning("Contenu vide"));
            return;
        }

        try {
            int pos = posStr.isEmpty() ? lines.size() : Integer.parseInt(posStr);

            if (pos < 1 || pos > lines.size()) {
                System.out.println(AnsiColors.error("Position invalide (1-" + lines.size() + ")"));
                return;
            }

            String removed = lines.remove(pos - 1);
            System.out.println(AnsiColors.success("✓ Ligne " + pos + " supprimée: ") +
                             AnsiColors.dim(truncate(removed, 40)));

        } catch (NumberFormatException e) {
            System.out.println(AnsiColors.error("Position invalide"));
        }
    }

    /**
     * Édite une ligne spécifique
     */
    private void editLine(List<String> lines, String posStr) {
        if (lines.isEmpty()) {
            System.out.println(AnsiColors.warning("Contenu vide"));
            return;
        }

        try {
            int pos = posStr.isEmpty() ? 1 : Integer.parseInt(posStr);

            if (pos < 1 || pos > lines.size()) {
                System.out.println(AnsiColors.error("Position invalide (1-" + lines.size() + ")"));
                return;
            }

            String current = lines.get(pos - 1);
            System.out.println("Ligne actuelle: " + AnsiColors.dim(current));

            String newContent = consoleReader.readLine("Nouveau contenu : ");
            lines.set(pos - 1, newContent);

            System.out.println(AnsiColors.success("✓ Ligne " + pos + " modifiée"));

        } catch (NumberFormatException e) {
            System.out.println(AnsiColors.error("Position invalide"));
        }
    }

    /**
     * Remplace tout le contenu
     */
    private void replaceLine(List<String> lines) {
        System.out.println(AnsiColors.warning("⚠️  Remplacer tout le contenu"));
        System.out.println("Entrez les lignes (ligne vide pour terminer):");

        lines.clear();

        while (true) {
            String line = consoleReader.readLine(
                    AnsiColors.colorize(String.format("%4d│ ", lines.size() + 1), AnsiColors.BRIGHT_BLACK)
            );

            if (line == null || line.trim().isEmpty()) {
                break;
            }

            lines.add(line);
        }

        System.out.println(AnsiColors.success("✓ Contenu remplacé (" + lines.size() + " lignes)"));
    }

    /**
     * Tronque une chaîne
     */
    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Résultat de l'édition
     */
    public static class EditResult {
        private final boolean saved;
        private final String content;

        public EditResult(boolean saved, String content) {
            this.saved = saved;
            this.content = content;
        }

        public boolean isSaved() {
            return saved;
        }

        public String getContent() {
            return content;
        }
    }
}
