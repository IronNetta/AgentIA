package org.seba.agentcli.security;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Audit logger for tracking all file operations
 * Provides a complete audit trail for compliance and debugging
 */
@Component
public class AuditLogger {

    private static final String AUDIT_FILE = ".agentcli/audit.log";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public enum Operation {
        FILE_READ,
        FILE_WRITE,
        FILE_EDIT,
        FILE_DELETE,
        DIRECTORY_CREATE,
        DIRECTORY_LIST,
        GIT_OPERATION,
        SEARCH_OPERATION
    }

    /**
     * Logs a file operation to the audit trail
     *
     * @param operation The type of operation
     * @param path The file path involved
     * @param success Whether the operation succeeded
     * @param details Additional details
     */
    public void logOperation(Operation operation, String path, boolean success, String details) {
        try {
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            String status = success ? "SUCCESS" : "FAILURE";
            String username = System.getProperty("user.name");

            String logEntry = String.format(
                "[%s] [%s] [%s] User: %s | Operation: %s | Path: %s | Details: %s%n",
                timestamp,
                status,
                operation.name(),
                username,
                operation.name(),
                path != null ? path : "N/A",
                details != null ? details : "N/A"
            );

            // Ensure log directory exists
            Path logPath = Paths.get(AUDIT_FILE);
            if (logPath.getParent() != null) {
                Files.createDirectories(logPath.getParent());
            }

            // Append to audit file
            Files.writeString(
                logPath,
                logEntry,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            // Fail silently - don't interrupt operations due to logging failures
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }

    /**
     * Logs a successful file read
     */
    public void logFileRead(String path, long bytesRead) {
        logOperation(
            Operation.FILE_READ,
            path,
            true,
            "Bytes read: " + bytesRead
        );
    }

    /**
     * Logs a file write operation
     */
    public void logFileWrite(String path, long bytesWritten, boolean created) {
        logOperation(
            Operation.FILE_WRITE,
            path,
            true,
            String.format("Bytes written: %d | Created: %s", bytesWritten, created)
        );
    }

    /**
     * Logs a file edit operation
     */
    public void logFileEdit(String path, String editType, int linesChanged) {
        logOperation(
            Operation.FILE_EDIT,
            path,
            true,
            String.format("Type: %s | Lines changed: %d", editType, linesChanged)
        );
    }

    /**
     * Logs a failed operation
     */
    public void logFailure(Operation operation, String path, String reason) {
        logOperation(operation, path, false, "Reason: " + reason);
    }

    /**
     * Logs a git operation
     */
    public void logGitOperation(String command, boolean success) {
        logOperation(
            Operation.GIT_OPERATION,
            null,
            success,
            "Command: " + command
        );
    }

    /**
     * Logs a search operation
     */
    public void logSearch(String pattern, int resultsFound) {
        logOperation(
            Operation.SEARCH_OPERATION,
            null,
            true,
            String.format("Pattern: %s | Results: %d", pattern, resultsFound)
        );
    }

    /**
     * Gets the audit log file path
     */
    public Path getAuditFilePath() {
        return Paths.get(AUDIT_FILE).toAbsolutePath();
    }

    /**
     * Reads recent audit entries
     */
    public String getRecentAuditEntries(int count) throws IOException {
        Path logPath = Paths.get(AUDIT_FILE);
        if (!Files.exists(logPath)) {
            return "No audit entries found.";
        }

        var lines = Files.readAllLines(logPath);
        int start = Math.max(0, lines.size() - count);
        return String.join("\n", lines.subList(start, lines.size()));
    }

    /**
     * Searches audit log for specific operation
     */
    public String searchAuditLog(String searchTerm) throws IOException {
        Path logPath = Paths.get(AUDIT_FILE);
        if (!Files.exists(logPath)) {
            return "No audit entries found.";
        }

        return Files.lines(logPath)
            .filter(line -> line.contains(searchTerm))
            .reduce((a, b) -> a + "\n" + b)
            .orElse("No matching audit entries found.");
    }

    /**
     * Clears the audit log (use with caution!)
     */
    public void clearAuditLog() throws IOException {
        Path logPath = Paths.get(AUDIT_FILE);
        if (Files.exists(logPath)) {
            Files.delete(logPath);
        }
    }
}
