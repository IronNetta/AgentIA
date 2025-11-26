package org.seba.agentcli.review;

import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automated code review service
 * Analyzes code for issues, best practices, and potential improvements
 */
@Component
public class CodeReviewService {

    /**
     * Reviews a file and returns findings
     */
    public ReviewResult reviewFile(Path file) {
        try {
            String content = Files.readString(file);
            String extension = getFileExtension(file);

            List<ReviewFinding> findings = new ArrayList<>();

            // Apply language-specific rules
            switch (extension) {
                case "java" -> findings.addAll(reviewJava(content, file));
                case "py" -> findings.addAll(reviewPython(content, file));
                case "js", "ts" -> findings.addAll(reviewJavaScript(content, file));
                default -> findings.addAll(reviewGeneric(content, file));
            }

            // Calculate quality score
            double score = calculateQualityScore(content, findings);

            return new ReviewResult(file, findings, score);

        } catch (IOException e) {
            return new ReviewResult(file, List.of(
                new ReviewFinding(Severity.ERROR, "IO Error", "Could not read file: " + e.getMessage(), 0)
            ), 0.0);
        }
    }

    /**
     * Reviews multiple files
     */
    public MultiFileReviewResult reviewFiles(List<Path> files) {
        List<ReviewResult> results = new ArrayList<>();

        for (Path file : files) {
            results.add(reviewFile(file));
        }

        return new MultiFileReviewResult(results);
    }

    /**
     * Reviews Java code
     */
    private List<ReviewFinding> reviewJava(String content, Path file) {
        List<ReviewFinding> findings = new ArrayList<>();

        // Check for common issues
        findings.addAll(checkForLongMethods(content, 50));
        findings.addAll(checkForLargeClasses(content, 500));
        findings.addAll(checkForEmptyCatchBlocks(content));
        findings.addAll(checkForTodoComments(content));
        findings.addAll(checkForPrintStatements(content, List.of("System.out", "System.err")));
        findings.addAll(checkForHardcodedStrings(content));
        findings.addAll(checkForMagicNumbers(content));
        findings.addAll(checkForLongParameterLists(content, 5));
        findings.addAll(checkForDeepNesting(content, 4));

        // Java-specific checks
        findings.addAll(checkJavaExceptionHandling(content));
        findings.addAll(checkJavaNullChecks(content));
        findings.addAll(checkJavaResourceLeaks(content));

        return findings;
    }

    /**
     * Reviews Python code
     */
    private List<ReviewFinding> reviewPython(String content, Path file) {
        List<ReviewFinding> findings = new ArrayList<>();

        findings.addAll(checkForLongMethods(content, 40));
        findings.addAll(checkForTodoComments(content));
        findings.addAll(checkForPrintStatements(content, List.of("print(")));
        findings.addAll(checkForDeepNesting(content, 4));

        // Python-specific
        findings.addAll(checkPythonImports(content));
        findings.addAll(checkPythonExceptionHandling(content));

        return findings;
    }

    /**
     * Reviews JavaScript/TypeScript code
     */
    private List<ReviewFinding> reviewJavaScript(String content, Path file) {
        List<ReviewFinding> findings = new ArrayList<>();

        findings.addAll(checkForLongMethods(content, 40));
        findings.addAll(checkForTodoComments(content));
        findings.addAll(checkForPrintStatements(content, List.of("console.log", "console.error")));
        findings.addAll(checkForDeepNesting(content, 4));

        // JS-specific
        findings.addAll(checkJavaScriptVarUsage(content));
        findings.addAll(checkJavaScriptEquality(content));

        return findings;
    }

    /**
     * Generic code review
     */
    private List<ReviewFinding> reviewGeneric(String content, Path file) {
        List<ReviewFinding> findings = new ArrayList<>();

        findings.addAll(checkForTodoComments(content));
        findings.addAll(checkForLongLines(content, 120));
        findings.addAll(checkForTrailingWhitespace(content));

        return findings;
    }

    // === Check Methods ===

    private List<ReviewFinding> checkForLongMethods(String content, int maxLines) {
        List<ReviewFinding> findings = new ArrayList<>();
        Pattern methodPattern = Pattern.compile("(function|def|public|private|protected)\\s+\\w+.*\\{", Pattern.MULTILINE);
        Matcher matcher = methodPattern.matcher(content);

        while (matcher.find()) {
            int start = matcher.start();
            int braceCount = 1;
            int end = matcher.end();
            int lineStart = getLineNumber(content, start);

            // Find matching closing brace
            for (int i = end; i < content.length() && braceCount > 0; i++) {
                if (content.charAt(i) == '{') braceCount++;
                if (content.charAt(i) == '}') braceCount--;
                if (braceCount == 0) end = i;
            }

            int lineEnd = getLineNumber(content, end);
            int methodLines = lineEnd - lineStart;

            if (methodLines > maxLines) {
                findings.add(new ReviewFinding(
                    Severity.WARNING,
                    "Long Method",
                    String.format("Method has %d lines (max recommended: %d). Consider breaking it down.", methodLines, maxLines),
                    lineStart
                ));
            }
        }

        return findings;
    }

