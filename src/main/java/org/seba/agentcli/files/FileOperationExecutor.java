package org.seba.agentcli.files;

import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.seba.agentcli.io.ConsoleReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Exécute les opérations de fichiers détectées par le parser
 * Avec confirmation interactive de l'utilisateur
 */
@Component
public class FileOperationExecutor {

    private final FileWriterService fileWriterService;
    private final FileEditorService fileEditorService;
    private final ConsoleReader consoleReader;

    public FileOperationExecutor(FileWriterService fileWriterService,
                                FileEditorService fileEditorService,
                                ConsoleReader consoleReader) {
        this.fileWriterService = fileWriterService;
        this.fileEditorService = fileEditorService;
        this.consoleReader = consoleReader;
    }

    /**
     * Exécute une liste d'opérations avec confirmation
     */
    public ExecutionResult execute(List<FileOperationParser.FileOperation> operations) {
        if (operations.isEmpty()) {
            return new ExecutionResult(0, 0, new ArrayList<>());
        }

        System.out.println("\n");
        System.out.println(BoxDrawer.drawSeparator(
                String.format("🤖 %d OPÉRATION(S) DE FICHIER DÉTECTÉE(S)", operations.size()),
                70,
                AnsiColors.PURPLE
        ));
        System.out.println();

        List<String> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;

        for (int i = 0; i < operations.size(); i++) {
            FileOperationParser.FileOperation op = operations.get(i);

            System.out.println(AnsiColors.colorize(
                    String.format("[%d/%d] ", i + 1, operations.size()),
                    AnsiColors.BRIGHT_BLACK
            ) + op);
            System.out.println();

            try {
                boolean success = executeOperation(op);

                if (success) {
                    successful++;
                    results.add(AnsiColors.success("✓ " + op.getPath()));
                } else {
                    failed++;
                    results.add(AnsiColors.warning("⊘ " + op.getPath() + " (annulé)"));
                }

            } catch (Exception e) {
                failed++;
                results.add(AnsiColors.error("✗ " + op.getPath() + " (" + e.getMessage() + ")"));
                System.out.println(AnsiColors.error("Erreur: " + e.getMessage()));
            }

            System.out.println();
        }

        // Résumé
        System.out.println(BoxDrawer.drawSeparator("RÉSUMÉ", 70, AnsiColors.CYAN));
        System.out.println();

        if (successful > 0) {
            System.out.println(AnsiColors.success(String.format("✓ %d réussie(s)", successful)));
        }
        if (failed > 0) {
            System.out.println(AnsiColors.warning(String.format("⊘ %d annulée(s)/échouée(s)", failed)));
        }

        System.out.println();

        return new ExecutionResult(successful, failed, results);
    }

    /**
     * Exécute une opération unique
     */
    private boolean executeOperation(FileOperationParser.FileOperation op) throws Exception {
        return switch (op.getType()) {
            case WRITE -> executeWrite(op);
            case EDIT -> executeEdit(op);
            case DELETE -> executeDelete(op);
        };
    }

    /**
     * Exécute une opération WRITE
     */
    private boolean executeWrite(FileOperationParser.FileOperation op) throws Exception {
        FileWriterService.WriteResult result = fileWriterService.writeFile(
                op.getPath(),
                op.getContent(),
                true,  // avec confirmation
                consoleReader
        );

        if (result.isSuccess()) {
            System.out.println(result.getFormattedMessage());
        }

        return result.isSuccess();
    }

    /**
     * Exécute une opération EDIT
     */
    private boolean executeEdit(FileOperationParser.FileOperation op) throws Exception {
        FileEditorService.EditResult result = fileEditorService.replaceString(
                op.getPath(),
                op.getOldText(),
                op.getNewText(),
                false,  // première occurrence seulement
                true,   // avec confirmation
                consoleReader
        );

        if (result.isSuccess()) {
            System.out.println(result.getFormattedMessage());
        }

        return result.isSuccess();
    }

    /**
     * Exécute une opération DELETE
     */
    private boolean executeDelete(FileOperationParser.FileOperation op) throws Exception {
        FileWriterService.WriteResult result = fileWriterService.deleteFile(
                op.getPath(),
                true,  // avec confirmation
                consoleReader
        );

        if (result.isSuccess()) {
            System.out.println(result.getFormattedMessage());
        }

        return result.isSuccess();
    }

    /**
     * Résultat de l'exécution
     */
    public static class ExecutionResult {
        private final int successful;
        private final int failed;
        private final List<String> details;

        public ExecutionResult(int successful, int failed, List<String> details) {
            this.successful = successful;
            this.failed = failed;
            this.details = details;
        }

        public int getSuccessful() {
            return successful;
        }

        public int getFailed() {
            return failed;
        }

        public List<String> getDetails() {
            return details;
        }

        public boolean hasSuccessful() {
            return successful > 0;
        }
    }
}
