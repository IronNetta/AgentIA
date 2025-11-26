package org.seba.agentcli.files;

import org.seba.agentcli.model.ProjectType;
import org.springframework.stereotype.Component;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates code syntax before writing to files
 * Helps prevent syntax errors and invalid code
 */
@Component
public class CodeValidator {

    /**
     * Validates code syntax based on file extension
     */
    public ValidationResult validate(String filePath, String content, ProjectType projectType) {
        String extension = getFileExtension(filePath);

        return switch (extension) {
            case "java" -> validateJava(content);
            case "py" -> validatePython(content);
            case "js", "ts", "jsx", "tsx" -> validateJavaScript(content);
            case "json" -> validateJSON(content);
            case "xml" -> validateXML(content);
            case "yaml", "yml" -> validateYAML(content);
            default -> ValidationResult.success(); // No validation for unknown types
        };
    }

    /**
     * Validates that old text exists in file before editing
     */
    public ValidationResult validateEditOperation(String filePath, String fileContent, String oldText) {
        if (!fileContent.contains(oldText)) {
            // Try to find similar text
            List<String> suggestions = findSimilarLines(fileContent, oldText, 3);

            if (!suggestions.isEmpty()) {
                return ValidationResult.error(
                        "Old text not found in file.\n" +
                        "Did you mean one of these?\n" +
                        String.join("\n", suggestions)
                );
            }

            return ValidationResult.error(
                    "Old text not found in file. Please use @read to view current file content."
            );
        }

        return ValidationResult.success();
    }

    /**
     * Validates Java code syntax
     */
    private ValidationResult validateJava(String code) {
        try {
            // Basic syntax checks
            if (!hasBalancedBraces(code)) {
                return ValidationResult.error("Unbalanced braces {} in Java code");
            }

            if (!hasBalancedParentheses(code)) {
                return ValidationResult.error("Unbalanced parentheses () in Java code");
            }

            if (!hasBalancedBrackets(code)) {
                return ValidationResult.error("Unbalanced brackets [] in Java code");
            }

            // Check for common syntax errors
            if (code.contains("public class") && !code.trim().endsWith("}")) {
                return ValidationResult.warning("Java class may be incomplete (missing closing brace)");
            }

            // Try to compile if possible (advanced)
            return tryCompileJava(code);

        } catch (Exception e) {
            return ValidationResult.error("Validation error: " + e.getMessage());
        }
    }

