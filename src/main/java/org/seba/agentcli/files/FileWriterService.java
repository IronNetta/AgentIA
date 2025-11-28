package org.seba.agentcli.files;

import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.seba.agentcli.io.ConsoleReader;
import org.seba.agentcli.security.PathValidator;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

/**
 * Service d'écriture de fichiers - 100% homemade!
 * Création et modification de fichiers avec backup automatique et confirmation
 */
@Service
public class FileWriterService {

    private final BackupManager backupManager;
    private final FileReaderService fileReaderService;
    private final InteractiveEditor interactiveEditor;
    private final CodeValidator codeValidator;
    private final TestRunner testRunner;
    private final PathValidator pathValidator;

    public FileWriterService(BackupManager backupManager,
                           FileReaderService fileReaderService,
                           InteractiveEditor interactiveEditor,
                           CodeValidator codeValidator,
                           TestRunner testRunner,
                           PathValidator pathValidator) {
        this.backupManager = backupManager;
        this.fileReaderService = fileReaderService;
        this.interactiveEditor = interactiveEditor;
        this.codeValidator = codeValidator;
        this.testRunner = testRunner;
        this.pathValidator = pathValidator;
    }

    /**
     * Écrit du contenu dans un fichier (crée ou écrase)
     */
    public WriteResult writeFile(String filePath, String content, boolean withConfirmation, ConsoleReader reader) throws IOException {
        // Security: Validate path before writing
        PathValidator.ValidationResult pathValidation = pathValidator.validatePath(filePath);
        if (!pathValidation.isValid()) {
            throw new SecurityException(pathValidation.getErrorMessage());
        }

        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        boolean fileExists = Files.exists(path);
        String fileName = path.getFileName().toString();

        // Validate code syntax before writing
        CodeValidator.ValidationResult validation = codeValidator.validate(filePath, content, null);
        if (!validation.isSuccess()) {
            System.out.println(AnsiColors.colorize("⚠ Validation error: " + validation.getMessage(), AnsiColors.RED));
            System.out.println(AnsiColors.colorize("File will still be created (validation can be fixed later)", AnsiColors.YELLOW));
            System.out.println();
            // Don't block file creation for validation errors - just warn
            // return new WriteResult(Paths.get(filePath), false, "Code validation failed: " + validation.getMessage());
        }

        if (validation.hasMessage() && validation.getLevel() == CodeValidator.ValidationLevel.WARNING) {
            System.out.println(AnsiColors.colorize("⚠ Warning: " + validation.getMessage(), AnsiColors.YELLOW));
            System.out.println();
        }

        // Créer les répertoires parents si nécessaire
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        // Preview du contenu
        if (withConfirmation) {
            displayWritePreview(path, content, fileExists);

            String response = reader.readLine(
                    AnsiColors.colorize("Confirmer l'écriture ? [O/n/e(dit)] : ", AnsiColors.YELLOW)
            ).trim().toLowerCase();

            if (response.equals("n") || response.equals("non") || response.equals("no")) {
                return new WriteResult(path, false, "Annulé par l'utilisateur");
            }

            if (response.equals("e") || response.equals("edit")) {
                // Ouvrir l'éditeur interactif
                InteractiveEditor.EditResult editResult = interactiveEditor.edit(content, fileName);

                if (!editResult.isSaved()) {
                    return new WriteResult(path, false, "Édition annulée");
                }

                // Utiliser le contenu édité
                content = editResult.getContent();
                System.out.println(AnsiColors.success("\n✓ Modifications appliquées\n"));
            }
        }

        // Backup si le fichier existe
        BackupManager.BackupEntry backup = null;
        if (fileExists) {
            backup = backupManager.createBackup(path);
        }

        // Écrire le fichier
        Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        String message = fileExists ? "Fichier modifié avec succès" : "Fichier créé avec succès";
        if (backup != null) {
            message += " (backup: " + backup.getBackupPath().getFileName() + ")";
        }

        return new WriteResult(path, true, message);
    }

    /**
     * Ajoute du contenu à la fin d'un fichier
     */
    public WriteResult appendToFile(String filePath, String content, boolean withConfirmation, ConsoleReader reader) throws IOException {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();

        if (!Files.exists(path)) {
            throw new IOException("Fichier inexistant: " + path);
        }

        // Preview
        if (withConfirmation) {
            System.out.println(BoxDrawer.drawSeparator("📝 AJOUT DE CONTENU", 70, AnsiColors.CYAN));
            System.out.println("\nFichier: " + AnsiColors.info(path.toString()));
            System.out.println("\nContenu à ajouter:");
            System.out.println(AnsiColors.colorize("─".repeat(70), AnsiColors.BRIGHT_BLACK));
            System.out.println(content);
            System.out.println(AnsiColors.colorize("─".repeat(70), AnsiColors.BRIGHT_BLACK));

            String response = reader.readLine(
                    AnsiColors.colorize("Confirmer l'ajout ? [O/n] : ", AnsiColors.YELLOW)
            ).trim().toLowerCase();

            if (response.equals("n") || response.equals("non") || response.equals("no")) {
                return new WriteResult(path, false, "Annulé par l'utilisateur");
            }
        }

        // Backup
        BackupManager.BackupEntry backup = backupManager.createBackup(path);

        // Ajouter le contenu
        Files.writeString(path, content, StandardOpenOption.APPEND);

        return new WriteResult(path, true, "Contenu ajouté (backup: " + backup.getBackupPath().getFileName() + ")");
    }

