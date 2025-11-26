package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.files.BackupManager;
import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool pour annuler la dernière modification de fichier
 */
@Component
public class UndoTool extends AbstractTool {

    private final BackupManager backupManager;

    public UndoTool(CliService cliService, BackupManager backupManager) {
        super(cliService);
        this.backupManager = backupManager;
    }

    @Override
    public String getName() {
        return "@undo";
    }

    @Override
    public String getDescription() {
        return "Annule la dernière modification de fichier";
    }

    @Override
    public String getUsage() {
        return "@undo [--list] [--clear]";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        try {
            args = args.trim();

            // List backups
            if (args.equals("--list") || args.equals("-l")) {
                return listBackups();
            }

            // Clear all backups
            if (args.equals("--clear")) {
                backupManager.clearAllBackups();
                return formatSuccess("Tous les backups ont été effacés");
            }

            // Undo last change
            boolean success = backupManager.undo();

            if (success) {
                return AnsiColors.success("✓ Dernière modification annulée avec succès");
            } else {
                return AnsiColors.warning("⚠ Aucun backup disponible pour annuler");
            }

        } catch (Exception e) {
            return formatError("Erreur lors de l'annulation: " + e.getMessage());
        }
    }

    /**
     * Liste tous les backups disponibles
     */
    private String listBackups() {
        try {
            List<BackupManager.BackupEntry> backups = backupManager.listBackups();

            if (backups.isEmpty()) {
                return AnsiColors.info("Aucun backup disponible");
            }

            StringBuilder output = new StringBuilder();

            output.append(BoxDrawer.drawSeparator(
                String.format("📦 %d BACKUP(S) DISPONIBLE(S)", backups.size()),
                70,
                AnsiColors.CYAN
            ));
            output.append("\n\n");

            int count = 1;
            for (BackupManager.BackupEntry entry : backups) {
                output.append(AnsiColors.colorize(
                    String.format("%2d. ", count),
                    AnsiColors.BRIGHT_BLACK
                ));

                output.append(AnsiColors.highlight(entry.getOriginalPath().getFileName().toString()));
                output.append(" ");
                output.append(AnsiColors.dim("(" + entry.getTimestamp() + ")"));
                output.append("\n");

                count++;
            }

            output.append("\n");
            output.append(AnsiColors.info("💡 Utilisez @undo pour annuler la dernière modification"));

            return output.toString();

        } catch (Exception e) {
            return formatError("Erreur lors du listage des backups: " + e.getMessage());
        }
    }
}
