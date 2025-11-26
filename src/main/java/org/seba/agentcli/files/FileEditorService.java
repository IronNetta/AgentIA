package org.seba.agentcli.files;

import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.seba.agentcli.io.ConsoleReader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service d'édition de fichiers - 100% homemade!
 * Édition précise avec recherche/remplacement, comme l'outil Edit de Claude Code
 */
@Service
public class FileEditorService {

    private final BackupManager backupManager;
    private final FileReaderService fileReaderService;
    private final DiffViewer diffViewer;
    private final CodeValidator codeValidator;

    public FileEditorService(BackupManager backupManager,
                            FileReaderService fileReaderService,
                            DiffViewer diffViewer,
                            CodeValidator codeValidator) {
        this.backupManager = backupManager;
        this.fileReaderService = fileReaderService;
        this.diffViewer = diffViewer;
        this.codeValidator = codeValidator;
    }

    /**
     * Remplace une chaîne par une autre dans un fichier
     */
    public EditResult replaceString(String filePath, String oldString, String newString,
                                   boolean replaceAll, boolean withConfirmation,
                                   ConsoleReader reader) throws IOException {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();

        if (!Files.exists(path)) {
            throw new IOException("File not found: " + path);
        }

        // Lire le contenu
        String content = Files.readString(path);

        // Validate that oldString exists in file
        CodeValidator.ValidationResult validation = codeValidator.validateEditOperation(filePath, content, oldString);
        if (!validation.isSuccess()) {
            return new EditResult(path, false, validation.getMessage(), 0);
        }

        // Vérifier que oldString existe
        if (!content.contains(oldString)) {
            return new EditResult(path, false, "String not found: " + oldString, 0);
        }

        // Compter les occurrences
        int occurrences = countOccurrences(content, oldString);

        // Si pas replaceAll et plus d'une occurrence, erreur
        if (!replaceAll && occurrences > 1) {
            return new EditResult(path, false,
                    String.format("La chaîne apparaît %d fois. Utilisez replaceAll=true ou spécifiez un contexte plus large.",
                            occurrences), 0);
        }

        // Préparer le nouveau contenu
        String newContent = replaceAll ?
                content.replace(oldString, newString) :
                content.replaceFirst(Pattern.quote(oldString), Matcher.quoteReplacement(newString));

        // Preview avec diff
        if (withConfirmation) {
            displayEditPreview(path, content, newContent, oldString, newString, occurrences, replaceAll);

            String response = reader.readLine(
                    AnsiColors.colorize("Confirmer la modification ? [O/n] : ", AnsiColors.YELLOW)
            ).trim().toLowerCase();

            if (response.equals("n") || response.equals("non") || response.equals("no")) {
                return new EditResult(path, false, "Annulé par l'utilisateur", 0);
            }
        }

        // Backup
        BackupManager.BackupEntry backup = backupManager.createBackup(path);

        // Écrire
        Files.writeString(path, newContent);

        int replacements = replaceAll ? occurrences : 1;
        return new EditResult(path, true,
                String.format("Modifié avec succès (%d remplacement(s)) - Backup: %s",
                        replacements, backup.getBackupPath().getFileName()),
                replacements);
    }

    /**
     * Remplace du texte sur une ligne spécifique
     */
    public EditResult replaceOnLine(String filePath, int lineNumber, String oldString, String newString,
                                   boolean withConfirmation, ConsoleReader reader) throws IOException {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();

        if (!Files.exists(path)) {
            throw new IOException("Fichier inexistant: " + path);
        }

        // Lire les lignes
        List<String> lines = Files.readAllLines(path);

        if (lineNumber < 1 || lineNumber > lines.size()) {
            throw new IOException(String.format("Numéro de ligne invalide: %d (fichier: %d lignes)",
                    lineNumber, lines.size()));
        }

        String oldLine = lines.get(lineNumber - 1);

        if (!oldLine.contains(oldString)) {
            return new EditResult(path, false,
                    String.format("Chaîne introuvable à la ligne %d", lineNumber), 0);
        }

        String newLine = oldLine.replace(oldString, newString);

        // Preview
        if (withConfirmation) {
            displayLineEditPreview(path, lineNumber, oldLine, newLine);

            String response = reader.readLine(
                    AnsiColors.colorize("Confirmer la modification ? [O/n] : ", AnsiColors.YELLOW)
            ).trim().toLowerCase();

            if (response.equals("n") || response.equals("non") || response.equals("no")) {
                return new EditResult(path, false, "Annulé par l'utilisateur", 0);
            }
        }

        // Backup
        BackupManager.BackupEntry backup = backupManager.createBackup(path);

        // Modifier
        lines.set(lineNumber - 1, newLine);
        Files.write(path, lines);

        return new EditResult(path, true,
                String.format("Ligne %d modifiée - Backup: %s", lineNumber, backup.getBackupPath().getFileName()),
                1);
    }