    /**
     * Crée un nouveau fichier (échoue si existe déjà)
     */
    public WriteResult createFile(String filePath, String content) throws IOException {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();

        if (Files.exists(path)) {
            throw new IOException("Le fichier existe déjà: " + path);
        }

        // Créer les répertoires parents
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        // Écrire le fichier
        Files.writeString(path, content, StandardOpenOption.CREATE_NEW);

        return new WriteResult(path, true, "Fichier créé avec succès");
    }

    /**
     * Supprime un fichier avec confirmation
     */
    public WriteResult deleteFile(String filePath, boolean withConfirmation, ConsoleReader reader) throws IOException {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();

        if (!Files.exists(path)) {
            throw new IOException("Fichier inexistant: " + path);
        }

        // Confirmation
        if (withConfirmation) {
            System.out.println(BoxDrawer.drawWarningBox(
                    "Supprimer le fichier: " + path.getFileName(),
                    60
            ));

            String response = reader.readLine(
                    AnsiColors.colorize("Confirmer la suppression ? [o/N] : ", AnsiColors.RED)
            ).trim().toLowerCase();

            if (!response.equals("o") && !response.equals("oui") && !response.equals("yes")) {
                return new WriteResult(path, false, "Annulé par l'utilisateur");
            }
        }

        // Backup avant suppression
        BackupManager.BackupEntry backup = backupManager.createBackup(path);

        // Supprimer
        Files.delete(path);

        return new WriteResult(path, true, "Fichier supprimé (backup: " + backup.getBackupPath().getFileName() + ")");
    }

    /**
     * Affiche un preview du contenu à écrire
     */
    private void displayWritePreview(Path path, String content, boolean fileExists) throws IOException {
        String fileName = path.getFileName().toString();

        System.out.println("\n");
        System.out.println(BoxDrawer.drawSeparator(
                fileExists ? "📝 MODIFICATION DE FICHIER" : "📄 CRÉATION DE FICHIER",
                70,
                AnsiColors.CYAN
        ));
        System.out.println();

        // Infos du fichier
        List<String[]> info = Arrays.asList(
                new String[]{"Path", path.toString()},
                new String[]{"Status", fileExists ? AnsiColors.warning("Existe déjà") : AnsiColors.success("Nouveau")},
                new String[]{"Lignes", String.valueOf(content.split("\n").length)},
                new String[]{"Taille", String.format("%d octets", content.length())}
        );

        System.out.println(BoxDrawer.drawInfoPanel("ℹ️ INFORMATIONS", info, 66));
        System.out.println();

        // Preview du contenu (premières lignes)
        System.out.println(AnsiColors.colorize("📄 Preview du contenu:", AnsiColors.BOLD_WHITE));
        System.out.println(AnsiColors.colorize("─".repeat(70), AnsiColors.BRIGHT_BLACK));

        String[] lines = content.split("\n");
        int maxPreview = Math.min(20, lines.length);

        for (int i = 0; i < maxPreview; i++) {
            String lineNum = AnsiColors.colorize(
                    String.format("%4d│ ", i + 1),
                    AnsiColors.BRIGHT_BLACK
            );
            System.out.println(lineNum + lines[i]);
        }

        if (lines.length > maxPreview) {
            System.out.println(AnsiColors.colorize(
                    String.format("... (%d lignes supplémentaires)", lines.length - maxPreview),
                    AnsiColors.BRIGHT_BLACK
            ));
        }

        System.out.println(AnsiColors.colorize("─".repeat(70), AnsiColors.BRIGHT_BLACK));
        System.out.println();

        // Warning si écrasement
        if (fileExists) {
            System.out.println(BoxDrawer.drawWarningBox(
                    "⚠️  Le fichier existant sera écrasé (backup automatique)",
                    66
            ));
            System.out.println();
        }
    }

    /**
     * Classe représentant le résultat d'une opération d'écriture
     */
    public static class WriteResult {
        private final Path path;
        private final boolean success;
        private final String message;

        public WriteResult(Path path, boolean success, String message) {
            this.path = path;
            this.success = success;
            this.message = message;
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

        public String getFormattedMessage() {
            if (success) {
                return AnsiColors.success("✓ " + message);
            } else {
                return AnsiColors.warning("⚠ " + message);
            }
        }
    }
}