    private List<ReviewFinding> checkForLargeClasses(String content, int maxLines) {
        int lineCount = content.split("\n").length;
        if (lineCount > maxLines) {
            return List.of(new ReviewFinding(
                Severity.WARNING,
                "Large Class",
                String.format("File has %d lines (max recommended: %d). Consider splitting into smaller classes.", lineCount, maxLines),
                1
            ));
        }
        return List.of();
    }

    private List<ReviewFinding> checkForEmptyCatchBlocks(String content) {
        List<ReviewFinding> findings = new ArrayList<>();
        Pattern pattern = Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{\\s*}", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int line = getLineNumber(content, matcher.start());
            findings.add(new ReviewFinding(
                Severity.ERROR,
                "Empty Catch Block",
                "Catch block is empty. Either handle the exception or add a comment explaining why it's ignored.",
                line
            ));
        }

        return findings;
    }

    private List<ReviewFinding> checkForTodoComments(String content) {
        List<ReviewFinding> findings = new ArrayList<>();
        Pattern pattern = Pattern.compile("(//|#|/\\*)\\s*TODO:", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int line = getLineNumber(content, matcher.start());
            findings.add(new ReviewFinding(
                Severity.INFO,
                "TODO Comment",
                "TODO comment found. Consider addressing before commit.",
                line
            ));
        }

        return findings;
    }

    private List<ReviewFinding> checkForPrintStatements(String content, List<String> patterns) {
        List<ReviewFinding> findings = new ArrayList<>();

        for (String pattern : patterns) {
            Pattern p = Pattern.compile(Pattern.quote(pattern));
            Matcher matcher = p.matcher(content);

            while (matcher.find()) {
                int line = getLineNumber(content, matcher.start());
                findings.add(new ReviewFinding(
                    Severity.WARNING,
                    "Debug Statement",
                    "Found " + pattern + ". Remove debug statements before commit.",
                    line
                ));
            }
        }

        return findings;
    }

    private List<ReviewFinding> checkForHardcodedStrings(String content) {
        List<ReviewFinding> findings = new ArrayList<>();
        Pattern pattern = Pattern.compile("(password|token|api[_-]?key|secret)\\s*=\\s*\"[^\"]+\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int line = getLineNumber(content, matcher.start());
            findings.add(new ReviewFinding(
                Severity.ERROR,
                "Hardcoded Credentials",
                "Potential hardcoded credential detected. Use environment variables or config files.",
                line
            ));
        }

        return findings;
    }

    private List<ReviewFinding> checkForMagicNumbers(String content) {
        // Skip for now - too many false positives
        return List.of();
    }

    private List<ReviewFinding> checkForLongParameterLists(String content, int maxParams) {
        List<ReviewFinding> findings = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\([^)]{100,}\\)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String params = matcher.group();
            int commaCount = params.split(",").length;

            if (commaCount > maxParams) {
                int line = getLineNumber(content, matcher.start());
                findings.add(new ReviewFinding(
                    Severity.WARNING,
                    "Long Parameter List",
                    String.format("Method has %d parameters (max recommended: %d). Consider using a parameter object.", commaCount, maxParams),
                    line
                ));
            }
        }

        return findings;
    }

    private List<ReviewFinding> checkForDeepNesting(String content, int maxDepth) {
        List<ReviewFinding> findings = new ArrayList<>();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int depth = countIndentationDepth(line);

            if (depth > maxDepth) {
                findings.add(new ReviewFinding(
                    Severity.WARNING,
                    "Deep Nesting",
                    String.format("Code is nested %d levels deep (max recommended: %d). Consider extracting methods.", depth, maxDepth),
                    i + 1
                ));
            }
        }

        return findings;
    }

    private List<ReviewFinding> checkForLongLines(String content, int maxLength) {
        List<ReviewFinding> findings = new ArrayList<>();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() > maxLength) {
                findings.add(new ReviewFinding(
                    Severity.INFO,
                    "Long Line",
                    String.format("Line is %d characters (max recommended: %d).", lines[i].length(), maxLength),
                    i + 1
                ));
            }
        }

        return findings;
    }

    private List<ReviewFinding> checkForTrailingWhitespace(String content) {
        List<ReviewFinding> findings = new ArrayList<>();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].matches(".*\\s+$")) {
                findings.add(new ReviewFinding(
                    Severity.INFO,
                    "Trailing Whitespace",
                    "Line has trailing whitespace.",
                    i + 1
                ));
            }
        }

        return findings;
    }

    // Language-specific checks

    private List<ReviewFinding> checkJavaExceptionHandling(String content) {
        List<ReviewFinding> findings = new ArrayList<>();
        Pattern pattern = Pattern.compile("catch\\s*\\(\\s*Exception\\s+\\w+\\s*\\)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int line = getLineNumber(content, matcher.start());
            findings.add(new ReviewFinding(
                Severity.WARNING,
                "Generic Exception Catch",
                "Catching generic Exception. Consider catching specific exception types.",
                line
            ));
        }

        return findings;
    }

    private List<ReviewFinding> checkJavaNullChecks(String content) {
        List<ReviewFinding> findings = new ArrayList<>();
        Pattern pattern = Pattern.compile("==\\s*null|!=\\s*null");
        Matcher matcher = pattern.matcher(content);

        int count = 0;
        while (matcher.find()) count++;

        if (count > 10) {
            findings.add(new ReviewFinding(
                Severity.INFO,
                "Many Null Checks",
                String.format("File has %d null checks. Consider using Optional or null-safe libraries.", count),
                1
            ));
        }

        return findings;
    }

    private List<ReviewFinding> checkJavaResourceLeaks(String content) {
        List<ReviewFinding> findings = new ArrayList<>();
        Pattern pattern = Pattern.compile("new\\s+(FileReader|FileWriter|BufferedReader|BufferedWriter|InputStream|OutputStream)\\s*\\(");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            // Check if it's in a try-with-resources
            int pos = matcher.start();
            String before = content.substring(Math.max(0, pos - 100), pos);

            if (!before.contains("try (")) {
                int line = getLineNumber(content, pos);
                findings.add(new ReviewFinding(
                    Severity.WARNING,
                    "Potential Resource Leak",
                    "Resource might not be properly closed. Use try-with-resources.",
                    line
                ));
            }
        }

        return findings;
    }

    private List<ReviewFinding> checkPythonImports(String content) {
        List<ReviewFinding> findings = new ArrayList<>();
        Pattern pattern = Pattern.compile("from\\s+\\w+\\s+import\\s+\\*");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int line = getLineNumber(content, matcher.start());
            findings.add(new ReviewFinding(
                Severity.WARNING,
                "Wildcard Import",
                "Avoid wildcard imports. Import specific names instead.",
                line
            ));
        }

        return findings;
    }

    private List<ReviewFinding> checkPythonExceptionHandling(String content) {
        List<ReviewFinding> findings = new ArrayList<>();
        Pattern pattern = Pattern.compile("except:");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int line = getLineNumber(content, matcher.start());
            findings.add(new ReviewFinding(
                Severity.WARNING,
                "Bare Except",
                "Bare except catches all exceptions. Specify exception types.",
                line
            ));
        }

        return findings;
    }

    private List<ReviewFinding> checkJavaScriptVarUsage(String content) {
        List<ReviewFinding> findings = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\bvar\\s+\\w+");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int line = getLineNumber(content, matcher.start());
            findings.add(new ReviewFinding(
                Severity.WARNING,
                "Var Usage",
                "Use 'let' or 'const' instead of 'var'.",
                line
            ));
        }

        return findings;
    }

    private List<ReviewFinding> checkJavaScriptEquality(String content) {
        List<ReviewFinding> findings = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\s(==|!=)\\s");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int line = getLineNumber(content, matcher.start());
            findings.add(new ReviewFinding(
                Severity.INFO,
                "Loose Equality",
                "Consider using strict equality (=== or !==).",
                line
            ));
        }

        return findings;
    }

    // === Helper Methods ===

    private int getLineNumber(String content, int position) {
        return content.substring(0, position).split("\n").length;
    }

    private int countIndentationDepth(String line) {
        int depth = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ' || c == '\t') {
                depth++;
            } else {
                break;
            }
        }
        return depth / 4; // Assume 4 spaces per level
    }

    private double calculateQualityScore(String content, List<ReviewFinding> findings) {
        double score = 100.0;

        for (ReviewFinding finding : findings) {
            score -= switch (finding.severity) {
                case ERROR -> 10.0;
                case WARNING -> 5.0;
                case INFO -> 1.0;
            };
        }

        return Math.max(0, score);
    }

    private String getFileExtension(Path file) {
        String name = file.getFileName().toString();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1) : "";
    }

    // === Data Classes ===

    public enum Severity {
        ERROR, WARNING, INFO
    }

    public static class ReviewFinding {
        public final Severity severity;
        public final String category;
        public final String message;
        public final int lineNumber;

        public ReviewFinding(Severity severity, String category, String message, int lineNumber) {
            this.severity = severity;
            this.category = category;
            this.message = message;
            this.lineNumber = lineNumber;
        }
    }

    public static class ReviewResult {
        public final Path file;
        public final List<ReviewFinding> findings;
        public final double qualityScore;

        public ReviewResult(Path file, List<ReviewFinding> findings, double qualityScore) {
            this.file = file;
            this.findings = findings;
            this.qualityScore = qualityScore;
        }

        public int getErrorCount() {
            return (int) findings.stream().filter(f -> f.severity == Severity.ERROR).count();
        }

        public int getWarningCount() {
            return (int) findings.stream().filter(f -> f.severity == Severity.WARNING).count();
        }

        public int getInfoCount() {
            return (int) findings.stream().filter(f -> f.severity == Severity.INFO).count();
        }
    }

    public static class MultiFileReviewResult {
        public final List<ReviewResult> results;

        public MultiFileReviewResult(List<ReviewResult> results) {
            this.results = results;
        }

        public int getTotalFindings() {
            return results.stream().mapToInt(r -> r.findings.size()).sum();
        }

        public double getAverageScore() {
            return results.stream().mapToDouble(r -> r.qualityScore).average().orElse(0.0);
        }
    }
}