    /**
     * Insère du texte à une ligne spécifique
     */
    public EditResult insertAtLine(String filePath, int lineNumber, String content,
                                  boolean withConfirmation, ConsoleReader reader) throws IOException {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();

        if (!Files.exists(path)) {
            throw new IOException("Fichier inexistant: " + path);
        }

        List<String> lines = Files.readAllLines(path);

        if (lineNumber < 1 || lineNumber > lines.size() + 1) {
            throw new IOException(String.format("Numéro de ligne invalide: %d", lineNumber));
        }

        // Preview
        if (withConfirmation) {
            displayInsertPreview(path, lineNumber, content, lines);

            String response = reader.readLine(
                    AnsiColors.colorize("Confirmer l'insertion ? [O/n] : ", AnsiColors.YELLOW)
            ).trim().toLowerCase();

            if (response.equals("n") || response.equals("non") || response.equals("no")) {
                return new EditResult(path, false, "Annulé par l'utilisateur", 0);
            }
        }

        // Backup
        BackupManager.BackupEntry backup = backupManager.createBackup(path);

        // Insérer
        lines.add(lineNumber - 1, content);
        Files.write(path, lines);

        return new EditResult(path, true,
                String.format("Ligne insérée à la position %d - Backup: %s",
                        lineNumber, backup.getBackupPath().getFileName()),
                1);
    }

    /**
     * Supprime des lignes
     */
    public EditResult deleteLines(String filePath, int startLine, int endLine,
                                 boolean withConfirmation, ConsoleReader reader) throws IOException {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();

        if (!Files.exists(path)) {
            throw new IOException("Fichier inexistant: " + path);
        }

        List<String> lines = Files.readAllLines(path);

        if (startLine < 1 || endLine > lines.size() || startLine > endLine) {
            throw new IOException("Plage de lignes invalide");
        }

        // Preview
        if (withConfirmation) {
            displayDeletePreview(path, startLine, endLine, lines);

            String response = reader.readLine(
                    AnsiColors.colorize("Confirmer la suppression ? [o/N] : ", AnsiColors.RED)
            ).trim().toLowerCase();

            if (!response.equals("o") && !response.equals("oui") && !response.equals("yes")) {
                return new EditResult(path, false, "Annulé par l'utilisateur", 0);
            }
        }

        // Backup
        BackupManager.BackupEntry backup = backupManager.createBackup(path);

        // Supprimer
        List<String> newLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (i + 1 < startLine || i + 1 > endLine) {
                newLines.add(lines.get(i));
            }
        }

        Files.write(path, newLines);

