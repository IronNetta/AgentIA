package org.seba.agentcli.files;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse les balises de fichiers dans les réponses du LLM
 * Détecte automatiquement les intentions d'écriture/édition de fichiers
 */
@Component
public class FileOperationParser {

    // Pattern pour <file path="...">contenu</file>
    private static final Pattern FILE_TAG_PATTERN = Pattern.compile(
            "<file\\s+path=\"([^\"]+)\">(.*?)</file>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    // Pattern pour <edit file="..." old="..." new="..."/>
    private static final Pattern EDIT_TAG_PATTERN = Pattern.compile(
            "<edit\\s+file=\"([^\"]+)\"\\s+old=\"([^\"]+)\"\\s+new=\"([^\"]+)\"\\s*/>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    // Pattern pour <delete file="..."/>
    private static final Pattern DELETE_TAG_PATTERN = Pattern.compile(
            "<delete\\s+file=\"([^\"]+)\"\\s*/>",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Parse une réponse LLM et extrait toutes les opérations de fichiers
     */
    public ParseResult parse(String llmResponse) {
        List<FileOperation> operations = new ArrayList<>();

        // Détecter les <file> tags
        Matcher fileMatcher = FILE_TAG_PATTERN.matcher(llmResponse);
        while (fileMatcher.find()) {
            String path = fileMatcher.group(1);
            String content = fileMatcher.group(2).trim();

            // Nettoyer le contenu (enlever les balises markdown si présentes)
            content = cleanContent(content);

            operations.add(new FileOperation(
                    FileOperationType.WRITE,
                    path,
                    content,
                    null,
                    null
            ));
        }

        // Détecter les <edit> tags
        Matcher editMatcher = EDIT_TAG_PATTERN.matcher(llmResponse);
        while (editMatcher.find()) {
            String path = editMatcher.group(1);
            String oldText = editMatcher.group(2);
            String newText = editMatcher.group(3);

            operations.add(new FileOperation(
                    FileOperationType.EDIT,
                    path,
                    null,
                    oldText,
                    newText
            ));
        }

        // Détecter les <delete> tags
        Matcher deleteMatcher = DELETE_TAG_PATTERN.matcher(llmResponse);
        while (deleteMatcher.find()) {
            String path = deleteMatcher.group(1);

            operations.add(new FileOperation(
                    FileOperationType.DELETE,
                    path,
                    null,
                    null,
                    null
            ));
        }

        // Nettoyer la réponse des balises
        String cleanedResponse = removeAllTags(llmResponse);

        return new ParseResult(operations, cleanedResponse);
    }

    /**
     * Nettoie le contenu (enlève les balises markdown si présentes)
     */
    private String cleanContent(String content) {
        // Enlever les ``` au début et à la fin si présents
        content = content.replaceAll("^```[a-zA-Z]*\\n?", "");
        content = content.replaceAll("\\n?```$", "");

        return content.trim();
    }

    /**
     * Enlève toutes les balises de la réponse
     */
    private String removeAllTags(String response) {
        String cleaned = response;

        // Enlever les <file> tags
        cleaned = FILE_TAG_PATTERN.matcher(cleaned).replaceAll("");

        // Enlever les <edit> tags
        cleaned = EDIT_TAG_PATTERN.matcher(cleaned).replaceAll("");

        // Enlever les <delete> tags
        cleaned = DELETE_TAG_PATTERN.matcher(cleaned).replaceAll("");

        return cleaned.trim();
    }

    /**
     * Vérifie si une réponse contient des opérations de fichiers
     */
    public boolean containsFileOperations(String llmResponse) {
        return FILE_TAG_PATTERN.matcher(llmResponse).find() ||
               EDIT_TAG_PATTERN.matcher(llmResponse).find() ||
               DELETE_TAG_PATTERN.matcher(llmResponse).find();
    }

    /**
     * Type d'opération sur fichier
     */
    public enum FileOperationType {
        WRITE,   // Créer ou écraser un fichier
        EDIT,    // Éditer une partie d'un fichier
        DELETE   // Supprimer un fichier
    }

    /**
     * Représente une opération sur un fichier
     */
    public static class FileOperation {
        private final FileOperationType type;
        private final String path;
        private final String content;      // Pour WRITE
        private final String oldText;      // Pour EDIT
        private final String newText;      // Pour EDIT

        public FileOperation(FileOperationType type, String path, String content,
                           String oldText, String newText) {
            this.type = type;
            this.path = path;
            this.content = content;
            this.oldText = oldText;
            this.newText = newText;
        }

        public FileOperationType getType() {
            return type;
        }

        public String getPath() {
            return path;
        }

        public String getContent() {
            return content;
        }

        public String getOldText() {
            return oldText;
        }

        public String getNewText() {
            return newText;
        }

        @Override
        public String toString() {
            return switch (type) {
                case WRITE -> "Write to " + path;
                case EDIT -> "Edit " + path;
                case DELETE -> "Delete " + path;
            };
        }
    }

    /**
     * Résultat du parsing
     */
    public static class ParseResult {
        private final List<FileOperation> operations;
        private final String cleanedResponse;

        public ParseResult(List<FileOperation> operations, String cleanedResponse) {
            this.operations = operations;
            this.cleanedResponse = cleanedResponse;
        }

        public List<FileOperation> getOperations() {
            return operations;
        }

        public String getCleanedResponse() {
            return cleanedResponse;
        }

        public boolean hasOperations() {
            return !operations.isEmpty();
        }
    }
}
