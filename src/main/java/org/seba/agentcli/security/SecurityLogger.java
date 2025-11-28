package org.seba.agentcli.security;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Security event logger for tracking access violations and suspicious activities
 */
@Component
public class SecurityLogger {

    private static final String LOG_FILE = ".agentcli/security.log";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SecurityMonitor securityMonitor;

    // Use lazy injection to avoid circular dependency
    public void setSecurityMonitor(@Lazy SecurityMonitor securityMonitor) {
        this.securityMonitor = securityMonitor;
    }

    public enum SecurityEvent {
        PATH_TRAVERSAL_BLOCKED,
        FORBIDDEN_PATH_ACCESS,
        COMMAND_INJECTION_ATTEMPT,
        RATE_LIMIT_EXCEEDED,
        INVALID_FILE_ACCESS,
        SUSPICIOUS_PATTERN_DETECTED
    }

    /**
     * Logs a security event
     *
     * @param event The type of security event
     * @param path The path that triggered the event (can be null)
     * @param details Additional details about the event
     */
    public void logSecurityEvent(SecurityEvent event, String path, String details) {
        try {
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            String logEntry = String.format("[%s] [%s] Path: %s | Details: %s%n",
                timestamp,
                event.name(),
                path != null ? path : "N/A",
                details != null ? details : "N/A"
            );

            // Ensure log directory exists
            Path logPath = Paths.get(LOG_FILE);
            if (logPath.getParent() != null) {
                Files.createDirectories(logPath.getParent());
            }

            // Append to log file
            Files.writeString(
                logPath,
                logEntry,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );

            // Also log to console for immediate visibility
            System.err.println("⚠️  SECURITY: " + event.name() + " - " + (path != null ? path : details));

            // Notify security monitor
            if (securityMonitor != null) {
                securityMonitor.recordEvent(event);
            }

        } catch (IOException e) {
            // Fail silently - don't interrupt operations due to logging failures
            System.err.println("Failed to write security log: " + e.getMessage());
        }
    }

    /**
     * Logs a path traversal attempt
     */
    public void logPathTraversal(String attemptedPath, String reason) {
        logSecurityEvent(
            SecurityEvent.PATH_TRAVERSAL_BLOCKED,
            attemptedPath,
            "Reason: " + reason
        );
    }

    /**
     * Logs access to a forbidden path
     */
    public void logForbiddenAccess(String path, String forbiddenElement) {
        logSecurityEvent(
            SecurityEvent.FORBIDDEN_PATH_ACCESS,
            path,
            "Forbidden element: " + forbiddenElement
        );
    }

    /**
     * Logs a potential command injection attempt
     */
    public void logCommandInjection(String command, String suspiciousPattern) {
        logSecurityEvent(
            SecurityEvent.COMMAND_INJECTION_ATTEMPT,
            null,
            "Command: " + command + " | Pattern: " + suspiciousPattern
        );
    }

    /**
     * Logs rate limit exceeded event
     */
    public void logRateLimitExceeded(String operation, int count, int limit) {
        logSecurityEvent(
            SecurityEvent.RATE_LIMIT_EXCEEDED,
            null,
            String.format("Operation: %s | Count: %d | Limit: %d", operation, count, limit)
        );
    }

    /**
     * Logs suspicious file access patterns
     */
    public void logSuspiciousPattern(String path, String pattern) {
        logSecurityEvent(
            SecurityEvent.SUSPICIOUS_PATTERN_DETECTED,
            path,
            "Pattern: " + pattern
        );
    }

    /**
     * Gets the security log file path
     */
    public Path getLogFilePath() {
        return Paths.get(LOG_FILE).toAbsolutePath();
    }

    /**
     * Clears the security log (for testing or maintenance)
     */
    public void clearLog() throws IOException {
        Path logPath = Paths.get(LOG_FILE);
        if (Files.exists(logPath)) {
            Files.delete(logPath);
        }
    }

    /**
     * Reads recent security events
     */
    public String getRecentEvents(int count) throws IOException {
        Path logPath = Paths.get(LOG_FILE);
        if (!Files.exists(logPath)) {
            return "No security events logged.";
        }

        var lines = Files.readAllLines(logPath);
        int start = Math.max(0, lines.size() - count);
        return String.join("\n", lines.subList(start, lines.size()));
    }
}
