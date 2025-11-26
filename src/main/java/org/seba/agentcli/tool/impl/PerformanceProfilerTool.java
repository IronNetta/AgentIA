package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performance profiler and optimizer.
 *
 * Commands:
 *   @perf analyze          - Detect performance bottlenecks
 *   @perf suggest          - Get optimization suggestions
 *   @perf benchmark        - Run benchmarks
 *   @perf compare          - Compare before/after performance
 */
@Component
public class PerformanceProfilerTool extends AbstractTool {

    public PerformanceProfilerTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@perf";
    }

    @Override
    public String getDescription() {
        return "Performance profiler and optimization advisor";
    }

    @Override
    public String getUsage() {
        return "@perf <command>\n\n" +
               "Commands:\n" +
               "  analyze        Detect performance bottlenecks\n" +
               "  suggest        Get AI optimization suggestions\n" +
               "  benchmark      Run performance benchmarks\n" +
               "  compare        Compare before/after changes\n\n" +
               "Examples:\n" +
               "  @perf analyze\n" +
               "  @perf suggest\n" +
               "  @perf benchmark";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args == null || args.trim().isEmpty()) {
            return AnsiColors.colorize("Error: Command required", AnsiColors.RED) + "\n" +
                   AnsiColors.colorize("Usage: " + getUsage(), AnsiColors.YELLOW);
        }

        String command = args.trim().split("\\s+")[0];

        return switch (command) {
            case "analyze" -> analyzePerformance(context);
            case "suggest" -> getSuggestions(context);
            case "benchmark" -> runBenchmarks(context);
            case "compare" -> comparePerformance(context);
            default -> AnsiColors.colorize("Error: Unknown command: " + command, AnsiColors.RED) + "\n" +
                       AnsiColors.colorize("Available: analyze, suggest, benchmark, compare", AnsiColors.YELLOW);
        };
    }

    private String analyzePerformance(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🔍 Analyzing code for performance issues...", AnsiColors.CYAN));

        try {
            List<PerformanceIssue> issues = findPerformanceIssues(context);

            if (issues.isEmpty()) {
                return AnsiColors.colorize("\n✅ No obvious performance issues detected!", AnsiColors.GREEN);
            }

            StringBuilder result = new StringBuilder();
            result.append(AnsiColors.colorize("\n⚠️  Found " + issues.size() + " performance issues:\n\n", AnsiColors.YELLOW));

            for (PerformanceIssue issue : issues) {
                result.append(AnsiColors.colorize("  " + issue.severity + " ", AnsiColors.RED));
                result.append(AnsiColors.colorize(issue.file + ":" + issue.line, AnsiColors.CYAN));
                result.append("\n");
                result.append(AnsiColors.colorize("    " + issue.description + "\n", AnsiColors.WHITE));
                result.append(AnsiColors.colorize("    → " + issue.suggestion + "\n\n", AnsiColors.BRIGHT_BLACK));
            }

            result.append(AnsiColors.colorize("Use: @perf suggest - for detailed optimization advice\n", AnsiColors.BRIGHT_BLACK));

            return result.toString();

        } catch (IOException e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    private String getSuggestions(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🤖 Generating optimization suggestions...", AnsiColors.CYAN));

        try {
            List<PerformanceIssue> issues = findPerformanceIssues(context);

            if (issues.isEmpty()) {
                return AnsiColors.colorize("\n✅ No issues to optimize!", AnsiColors.GREEN);
            }

            // Use LLM for detailed suggestions
            StringBuilder prompt = new StringBuilder();
            prompt.append("Analyze these performance issues and provide specific optimization recommendations:\n\n");

            for (PerformanceIssue issue : issues.stream().limit(5).toList()) {
                prompt.append("- ").append(issue.description).append(" in ").append(issue.file).append("\n");
            }

            prompt.append("\nProvide:\n");
            prompt.append("1. Specific code improvements\n");
            prompt.append("2. Best practices to follow\n");
            prompt.append("3. Expected performance impact\n");

            String suggestions = cliService.query(prompt.toString());

            return AnsiColors.colorize("\n💡 Optimization Suggestions:\n\n", AnsiColors.CYAN) +
                   suggestions + "\n";

        } catch (Exception e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    private String runBenchmarks(ProjectContext context) {
        return AnsiColors.colorize("\n⚡ Performance Benchmarks:\n\n", AnsiColors.CYAN) +
               AnsiColors.colorize("To run benchmarks:\n\n", AnsiColors.WHITE) +
               AnsiColors.colorize("Java (JMH):\n", AnsiColors.YELLOW) +
               AnsiColors.colorize("  mvn clean install\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  java -jar target/benchmarks.jar\n\n", AnsiColors.WHITE) +
               AnsiColors.colorize("Node.js (Benchmark.js):\n", AnsiColors.YELLOW) +
               AnsiColors.colorize("  npm install --save-dev benchmark\n\n", AnsiColors.WHITE) +
               AnsiColors.colorize("Python (pytest-benchmark):\n", AnsiColors.YELLOW) +
               AnsiColors.colorize("  pip install pytest-benchmark\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  pytest --benchmark-only\n\n", AnsiColors.WHITE) +
               AnsiColors.colorize("⚠️  Automated benchmarking coming soon!\n", AnsiColors.BRIGHT_BLACK);
    }

    private String comparePerformance(ProjectContext context) {
        return AnsiColors.colorize("\n📊 Performance Comparison:\n\n", AnsiColors.CYAN) +
               AnsiColors.colorize("Feature coming soon!\n\n", AnsiColors.YELLOW) +
               AnsiColors.colorize("Will compare:\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  • Execution time\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  • Memory usage\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  • CPU utilization\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  • Before/after metrics\n", AnsiColors.WHITE);
    }

    private List<PerformanceIssue> findPerformanceIssues(ProjectContext context) throws IOException {
        List<PerformanceIssue> issues = new ArrayList<>();

        // Common performance anti-patterns
        Pattern[] patterns = {
            Pattern.compile("String\\s+\\w+\\s*=\\s*\"\"\\s*;.*\\+\\s*"),  // String concatenation in loop
            Pattern.compile("new\\s+\\w+\\(\\).*\\{"),  // Object creation in loop
            Pattern.compile("\\.size\\(\\).*for\\s*\\("),  // size() called in loop condition
            Pattern.compile("System\\.out\\.println"),  // Logging in production
            Pattern.compile("Thread\\.sleep"),  // Thread.sleep usage
            Pattern.compile("\\.toString\\(\\)\\s*\\+"),  // Unnecessary toString
        };

        String[] descriptions = {
            "String concatenation in loop (use StringBuilder)",
            "Object creation in loop (move outside or use flyweight)",
            "size() called in loop condition (cache the size)",
            "System.out.println in code (use proper logging)",
            "Thread.sleep usage (consider better alternatives)",
            "Unnecessary toString() in concatenation"
        };

        String[] suggestions = {
            "Use StringBuilder for string building in loops",
            "Create objects outside loops when possible",
            "Cache collection.size() before the loop",
            "Replace with logger.debug/info",
            "Use CompletableFuture or reactive patterns",
            "Remove .toString() - implicit in string concatenation"
        };

        // Scan Java files
        Files.walk(context.getRootPath())
            .filter(p -> p.toString().endsWith(".java"))
            .filter(p -> !p.toString().contains("/target/"))
            .filter(p -> !p.toString().contains("/build/"))
            .forEach(file -> {
                try {
                    String content = Files.readString(file);
                    String[] lines = content.split("\n");

                    for (int i = 0; i < lines.length; i++) {
                        for (int p = 0; p < patterns.length; p++) {
                            Matcher matcher = patterns[p].matcher(lines[i]);
                            if (matcher.find()) {
                                issues.add(new PerformanceIssue(
                                    "⚠️",
                                    context.getRootPath().relativize(file).toString(),
                                    i + 1,
                                    descriptions[p],
                                    suggestions[p]
                                ));
                            }
                        }
                    }
                } catch (IOException e) {
                    // Skip files that can't be read
                }
            });

        return issues;
    }

    private static class PerformanceIssue {
        String severity;
        String file;
        int line;
        String description;
        String suggestion;

        PerformanceIssue(String severity, String file, int line, String description, String suggestion) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
            this.suggestion = suggestion;
        }
    }
}
