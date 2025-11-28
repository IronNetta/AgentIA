package org.seba.agentcli.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter to prevent abuse of file operations
 * Uses a sliding window algorithm to track operation counts
 */
@Component
public class RateLimiter {

    private final SecurityLogger securityLogger;

    // Default limits
    private static final int DEFAULT_MAX_OPERATIONS_PER_MINUTE = 100;
    private static final int DEFAULT_MAX_OPERATIONS_PER_SECOND = 10;

    // Tracking maps: operation -> (timestamp, count)
    private final Map<String, RateLimitWindow> operationWindows = new ConcurrentHashMap<>();

    public RateLimiter(SecurityLogger securityLogger) {
        this.securityLogger = securityLogger;
    }

    /**
     * Checks if an operation is allowed under rate limits
     *
     * @param operation The operation type (e.g., "file_read", "file_write")
     * @return true if operation is allowed, false if rate limit exceeded
     */
    public boolean allowOperation(String operation) {
        return allowOperation(operation, DEFAULT_MAX_OPERATIONS_PER_SECOND, DEFAULT_MAX_OPERATIONS_PER_MINUTE);
    }

    /**
     * Checks if an operation is allowed with custom limits
     *
     * @param operation The operation type
     * @param maxPerSecond Maximum operations per second
     * @param maxPerMinute Maximum operations per minute
     * @return true if operation is allowed
     */
    public boolean allowOperation(String operation, int maxPerSecond, int maxPerMinute) {
        RateLimitWindow window = operationWindows.computeIfAbsent(
            operation,
            k -> new RateLimitWindow()
        );

        long now = Instant.now().toEpochMilli();

        // Clean old entries
        window.cleanOldEntries(now);

        // Check per-second limit
        int countLastSecond = window.getCountInWindow(now, 1000);
        if (countLastSecond >= maxPerSecond) {
            securityLogger.logRateLimitExceeded(operation, countLastSecond, maxPerSecond);
            return false;
        }

        // Check per-minute limit
        int countLastMinute = window.getCountInWindow(now, 60000);
        if (countLastMinute >= maxPerMinute) {
            securityLogger.logRateLimitExceeded(operation, countLastMinute, maxPerMinute);
            return false;
        }

        // Record this operation
        window.recordOperation(now);

        return true;
    }

    /**
     * Checks if operation is allowed and throws exception if not
     *
     * @param operation The operation type
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public void checkRateLimit(String operation) throws RateLimitExceededException {
        if (!allowOperation(operation)) {
            throw new RateLimitExceededException(
                "Rate limit exceeded for operation: " + operation
            );
        }
    }

    /**
     * Resets rate limits for an operation (for testing)
     */
    public void reset(String operation) {
        operationWindows.remove(operation);
    }

    /**
     * Resets all rate limits (for testing)
     */
    public void resetAll() {
        operationWindows.clear();
    }

    /**
     * Gets current operation count for an operation
     */
    public int getCurrentCount(String operation, int windowMillis) {
        RateLimitWindow window = operationWindows.get(operation);
        if (window == null) {
            return 0;
        }
        long now = Instant.now().toEpochMilli();
        return window.getCountInWindow(now, windowMillis);
    }

    /**
     * Sliding window for tracking operations
     */
    private static class RateLimitWindow {
        // Circular buffer of timestamps
        private final long[] timestamps;
        private int head = 0;
        private final AtomicInteger size = new AtomicInteger(0);
        private static final int MAX_BUFFER_SIZE = 200;

        public RateLimitWindow() {
            this.timestamps = new long[MAX_BUFFER_SIZE];
        }

        public synchronized void recordOperation(long timestamp) {
            timestamps[head] = timestamp;
            head = (head + 1) % MAX_BUFFER_SIZE;
            if (size.get() < MAX_BUFFER_SIZE) {
                size.incrementAndGet();
            }
        }

        public synchronized int getCountInWindow(long now, long windowMillis) {
            long cutoff = now - windowMillis;
            int count = 0;

            for (int i = 0; i < size.get(); i++) {
                if (timestamps[i] >= cutoff && timestamps[i] <= now) {
                    count++;
                }
            }

            return count;
        }

        public synchronized void cleanOldEntries(long now) {
            // Remove entries older than 1 minute
            long cutoff = now - 60000;
            int validCount = 0;

            for (int i = 0; i < size.get(); i++) {
                if (timestamps[i] >= cutoff) {
                    validCount++;
                }
            }

            size.set(validCount);
        }
    }

    /**
     * Exception thrown when rate limit is exceeded
     */
    public static class RateLimitExceededException extends Exception {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
