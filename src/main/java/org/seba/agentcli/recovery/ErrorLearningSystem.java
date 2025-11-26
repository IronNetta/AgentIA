package org.seba.agentcli.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.seba.agentcli.io.AnsiColors;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Learns from errors and successful resolutions
 * Persists knowledge to improve future error handling
 */
@Component
public class ErrorLearningSystem {

    private static final String KNOWLEDGE_FILE = ".agentcli/error-knowledge.json";
    private static final int MAX_LEARNED_PATTERNS = 200;

    private final Map<String, LearnedPattern> learnedPatterns = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ErrorLearningSystem() {
        loadKnowledge();
    }

    /**
     * Records a successful error resolution
     */
    public void recordSuccessfulResolution(ErrorRecoveryManager.ErrorRecord error,
                                          String solution,
                                          String outcome) {
        String errorSignature = generateErrorSignature(error);

        LearnedPattern pattern = learnedPatterns.computeIfAbsent(
            errorSignature,
            k -> new LearnedPattern(errorSignature, error.type, error.message)
        );

        pattern.recordResolution(new Resolution(solution, outcome, LocalDateTime.now()));
        pattern.incrementSuccessCount();

        // Save to disk
        saveKnowledge();
    }

    /**
     * Records a failed resolution attempt
     */
    public void recordFailedResolution(ErrorRecoveryManager.ErrorRecord error,
                                      String attemptedSolution,
                                      String reason) {
        String errorSignature = generateErrorSignature(error);

        LearnedPattern pattern = learnedPatterns.computeIfAbsent(
            errorSignature,
            k -> new LearnedPattern(errorSignature, error.type, error.message)
        );

        pattern.recordFailedAttempt(attemptedSolution, reason);
        pattern.incrementFailureCount();

        saveKnowledge();
    }

    /**
     * Gets learned solutions for an error
     */
    public List<LearnedSolution> getLearnedSolutions(ErrorRecoveryManager.ErrorRecord error) {
        String errorSignature = generateErrorSignature(error);
        List<LearnedSolution> solutions = new ArrayList<>();

        // Exact match
        LearnedPattern exactMatch = learnedPatterns.get(errorSignature);
        if (exactMatch != null && exactMatch.hasSuccessfulResolutions()) {
            solutions.addAll(exactMatch.getTopResolutions(3));
        }

        // Similar patterns
        List<LearnedPattern> similarPatterns = findSimilarPatterns(error);
        for (LearnedPattern pattern : similarPatterns) {
            if (pattern.hasSuccessfulResolutions()) {
                solutions.addAll(pattern.getTopResolutions(2));
            }
            if (solutions.size() >= 5) break;
        }

        return solutions.stream()
            .distinct()
            .sorted((a, b) -> Double.compare(b.confidence, a.confidence))
            .limit(5)
            .collect(Collectors.toList());
    }

    /**
     * Gets learning insights for display
     */
    public LearningInsights getInsights() {
        int totalPatterns = learnedPatterns.size();
        int totalSuccesses = learnedPatterns.values().stream()
            .mapToInt(p -> p.successCount)
            .sum();
        int totalFailures = learnedPatterns.values().stream()
            .mapToInt(p -> p.failureCount)
            .sum();

        Map<String, Integer> patternsByType = learnedPatterns.values().stream()
            .collect(Collectors.groupingBy(
                p -> p.errorType,
                Collectors.summingInt(p -> 1)
            ));

        List<LearnedPattern> topPatterns = learnedPatterns.values().stream()
            .sorted((a, b) -> Integer.compare(b.successCount, a.successCount))
            .limit(10)
            .collect(Collectors.toList());

        return new LearningInsights(
            totalPatterns,
            totalSuccesses,
            totalFailures,
            patternsByType,
            topPatterns
        );
    }

    /**
     * Generates a signature for an error pattern
     */
    private String generateErrorSignature(ErrorRecoveryManager.ErrorRecord error) {
        // Create a signature based on error type and key words from message
        StringBuilder signature = new StringBuilder(error.type);

        if (error.message != null) {
            // Extract key words (simplified approach)
            String[] words = error.message.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+");

            // Keep meaningful words (length >= 4)
            String keyWords = Arrays.stream(words)
                .filter(w -> w.length() >= 4)
                .limit(5)
                .collect(Collectors.joining("_"));

            signature.append("_").append(keyWords);
        }

        return signature.toString();
    }