    /**
     * Try to compile Java code using Java Compiler API
     */
    private ValidationResult tryCompileJava(String code) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                // Compiler not available, skip compilation check
                return ValidationResult.success();
            }

            // Create temporary file
            Path tempFile = Files.createTempFile("CodeValidation", ".java");
            Files.writeString(tempFile, code);

            // Try to compile
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(tempFile.toFile());

            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);

            boolean success = task.call();

            // Clean up
            Files.deleteIfExists(tempFile);
            fileManager.close();

            if (!success) {
                StringBuilder errors = new StringBuilder("Java compilation warnings:\n");
                boolean hasNonDependencyErrors = false;

                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                        String message = diagnostic.getMessage(null);
                        errors.append(String.format("Line %d: %s\n",
                                diagnostic.getLineNumber(),
                                message));

                        // Check if it's not a dependency/import error
                        if (!message.contains("package") &&
                            !message.contains("cannot find symbol") &&
                            !message.contains("does not exist")) {
                            hasNonDependencyErrors = true;
                        }
                    }
                }

                // Only block if there are real syntax errors, not dependency errors
                if (hasNonDependencyErrors) {
                    return ValidationResult.error(errors.toString());
                } else {
                    return ValidationResult.warning(errors.toString());
                }
            }

            return ValidationResult.success();

        } catch (Exception e) {
            // Compilation check failed, but don't block the operation
            return ValidationResult.warning("Could not verify Java syntax: " + e.getMessage());
        }
    }

    /**
     * Validates Python code syntax (basic checks)
     */
    private ValidationResult validatePython(String code) {
        // Check indentation consistency
        if (hasMixedIndentation(code)) {
            return ValidationResult.error("Mixed tabs and spaces in Python code");
        }

        // Check for balanced parentheses/brackets/braces
        if (!hasBalancedParentheses(code)) {
            return ValidationResult.error("Unbalanced parentheses in Python code");
        }

        if (!hasBalancedBrackets(code)) {
            return ValidationResult.error("Unbalanced brackets in Python code");
        }

        if (!hasBalancedBraces(code)) {
            return ValidationResult.error("Unbalanced braces in Python code");
        }

        // Check for common syntax errors
        if (code.contains("def ") && !code.contains(":")) {
            return ValidationResult.error("Python function definition missing colon");
        }

        return ValidationResult.success();
    }

    /**
     * Validates JavaScript/TypeScript code (basic checks)
     */
    private ValidationResult validateJavaScript(String code) {
        if (!hasBalancedBraces(code)) {
            return ValidationResult.error("Unbalanced braces in JavaScript code");
        }

        if (!hasBalancedParentheses(code)) {
            return ValidationResult.error("Unbalanced parentheses in JavaScript code");
        }

        if (!hasBalancedBrackets(code)) {
            return ValidationResult.error("Unbalanced brackets in JavaScript code");
        }

        return ValidationResult.success();
    }

    /**
     * Validates JSON syntax
     */
    private ValidationResult validateJSON(String content) {
        try {
            // Try to parse as JSON
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(content);
            return ValidationResult.success();
        } catch (Exception e) {
            return ValidationResult.error("Invalid JSON: " + e.getMessage());
        }
    }

    /**
     * Validates XML syntax (basic checks)
     */
    private ValidationResult validateXML(String content) {
        if (!content.trim().startsWith("<")) {
            return ValidationResult.error("XML must start with <");
        }

        // Check for balanced tags (basic)
        long openCount = content.chars().filter(ch -> ch == '<').count();
        long closeCount = content.chars().filter(ch -> ch == '>').count();

        if (openCount != closeCount) {
            return ValidationResult.error("Unbalanced XML tags");
        }

        return ValidationResult.success();
    }

    /**
     * Validates YAML syntax (basic checks)
     */
    private ValidationResult validateYAML(String content) {
        try {
            // Basic YAML validation
            new com.fasterxml.jackson.dataformat.yaml.YAMLMapper().readTree(content);
            return ValidationResult.success();
        } catch (Exception e) {
            return ValidationResult.error("Invalid YAML: " + e.getMessage());
        }
    }

    // Helper methods

    private boolean hasBalancedBraces(String code) {
        return isBalanced(code, '{', '}');
    }

    private boolean hasBalancedParentheses(String code) {
        return isBalanced(code, '(', ')');
    }

    private boolean hasBalancedBrackets(String code) {
        return isBalanced(code, '[', ']');
    }

    private boolean isBalanced(String code, char open, char close) {
        int count = 0;
        boolean inString = false;
        char stringChar = '\0';

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            // Handle strings
            if (c == '"' || c == '\'') {
                if (!inString) {
                    inString = true;
                    stringChar = c;
                } else if (c == stringChar && (i == 0 || code.charAt(i - 1) != '\\')) {
                    inString = false;
                }
                continue;
            }

            if (inString) continue;

            if (c == open) count++;
            if (c == close) count--;
            if (count < 0) return false;
        }

        return count == 0;
    }

    private boolean hasMixedIndentation(String code) {
        boolean hasTabs = code.contains("\t");
        boolean hasSpaces = Pattern.compile("^ +", Pattern.MULTILINE).matcher(code).find();
        return hasTabs && hasSpaces;
    }

    private List<String> findSimilarLines(String content, String target, int maxResults) {
        List<String> suggestions = new ArrayList<>();
        String[] lines = content.split("\n");
        String targetLower = target.toLowerCase().trim();

        for (String line : lines) {
            String lineLower = line.toLowerCase().trim();
            if (lineLower.contains(targetLower) || targetLower.contains(lineLower)) {
                suggestions.add(line.trim());
                if (suggestions.size() >= maxResults) break;
            }
        }

        return suggestions;
    }

    private String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filePath.length() - 1) {
            return filePath.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Result of code validation
     */
    public static class ValidationResult {
        private final boolean success;
        private final String message;
        private final ValidationLevel level;

        private ValidationResult(boolean success, String message, ValidationLevel level) {
            this.success = success;
            this.message = message;
            this.level = level;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, ValidationLevel.INFO);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message, ValidationLevel.ERROR);
        }

        public static ValidationResult warning(String message) {
            return new ValidationResult(true, message, ValidationLevel.WARNING);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public ValidationLevel getLevel() {
            return level;
        }

        public boolean hasMessage() {
            return message != null && !message.isEmpty();
        }
    }

    public enum ValidationLevel {
        INFO,
        WARNING,
        ERROR
    }
}
