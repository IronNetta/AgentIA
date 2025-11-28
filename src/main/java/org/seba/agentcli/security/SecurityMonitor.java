package org.seba.agentcli.security;

import org.seba.agentcli.io.AnsiColors;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Security monitoring and alerting system
 * Tracks security events and triggers alerts when thresholds are exceeded
 */
@Component
public class SecurityMonitor {

    private final SecurityConfig securityConfig;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Event counters per hour
    private final Map<String, EventCounter> eventCounters = new ConcurrentHashMap<>();

    // Alert cooldown tracking
    private final Map<String, Long> lastAlertTime = new ConcurrentHashMap<>();

    public SecurityMonitor(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    /**
     * Records a security event and checks if alert should be triggered
     */
    public void recordEvent(SecurityLogger.SecurityEvent eventType) {
        if (!securityConfig.getMonitoring().isEnabled()) {
            return;
        }

        String key = eventType.name();
        EventCounter counter = eventCounters.computeIfAbsent(key, k -> new EventCounter());

        long now = Instant.now().toEpochMilli();
        counter.increment(now);

        // Check if we should alert
        checkAndAlert(eventType, counter.getCountLastHour(now));
    }

    /**
     * Checks if an alert should be triggered based on thresholds
     */
    private void checkAndAlert(SecurityLogger.SecurityEvent eventType, int count) {
        SecurityConfig.Monitoring config = securityConfig.getMonitoring();
        int threshold = getThreshold(eventType, config);

        if (count >= threshold) {
            String alertKey = eventType.name();

            // Check cooldown
            Long lastAlert = lastAlertTime.get(alertKey);
            long now = Instant.now().toEpochMilli();
            long cooldownMillis = config.getAlertCooldownMinutes() * 60 * 1000L;

            if (lastAlert == null || (now - lastAlert) > cooldownMillis) {
                triggerAlert(eventType, count, threshold);
                lastAlertTime.put(alertKey, now);
            }
        }
    }

    /**
     * Gets the threshold for a specific event type
     */
    private int getThreshold(SecurityLogger.SecurityEvent eventType, SecurityConfig.Monitoring config) {
        return switch (eventType) {
            case RATE_LIMIT_EXCEEDED -> config.getRateLimitViolationThreshold();
            case PATH_TRAVERSAL_BLOCKED -> config.getPathTraversalThreshold();
            case FORBIDDEN_PATH_ACCESS -> config.getForbiddenAccessThreshold();
            default -> Integer.MAX_VALUE; // No threshold by default
        };
    }

    /**
     * Triggers a security alert
     */
    private void triggerAlert(SecurityLogger.SecurityEvent eventType, int count, int threshold) {
        String time = LocalDateTime.now().format(TIME_FORMAT);
        String alertMessage = String.format(
            """

            ╔═══════════════════════════════════════════════════════════╗
            ║              🚨 SECURITY ALERT 🚨                        ║
            ╠═══════════════════════════════════════════════════════════╣
            ║  Event Type: %-43s ║
            ║  Count (1h):  %-43d ║
            ║  Threshold:   %-43d ║
            ║  Time:        %-43s ║
            ╠═══════════════════════════════════════════════════════════╣
            ║  Action: Review security logs for details               ║
            ║  Log File: .agentcli/security.log                        ║
            ╚═══════════════════════════════════════════════════════════╝
            """,
            eventType.name(),
            count,
            threshold,
            time
        );

        if (securityConfig.getMonitoring().isAlertToConsole()) {
            System.err.println(AnsiColors.colorize(alertMessage, AnsiColors.RED));
        }
    }

    /**
     * Gets current event count for an event type
     */
    public int getEventCount(SecurityLogger.SecurityEvent eventType) {
        EventCounter counter = eventCounters.get(eventType.name());
        if (counter == null) {
            return 0;
        }
        return counter.getCountLastHour(Instant.now().toEpochMilli());
    }

    /**
     * Resets event counters (for testing)
     */
    public void reset() {
        eventCounters.clear();
        lastAlertTime.clear();
    }

    /**
     * Gets a summary of all security events in the last hour
     */
    public String getSecuritySummary() {
        long now = Instant.now().toEpochMilli();
        StringBuilder summary = new StringBuilder();

        summary.append("\n╔═══════════════════════════════════════════════════════╗\n");
        summary.append("║         SECURITY MONITORING SUMMARY (Last Hour)      ║\n");
        summary.append("╠═══════════════════════════════════════════════════════╣\n");

        for (SecurityLogger.SecurityEvent eventType : SecurityLogger.SecurityEvent.values()) {
            EventCounter counter = eventCounters.get(eventType.name());
            int count = counter != null ? counter.getCountLastHour(now) : 0;

            if (count > 0) {
                summary.append(String.format("║  %-35s : %5d     ║\n",
                    eventType.name(), count));
            }
        }

        if (eventCounters.isEmpty()) {
            summary.append("║  No security events recorded                         ║\n");
        }

        summary.append("╚═══════════════════════════════════════════════════════╝\n");

        return summary.toString();
    }

    /**
     * Counter for tracking events in a time window
     */
    private static class EventCounter {
        private static final int ONE_HOUR_MS = 60 * 60 * 1000;
        private final Map<Long, AtomicInteger> countsPerMinute = new ConcurrentHashMap<>();

        public void increment(long timestamp) {
            long minuteBucket = timestamp / (60 * 1000);
            countsPerMinute.computeIfAbsent(minuteBucket, k -> new AtomicInteger(0))
                .incrementAndGet();

            // Clean old entries
            cleanOldEntries(timestamp);
        }

        public int getCountLastHour(long now) {
            long oneHourAgo = now - ONE_HOUR_MS;
            long cutoffBucket = oneHourAgo / (60 * 1000);

            return countsPerMinute.entrySet().stream()
                .filter(entry -> entry.getKey() >= cutoffBucket)
                .mapToInt(entry -> entry.getValue().get())
                .sum();
        }

        private void cleanOldEntries(long now) {
            long oneHourAgo = now - ONE_HOUR_MS;
            long cutoffBucket = oneHourAgo / (60 * 1000);

            countsPerMinute.entrySet()
                .removeIf(entry -> entry.getKey() < cutoffBucket);
        }
    }
}
