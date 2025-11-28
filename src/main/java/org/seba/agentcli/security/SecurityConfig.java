package org.seba.agentcli.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Security configuration loaded from security-config.yml
 */
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityConfig {

    private PathValidation pathValidation = new PathValidation();
    private RateLimit rateLimit = new RateLimit();
    private Audit audit = new Audit();
    private SecurityLogging securityLogging = new SecurityLogging();
    private Monitoring monitoring = new Monitoring();
    private FileLimits fileLimits = new FileLimits();
    private Backup backup = new Backup();

    // Getters and setters

    public PathValidation getPathValidation() {
        return pathValidation;
    }

    public void setPathValidation(PathValidation pathValidation) {
        this.pathValidation = pathValidation;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Audit getAudit() {
        return audit;
    }

    public void setAudit(Audit audit) {
        this.audit = audit;
    }

    public SecurityLogging getSecurityLogging() {
        return securityLogging;
    }

    public void setSecurityLogging(SecurityLogging securityLogging) {
        this.securityLogging = securityLogging;
    }

    public Monitoring getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(Monitoring monitoring) {
        this.monitoring = monitoring;
    }

    public FileLimits getFileLimits() {
        return fileLimits;
    }

    public void setFileLimits(FileLimits fileLimits) {
        this.fileLimits = fileLimits;
    }

    public Backup getBackup() {
        return backup;
    }

    public void setBackup(Backup backup) {
        this.backup = backup;
    }

    // Nested classes for configuration sections

    public static class PathValidation {
        private boolean enabled = true;
        private List<String> forbiddenPaths = new ArrayList<>();
        private List<String> suspiciousPatterns = new ArrayList<>();
        private boolean requireProjectScope = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getForbiddenPaths() {
            return forbiddenPaths;
        }

        public void setForbiddenPaths(List<String> forbiddenPaths) {
            this.forbiddenPaths = forbiddenPaths;
        }

        public List<String> getSuspiciousPatterns() {
            return suspiciousPatterns;
        }

        public void setSuspiciousPatterns(List<String> suspiciousPatterns) {
            this.suspiciousPatterns = suspiciousPatterns;
        }

        public boolean isRequireProjectScope() {
            return requireProjectScope;
        }

        public void setRequireProjectScope(boolean requireProjectScope) {
            this.requireProjectScope = requireProjectScope;
        }
    }

    public static class RateLimit {
        private boolean enabled = true;
        private int maxOperationsPerSecond = 10;
        private int maxOperationsPerMinute = 100;
        private int maxGitOperationsPerMinute = 50;
        private int maxSearchOperationsPerMinute = 30;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxOperationsPerSecond() {
            return maxOperationsPerSecond;
        }

        public void setMaxOperationsPerSecond(int maxOperationsPerSecond) {
            this.maxOperationsPerSecond = maxOperationsPerSecond;
        }

        public int getMaxOperationsPerMinute() {
            return maxOperationsPerMinute;
        }

        public void setMaxOperationsPerMinute(int maxOperationsPerMinute) {
            this.maxOperationsPerMinute = maxOperationsPerMinute;
        }

        public int getMaxGitOperationsPerMinute() {
            return maxGitOperationsPerMinute;
        }

        public void setMaxGitOperationsPerMinute(int maxGitOperationsPerMinute) {
            this.maxGitOperationsPerMinute = maxGitOperationsPerMinute;
        }

        public int getMaxSearchOperationsPerMinute() {
            return maxSearchOperationsPerMinute;
        }

        public void setMaxSearchOperationsPerMinute(int maxSearchOperationsPerMinute) {
            this.maxSearchOperationsPerMinute = maxSearchOperationsPerMinute;
        }
    }

    public static class Audit {
        private boolean enabled = true;
        private String logFile = ".agentcli/audit.log";
        private boolean logFileOperations = true;
        private boolean logGitOperations = true;
        private boolean logSearchOperations = true;
        private int maxLogSizeMB = 10;
        private boolean rotateLogsOnMaxSize = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLogFile() {
            return logFile;
        }

        public void setLogFile(String logFile) {
            this.logFile = logFile;
        }

        public boolean isLogFileOperations() {
            return logFileOperations;
        }

        public void setLogFileOperations(boolean logFileOperations) {
            this.logFileOperations = logFileOperations;
        }

        public boolean isLogGitOperations() {
            return logGitOperations;
        }

        public void setLogGitOperations(boolean logGitOperations) {
            this.logGitOperations = logGitOperations;
        }

        public boolean isLogSearchOperations() {
            return logSearchOperations;
        }

        public void setLogSearchOperations(boolean logSearchOperations) {
            this.logSearchOperations = logSearchOperations;
        }

        public int getMaxLogSizeMB() {
            return maxLogSizeMB;
        }

        public void setMaxLogSizeMB(int maxLogSizeMB) {
            this.maxLogSizeMB = maxLogSizeMB;
        }

        public boolean isRotateLogsOnMaxSize() {
            return rotateLogsOnMaxSize;
        }

        public void setRotateLogsOnMaxSize(boolean rotateLogsOnMaxSize) {
            this.rotateLogsOnMaxSize = rotateLogsOnMaxSize;
        }
    }

    public static class SecurityLogging {
        private boolean enabled = true;
        private String logFile = ".agentcli/security.log";
        private boolean logToConsole = true;
        private String logLevel = "WARN";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLogFile() {
            return logFile;
        }

        public void setLogFile(String logFile) {
            this.logFile = logFile;
        }

        public boolean isLogToConsole() {
            return logToConsole;
        }

        public void setLogToConsole(boolean logToConsole) {
            this.logToConsole = logToConsole;
        }

        public String getLogLevel() {
            return logLevel;
        }

        public void setLogLevel(String logLevel) {
            this.logLevel = logLevel;
        }
    }

    public static class Monitoring {
        private boolean enabled = true;
        private int rateLimitViolationThreshold = 10;
        private int pathTraversalThreshold = 5;
        private int forbiddenAccessThreshold = 3;
        private boolean alertToConsole = true;
        private int alertCooldownMinutes = 15;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRateLimitViolationThreshold() {
            return rateLimitViolationThreshold;
        }

        public void setRateLimitViolationThreshold(int rateLimitViolationThreshold) {
            this.rateLimitViolationThreshold = rateLimitViolationThreshold;
        }

        public int getPathTraversalThreshold() {
            return pathTraversalThreshold;
        }

        public void setPathTraversalThreshold(int pathTraversalThreshold) {
            this.pathTraversalThreshold = pathTraversalThreshold;
        }

        public int getForbiddenAccessThreshold() {
            return forbiddenAccessThreshold;
        }

        public void setForbiddenAccessThreshold(int forbiddenAccessThreshold) {
            this.forbiddenAccessThreshold = forbiddenAccessThreshold;
        }

        public boolean isAlertToConsole() {
            return alertToConsole;
        }

        public void setAlertToConsole(boolean alertToConsole) {
            this.alertToConsole = alertToConsole;
        }

        public int getAlertCooldownMinutes() {
            return alertCooldownMinutes;
        }

        public void setAlertCooldownMinutes(int alertCooldownMinutes) {
            this.alertCooldownMinutes = alertCooldownMinutes;
        }
    }

    public static class FileLimits {
        private int maxReadSizeMB = 10;
        private int maxWriteSizeMB = 10;
        private int warnSizeMB = 5;

        public int getMaxReadSizeMB() {
            return maxReadSizeMB;
        }

        public void setMaxReadSizeMB(int maxReadSizeMB) {
            this.maxReadSizeMB = maxReadSizeMB;
        }

        public int getMaxWriteSizeMB() {
            return maxWriteSizeMB;
        }

        public void setMaxWriteSizeMB(int maxWriteSizeMB) {
            this.maxWriteSizeMB = maxWriteSizeMB;
        }

        public int getWarnSizeMB() {
            return warnSizeMB;
        }

        public void setWarnSizeMB(int warnSizeMB) {
            this.warnSizeMB = warnSizeMB;
        }
    }

    public static class Backup {
        private boolean enabled = true;
        private String directory = ".agentcli/backups";
        private int maxBackupsPerFile = 5;
        private boolean autoCleanup = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }

        public int getMaxBackupsPerFile() {
            return maxBackupsPerFile;
        }

        public void setMaxBackupsPerFile(int maxBackupsPerFile) {
            this.maxBackupsPerFile = maxBackupsPerFile;
        }

        public boolean isAutoCleanup() {
            return autoCleanup;
        }

        public void setAutoCleanup(boolean autoCleanup) {
            this.autoCleanup = autoCleanup;
        }
    }
}
