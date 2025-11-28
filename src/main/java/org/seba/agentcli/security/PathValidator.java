package org.seba.agentcli.security;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Security utility for validating file paths to prevent path traversal attacks
 * and unauthorized access to sensitive files.
 */
@Component
public class PathValidator {

    private final SecurityLogger securityLogger;

    public PathValidator(SecurityLogger securityLogger) {
        this.securityLogger = securityLogger;
    }

    /**
     * Directories and files that should never be accessed
     */
    private static final List<String> FORBIDDEN_PATHS = Arrays.asList(
        ".git",
        ".env",
        ".ssh",
        "id_rsa",
        "id_dsa",
        "credentials",
        "secrets"
    );

    /**
     * Validates that a file path is safe to access.
     *
     * Security checks:
     * 1. Path must be within project directory (prevent path traversal)
     * 2. Path must not access sensitive directories (.git, .env, etc.)
     * 3. Path must not contain dangerous patterns
     *
     * @param filePath The file path to validate (can be relative or absolute)
     * @return ValidationResult with status and error message
     */
    public ValidationResult validatePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return ValidationResult.invalid("File path cannot be empty");
        }

        try {
            Path path = Paths.get(filePath);
            return validatePath(path);
        } catch (Exception e) {
            return ValidationResult.invalid("Invalid file path: " + e.getMessage());
        }
    }

    /**
     * Validates a Path object.
     *
     * @param path The Path to validate
     * @return ValidationResult with status and error message
     */
    public ValidationResult validatePath(Path path) {
        try {
            // Normalize the path to resolve '..' and '.'
            Path projectRoot = Paths.get(".").toAbsolutePath().normalize();
            Path normalizedPath = path.toAbsolutePath().normalize();

            // Check 1: Path must be within project directory
            if (!normalizedPath.startsWith(projectRoot)) {
                securityLogger.logPathTraversal(
                    path.toString(),
                    "Attempted to access path outside project directory: " + normalizedPath
                );
                return ValidationResult.invalid(
                    "Access denied: file must be within project directory"
                );
            }

            // Check 2: Path must not access forbidden directories/files
            String pathString = normalizedPath.toString();
            for (String forbidden : FORBIDDEN_PATHS) {
                if (pathString.contains("/" + forbidden + "/") ||
                    pathString.contains("/" + forbidden) ||
                    pathString.endsWith("/" + forbidden) ||
                    normalizedPath.getFileName().toString().equals(forbidden)) {
                    securityLogger.logForbiddenAccess(
                        path.toString(),
                        forbidden
                    );
                    return ValidationResult.invalid(
                        "Access denied: cannot access sensitive path '" + forbidden + "'"
                    );
                }
            }

            // Check 3: Detect suspicious patterns
            if (pathString.contains("..")) {
                // This should already be handled by normalize(), but double-check
                securityLogger.logSuspiciousPattern(
                    path.toString(),
                    "Path contains '..' after normalization"
                );
                return ValidationResult.invalid(
                    "Access denied: path contains suspicious patterns"
                );
            }

            return ValidationResult.valid();

        } catch (Exception e) {
            return ValidationResult.invalid("Path validation failed: " + e.getMessage());
        }
    }

    /**
     * Validates and normalizes a path, throwing exception if invalid.
     *
     * @param filePath The file path to validate
     * @return The normalized Path object
     * @throws SecurityException if path is invalid
     */
    public Path validateAndNormalize(String filePath) throws SecurityException {
        ValidationResult result = validatePath(filePath);
        if (!result.isValid()) {
            throw new SecurityException(result.getErrorMessage());
        }
        return Paths.get(filePath).toAbsolutePath().normalize();
    }

    /**
     * Result of path validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