    /**
     * Finds patterns similar to the given error
     */
    private List<LearnedPattern> findSimilarPatterns(ErrorRecoveryManager.ErrorRecord error) {
        return learnedPatterns.values().stream()
            .filter(pattern -> pattern.errorType.equals(error.type))
            .filter(pattern -> calculateSimilarity(error.message, pattern.sampleMessage) > 0.3)
            .sorted((a, b) -> Double.compare(
                calculateSimilarity(error.message, b.sampleMessage),
                calculateSimilarity(error.message, a.sampleMessage)
            ))
            .limit(3)
            .collect(Collectors.toList());
    }

    /**
     * Calculates similarity between two messages (0.0 to 1.0)
     */
    private double calculateSimilarity(String msg1, String msg2) {
        if (msg1 == null || msg2 == null) return 0.0;

        Set<String> words1 = new HashSet<>(Arrays.asList(msg1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(msg2.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Loads knowledge from disk
     */
    private void loadKnowledge() {
        Path path = Paths.get(KNOWLEDGE_FILE);
        if (!Files.exists(path)) {
            return;
        }

        try {
            String json = Files.readString(path);
            KnowledgeBase kb = objectMapper.readValue(json, KnowledgeBase.class);

            learnedPatterns.clear();
            for (LearnedPattern pattern : kb.patterns) {
                learnedPatterns.put(pattern.signature, pattern);
            }

        } catch (IOException e) {
            System.err.println("Warning: Could not load error knowledge: " + e.getMessage());
        }
    }

    /**
     * Saves knowledge to disk
     */
    private void saveKnowledge() {
        try {
            Path path = Paths.get(KNOWLEDGE_FILE);
            Files.createDirectories(path.getParent());

            // Limit stored patterns
            List<LearnedPattern> topPatterns = learnedPatterns.values().stream()
                .sorted((a, b) -> Integer.compare(
                    b.successCount + b.failureCount,
                    a.successCount + a.failureCount
                ))
                .limit(MAX_LEARNED_PATTERNS)
                .collect(Collectors.toList());

            KnowledgeBase kb = new KnowledgeBase(topPatterns);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(kb);

            Files.writeString(path, json);

        } catch (IOException e) {
            System.err.println("Warning: Could not save error knowledge: " + e.getMessage());
        }
    }

    /**
     * Clears all learned knowledge
     */
    public void clearKnowledge() {
        learnedPatterns.clear();
        try {
            Files.deleteIfExists(Paths.get(KNOWLEDGE_FILE));
        } catch (IOException e) {
            // Ignore
        }
    }

    // === Data Classes ===

    /**
     * Represents a learned error pattern
     */
    public static class LearnedPattern {
        public String signature;
        public String errorType;
        public String sampleMessage;
        public int successCount;
        public int failureCount;
        public List<Resolution> successfulResolutions = new ArrayList<>();
        public List<FailedAttempt> failedAttempts = new ArrayList<>();

        public LearnedPattern() {} // For Jackson

        public LearnedPattern(String signature, String errorType, String sampleMessage) {
            this.signature = signature;
            this.errorType = errorType;
            this.sampleMessage = sampleMessage;
            this.successCount = 0;
            this.failureCount = 0;
        }

        public void recordResolution(Resolution resolution) {
            successfulResolutions.add(resolution);
            // Keep only recent resolutions (max 10)
            if (successfulResolutions.size() > 10) {
                successfulResolutions.remove(0);
            }
        }

        public void recordFailedAttempt(String solution, String reason) {
            failedAttempts.add(new FailedAttempt(solution, reason));
            if (failedAttempts.size() > 5) {
                failedAttempts.remove(0);
            }
        }

        public void incrementSuccessCount() {
            successCount++;
        }

        public void incrementFailureCount() {
            failureCount++;
        }

        public boolean hasSuccessfulResolutions() {
            return !successfulResolutions.isEmpty();
        }

        public List<LearnedSolution> getTopResolutions(int limit) {
            Map<String, Integer> solutionCounts = new HashMap<>();

            for (Resolution res : successfulResolutions) {
                solutionCounts.merge(res.solution, 1, Integer::sum);
            }

            return solutionCounts.entrySet().stream()
                .map(e -> {
                    double confidence = (double) e.getValue() / successfulResolutions.size();
                    String outcome = successfulResolutions.stream()
                        .filter(r -> r.solution.equals(e.getKey()))
                        .map(r -> r.outcome)
                        .findFirst()
                        .orElse("");
                    return new LearnedSolution(e.getKey(), confidence, e.getValue(), outcome);
                })
                .sorted((a, b) -> Integer.compare(b.usageCount, a.usageCount))
                .limit(limit)
                .collect(Collectors.toList());
        }

        public double getSuccessRate() {
            int total = successCount + failureCount;
            return total == 0 ? 0.0 : (double) successCount / total;
        }
    }

    /**
     * A learned solution for an error
     */
    public static class LearnedSolution {
        public final String solution;
        public final double confidence;
        public final int usageCount;
        public final String outcome;

        public LearnedSolution(String solution, double confidence, int usageCount, String outcome) {
            this.solution = solution;
            this.confidence = confidence;
            this.usageCount = usageCount;
            this.outcome = outcome;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LearnedSolution that = (LearnedSolution) o;
            return Objects.equals(solution, that.solution);
        }

        @Override
        public int hashCode() {
            return Objects.hash(solution);
        }
    }

    /**
     * A successful resolution
     */
    public static class Resolution {
        public String solution;
        public String outcome;
        public LocalDateTime timestamp;

        public Resolution() {} // For Jackson

        public Resolution(String solution, String outcome, LocalDateTime timestamp) {
            this.solution = solution;
            this.outcome = outcome;
            this.timestamp = timestamp;
        }
    }

    /**
     * A failed attempt
     */
    public static class FailedAttempt {
        public String attemptedSolution;
        public String reason;

        public FailedAttempt() {} // For Jackson

        public FailedAttempt(String attemptedSolution, String reason) {
            this.attemptedSolution = attemptedSolution;
            this.reason = reason;
        }
    }

    /**
     * Learning insights
     */
    public static class LearningInsights {
        public final int totalPatterns;
        public final int totalSuccesses;
        public final int totalFailures;
        public final Map<String, Integer> patternsByType;
        public final List<LearnedPattern> topPatterns;

        public LearningInsights(int totalPatterns, int totalSuccesses, int totalFailures,
                               Map<String, Integer> patternsByType, List<LearnedPattern> topPatterns) {
            this.totalPatterns = totalPatterns;
            this.totalSuccesses = totalSuccesses;
            this.totalFailures = totalFailures;
            this.patternsByType = patternsByType;
            this.topPatterns = topPatterns;
        }

        public String format() {
            StringBuilder output = new StringBuilder();
            output.append(AnsiColors.colorize("═".repeat(70), AnsiColors.CYAN)).append("\n");
            output.append(AnsiColors.colorize("  ERROR LEARNING INSIGHTS", AnsiColors.BOLD_CYAN)).append("\n");
            output.append(AnsiColors.colorize("═".repeat(70), AnsiColors.CYAN)).append("\n\n");

            output.append(String.format("Total Patterns Learned: %s\n",
                AnsiColors.colorize(String.valueOf(totalPatterns), AnsiColors.BOLD_WHITE)));
            output.append(String.format("Successful Resolutions: %s\n",
                AnsiColors.colorize(String.valueOf(totalSuccesses), AnsiColors.GREEN)));
            output.append(String.format("Failed Attempts: %s\n\n",
                AnsiColors.colorize(String.valueOf(totalFailures), AnsiColors.RED)));

            if (!patternsByType.isEmpty()) {
                output.append(AnsiColors.colorize("Patterns by Error Type:", AnsiColors.BOLD_WHITE)).append("\n");
                patternsByType.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(e -> output.append(String.format("  %-30s: %d\n", e.getKey(), e.getValue())));
                output.append("\n");
            }

            if (!topPatterns.isEmpty()) {
                output.append(AnsiColors.colorize("Most Resolved Patterns:", AnsiColors.BOLD_WHITE)).append("\n");
                for (int i = 0; i < Math.min(5, topPatterns.size()); i++) {
                    LearnedPattern pattern = topPatterns.get(i);
                    double successRate = pattern.getSuccessRate() * 100;
                    output.append(String.format("  %d. %s (%d successes, %.1f%% success rate)\n",
                        i + 1, pattern.errorType, pattern.successCount, successRate));
                }
            }

            return output.toString();
        }
    }

    /**
     * Serializable knowledge base
     */
    private static class KnowledgeBase {
        public List<LearnedPattern> patterns;

        public KnowledgeBase() {} // For Jackson

        public KnowledgeBase(List<LearnedPattern> patterns) {
            this.patterns = patterns;
        }
    }
}
