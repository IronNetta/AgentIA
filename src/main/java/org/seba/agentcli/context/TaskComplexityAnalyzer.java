package org.seba.agentcli.context;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Analyzes user requests to determine task complexity
 * Helps decide when to automatically create a plan
 */
@Component
public class TaskComplexityAnalyzer {

    // Keywords indicating complex tasks
    private static final List<String> COMPLEX_ACTION_KEYWORDS = Arrays.asList(
            "add", "create", "implement", "build", "develop", "setup", "configure",
            "integrate", "migrate", "refactor", "redesign", "restructure",
            "ajoute", "crée", "implémente", "développe", "configure", "intègre"
    );

    private static final List<String> COMPLEXITY_INDICATORS = Arrays.asList(
            "system", "feature", "functionality", "module", "component", "service",
            "authentication", "authorization", "api", "database", "architecture",
            "système", "fonctionnalité", "module", "composant", "service",
            "authentification", "base de données", "architecture"
    );

    // Keywords indicating simple questions or queries
    private static final List<String> SIMPLE_QUERY_KEYWORDS = Arrays.asList(
            "what", "how", "where", "when", "why", "explain", "show", "display",
            "comment", "où", "quand", "pourquoi", "explique", "montre", "affiche"
    );

    /**
     * Analyzes a user request and returns complexity assessment
     */
    public ComplexityResult analyze(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return new ComplexityResult(ComplexityLevel.SIMPLE, "Empty input", 0);
        }

        String input = userInput.toLowerCase().trim();

        // Check if it's a tool command
        if (input.startsWith("@")) {
            return new ComplexityResult(ComplexityLevel.SIMPLE, "Tool command", 0);
        }

        int complexityScore = 0;
        StringBuilder reasoning = new StringBuilder();

        // 1. Check for simple query patterns
        boolean isQuestion = input.contains("?");
        boolean hasSimpleQueryKeyword = SIMPLE_QUERY_KEYWORDS.stream()
                .anyMatch(keyword -> input.startsWith(keyword) || input.contains(" " + keyword + " "));

        if (isQuestion && hasSimpleQueryKeyword) {
            return new ComplexityResult(ComplexityLevel.SIMPLE, "Simple question", 0);
        }

        // 2. Check for complex action keywords
        long complexActionCount = COMPLEX_ACTION_KEYWORDS.stream()
                .filter(keyword -> containsWord(input, keyword))
                .count();

        if (complexActionCount > 0) {
            complexityScore += (int) complexActionCount * 2;
            reasoning.append("Action keywords: ").append(complexActionCount).append("; ");
        }

        // 3. Check for complexity indicators
        long complexityIndicatorCount = COMPLEXITY_INDICATORS.stream()
                .filter(keyword -> containsWord(input, keyword))
                .count();

        if (complexityIndicatorCount > 0) {
            complexityScore += (int) complexityIndicatorCount * 3;
            reasoning.append("Complexity indicators: ").append(complexityIndicatorCount).append("; ");
        }

        // 4. Check for multiple verbs (indicates multiple steps)
        int verbCount = countVerbs(input);
        if (verbCount >= 2) {
            complexityScore += (verbCount - 1) * 2;
            reasoning.append("Multiple verbs: ").append(verbCount).append("; ");
        }

        // 5. Check for multiple file/component mentions
        int fileComponentCount = countFileComponentMentions(input);
        if (fileComponentCount >= 2) {
            complexityScore += fileComponentCount * 2;
            reasoning.append("Multiple files/components: ").append(fileComponentCount).append("; ");
        }

        // 6. Check for words indicating multiple steps
        if (input.contains(" and ") || input.contains(" et ")) {
            complexityScore += 3;
            reasoning.append("Conjunction 'and'; ");
        }

        // 7. Input length as complexity indicator
        int wordCount = input.split("\\s+").length;
        if (wordCount > 15) {
            complexityScore += 2;
            reasoning.append("Long input (").append(wordCount).append(" words); ");
        }

        // Determine complexity level
        ComplexityLevel level;
        if (complexityScore >= 10) {
            level = ComplexityLevel.VERY_COMPLEX;
        } else if (complexityScore >= 6) {
            level = ComplexityLevel.COMPLEX;
        } else if (complexityScore >= 3) {
            level = ComplexityLevel.MODERATE;
        } else {
            level = ComplexityLevel.SIMPLE;
        }

        String finalReasoning = reasoning.length() > 0 ? reasoning.toString() : "Basic task";

        return new ComplexityResult(level, finalReasoning, complexityScore);
    }

    /**
     * Checks if a word exists as a whole word in the input
     */
    private boolean containsWord(String input, String word) {
        String pattern = "\\b" + Pattern.quote(word) + "\\b";
        return Pattern.compile(pattern).matcher(input).find();
    }

    /**
     * Counts action verbs in the input
     */
    private int countVerbs(String input) {
        List<String> commonVerbs = Arrays.asList(
                "add", "create", "implement", "build", "make", "write", "modify", "update",
                "delete", "remove", "change", "configure", "setup", "install", "integrate",
                "ajoute", "crée", "implémente", "construis", "fais", "écris", "modifie",
                "supprime", "change", "configure", "installe", "intègre"
        );

        return (int) commonVerbs.stream()
                .filter(verb -> containsWord(input, verb))
                .count();
    }

    /**
     * Counts mentions of files or components
     */
    private int countFileComponentMentions(String input) {
        int count = 0;

        // Check for file extensions
        if (input.matches(".*\\.(java|py|js|ts|xml|json|yaml|yml).*")) {
            count += input.split("\\.(java|py|js|ts|xml|json|yaml|yml)").length - 1;
        }

        // Check for component keywords
        List<String> componentKeywords = Arrays.asList(
                "controller", "service", "repository", "model", "entity", "dto",
                "config", "configuration", "filter", "interceptor", "handler"
        );

        count += (int) componentKeywords.stream()
                .filter(keyword -> containsWord(input, keyword))
                .count();

        return count;
    }

    /**
     * Suggests whether a plan should be created
     */
    public boolean shouldSuggestPlan(ComplexityResult result) {
        return result.getLevel() == ComplexityLevel.COMPLEX ||
               result.getLevel() == ComplexityLevel.VERY_COMPLEX;
    }

    /**
     * Result of complexity analysis
     */
    public static class ComplexityResult {
        private final ComplexityLevel level;
        private final String reasoning;
        private final int score;

        public ComplexityResult(ComplexityLevel level, String reasoning, int score) {
            this.level = level;
            this.reasoning = reasoning;
            this.score = score;
        }

        public ComplexityLevel getLevel() {
            return level;
        }

        public String getReasoning() {
            return reasoning;
        }

        public int getScore() {
            return score;
        }

        @Override
        public String toString() {
            return String.format("Level: %s, Score: %d, Reasoning: %s", level, score, reasoning);
        }
    }

    /**
     * Complexity levels
     */
    public enum ComplexityLevel {
        SIMPLE,         // Single step, straightforward
        MODERATE,       // 2-3 steps, manageable
        COMPLEX,        // 4-6 steps, benefits from planning
        VERY_COMPLEX    // 7+ steps, definitely needs planning
    }
}
