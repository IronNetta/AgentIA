package org.seba.agentcli.files;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestionnaire de backups automatiques - 100% homemade!
 * Sauvegarde les fichiers avant modification avec historique et undo
 */
@Service
public class BackupManager {

    private static final String BACKUP_DIR = ".agentcli/backups";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int MAX_BACKUPS_PER_FILE = 10;

    private final Deque<BackupEntry> backupHistory = new ArrayDeque<>();

    /**
     * Crée un backup d'un fichier avant modification
     */
    public BackupEntry createBackup(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("Fichier inexistant: " + filePath);
        }

        // Créer le répertoire de backup si nécessaire
        Path backupRoot = Paths.get(BACKUP_DIR);
        Files.createDirectories(backupRoot);

        // Générer le nom du backup avec timestamp
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String fileName = filePath.getFileName().toString();
        String backupFileName = fileName + "." + timestamp + ".backup";

        Path backupPath = backupRoot.resolve(backupFileName);

        // Copier le fichier
        Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);

        // Créer l'entrée de backup
        BackupEntry entry = new BackupEntry(filePath, backupPath, timestamp);
        backupHistory.addFirst(entry);

        // Nettoyer les vieux backups
        cleanOldBackups(fileName);

        return entry;
    }

    /**
     * Restaure le dernier backup (undo)
     */
    public boolean undo() throws IOException {
        if (backupHistory.isEmpty()) {
            return false;
        }

        BackupEntry entry = backupHistory.removeFirst();

        // Restaurer le fichier
        if (Files.exists(entry.getBackupPath())) {
            Files.copy(entry.getBackupPath(), entry.getOriginalPath(),
                      StandardCopyOption.REPLACE_EXISTING);
            return true;
        }

        return false;
    }

    /**
     * Restaure un backup spécifique
     */
    public boolean restore(BackupEntry entry) throws IOException {
        if (!Files.exists(entry.getBackupPath())) {
            return false;
        }

        Files.copy(entry.getBackupPath(), entry.getOriginalPath(),
                  StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    /**
     * Liste tous les backups disponibles
     */
    public List<BackupEntry> listBackups() throws IOException {
        Path backupRoot = Paths.get(BACKUP_DIR);

        if (!Files.exists(backupRoot)) {
            return Collections.emptyList();
        }

        return Files.list(backupRoot)
                .filter(p -> p.toString().endsWith(".backup"))
                .map(this::parseBackupEntry)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(BackupEntry::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Liste les backups pour un fichier spécifique
     */
    public List<BackupEntry> listBackupsForFile(String fileName) throws IOException {
        return listBackups().stream()
                .filter(e -> e.getOriginalPath().getFileName().toString().equals(fileName))
                .collect(Collectors.toList());
    }

    /**
     * Nettoie les vieux backups (garde seulement les N derniers)
     */
    private void cleanOldBackups(String fileName) throws IOException {
        List<BackupEntry> backups = listBackupsForFile(fileName);

        if (backups.size() > MAX_BACKUPS_PER_FILE) {
            // Supprimer les plus vieux
            backups.stream()
                    .skip(MAX_BACKUPS_PER_FILE)
                    .forEach(entry -> {
                        try {
                            Files.deleteIfExists(entry.getBackupPath());
                        } catch (IOException e) {
                            // Ignorer les erreurs de suppression
                        }
                    });
        }
    }

    /**
     * Parse une entrée de backup depuis le fichier
     */
    private BackupEntry parseBackupEntry(Path backupPath) {
        try {
            String backupName = backupPath.getFileName().toString();
            // Format: filename.yyyyMMdd_HHmmss.backup

            int lastDot = backupName.lastIndexOf(".backup");
            if (lastDot == -1) return null;

            String withoutBackup = backupName.substring(0, lastDot);
            int secondLastDot = withoutBackup.lastIndexOf('.');
            if (secondLastDot == -1) return null;

            String timestamp = withoutBackup.substring(secondLastDot + 1);
            String originalName = withoutBackup.substring(0, secondLastDot);

            // Reconstruire le path original (on suppose qu'il est dans le répertoire courant)
            Path originalPath = Paths.get(originalName);

            return new BackupEntry(originalPath, backupPath, timestamp);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Efface tous les backups
     */
    public void clearAllBackups() throws IOException {
        Path backupRoot = Paths.get(BACKUP_DIR);

        if (Files.exists(backupRoot)) {
            Files.walk(backupRoot)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignorer
                        }
                    });
        }

        backupHistory.clear();
    }

    /**
     * Retourne l'historique des backups
     */
    public List<BackupEntry> getHistory() {
        return new ArrayList<>(backupHistory);
    }

    /**
     * Classe représentant une entrée de backup
     */
    public static class BackupEntry {
        private final Path originalPath;
        private final Path backupPath;
        private final String timestamp;

        public BackupEntry(Path originalPath, Path backupPath, String timestamp) {
            this.originalPath = originalPath;
            this.backupPath = backupPath;
            this.timestamp = timestamp;
        }

        public Path getOriginalPath() {
            return originalPath;
        }

        public Path getBackupPath() {
            return backupPath;
        }

        public String getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("%s -> %s (%s)",
                    originalPath.getFileName(),
                    backupPath.getFileName(),
                    timestamp);
        }
    }
}