        int deleted = endLine - startLine + 1;
        return new EditResult(path, true,
                String.format("%d ligne(s) supprimée(s) - Backup: %s", deleted, backup.getBackupPath().getFileName()),
                deleted);
    }

    /**
     * Compte les occurrences d'une chaîne
     */
    private int countOccurrences(String content, String search) {
        int count = 0;
        int index = 0;

        while ((index = content.indexOf(search, index)) != -1) {
            count++;
            index += search.length();
        }

        return count;
    }

    /**
     * Affiche le preview d'une édition
     */
    private void displayEditPreview(Path path, String oldContent, String newContent,
                                   String oldString, String newString,
                                   int occurrences, boolean replaceAll) throws IOException {
        System.out.println("\n");
        System.out.println(BoxDrawer.drawSeparator("✏️  MODIFICATION DE FICHIER", 70, AnsiColors.CYAN));
        System.out.println();

        System.out.println("Fichier: " + AnsiColors.info(path.toString()));
        System.out.println("Occurrences: " + AnsiColors.highlight(String.valueOf(occurrences)));
        System.out.println("Mode: " + (replaceAll ? AnsiColors.warning("Remplacer tout") : AnsiColors.info("Première occurrence")));
        System.out.println();

        // Afficher le diff
        String diff = diffViewer.generateDiff(oldContent, newContent);
        System.out.println(diff);
    }

    /**
     * Affiche le preview d'une édition de ligne
     */
    private void displayLineEditPreview(Path path, int lineNumber, String oldLine, String newLine) {
        System.out.println("\n");
        System.out.println(BoxDrawer.drawSeparator("✏️  MODIFICATION DE LIGNE", 70, AnsiColors.CYAN));
        System.out.println();

        System.out.println("Fichier: " + AnsiColors.info(path.toString()));
        System.out.println("Ligne: " + AnsiColors.highlight(String.valueOf(lineNumber)));
        System.out.println();

        System.out.println(AnsiColors.colorize("- ", AnsiColors.RED) + AnsiColors.dim(oldLine));
        System.out.println(AnsiColors.colorize("+ ", AnsiColors.GREEN) + AnsiColors.highlight(newLine));
        System.out.println();
    }

    /**
     * Affiche le preview d'une insertion
     */
    private void displayInsertPreview(Path path, int lineNumber, String content, List<String> lines) {
        System.out.println("\n");
        System.out.println(BoxDrawer.drawSeparator("➕ INSERTION DE LIGNE", 70, AnsiColors.CYAN));
        System.out.println();

        System.out.println("Fichier: " + AnsiColors.info(path.toString()));
        System.out.println("Position: " + AnsiColors.highlight(String.valueOf(lineNumber)));
        System.out.println();

        // Contexte (3 lignes avant/après)
        int start = Math.max(0, lineNumber - 3);
        int end = Math.min(lines.size(), lineNumber + 2);

        for (int i = start; i < lineNumber - 1; i++) {
            System.out.println(AnsiColors.dim(String.format("%4d  %s", i + 1, lines.get(i))));
        }

        System.out.println(AnsiColors.colorize(String.format("%4d+ %s", lineNumber, content), AnsiColors.GREEN));

        for (int i = lineNumber - 1; i < end; i++) {
            System.out.println(AnsiColors.dim(String.format("%4d  %s", i + 2, lines.get(i))));
        }

        System.out.println();
    }

    /**
     * Affiche le preview d'une suppression
     */
    private void displayDeletePreview(Path path, int startLine, int endLine, List<String> lines) {
        System.out.println("\n");
        System.out.println(BoxDrawer.drawSeparator("❌ SUPPRESSION DE LIGNES", 70, AnsiColors.RED));
        System.out.println();

        System.out.println("Fichier: " + AnsiColors.info(path.toString()));
        System.out.println("Lignes: " + AnsiColors.error(String.format("%d à %d", startLine, endLine)));
        System.out.println();

        // Contexte
        int contextStart = Math.max(0, startLine - 3);
        int contextEnd = Math.min(lines.size(), endLine + 2);

        for (int i = contextStart; i < startLine - 1; i++) {
            System.out.println(AnsiColors.dim(String.format("%4d  %s", i + 1, lines.get(i))));
        }

        for (int i = startLine - 1; i < endLine; i++) {
            System.out.println(AnsiColors.colorize(String.format("%4d- %s", i + 1, lines.get(i)), AnsiColors.RED));
        }

        for (int i = endLine; i < contextEnd; i++) {
            System.out.println(AnsiColors.dim(String.format("%4d  %s", i + 1, lines.get(i))));
        }

        System.out.println();
    }

    /**
     * Classe représentant le résultat d'une édition
     */
    public static class EditResult {
        private final Path path;
        private final boolean success;
        private final String message;
        private final int changes;

        public EditResult(Path path, boolean success, String message, int changes) {
            this.path = path;
            this.success = success;
            this.message = message;
            this.changes = changes;
        }

        public Path getPath() {
            return path;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getChanges() {
            return changes;
        }

        public String getFormattedMessage() {
            if (success) {
                return AnsiColors.success("✓ " + message);
            } else {
                return AnsiColors.warning("⚠ " + message);
            }
        }
    }
}
