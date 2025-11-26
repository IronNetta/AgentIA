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
 * Security scanner for common vulnerabilities.
 *
 * Commands:
 *   @security scan         - Full security scan
 *   @security secrets      - Detect exposed secrets
 *   @security deps         - Check dependency vulnerabilities
 *   @security owasp        - OWASP Top 10 checks
 */
@Component
public class SecurityScannerTool extends AbstractTool {

    public SecurityScannerTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@security";
    }

    @Override
    public String getDescription() {
        return "Security scanner for vulnerabilities and secrets";
    }

    @Override
    public String getUsage() {
        return "@security <command>\n\n" +
               "Commands:\n" +
               "  scan           Full security scan\n" +
               "  secrets        Detect exposed secrets/credentials\n" +
               "  deps           Check dependency vulnerabilities\n" +
               "  owasp          OWASP Top 10 checks\n\n" +
               "Examples:\n" +
               "  @security scan\n" +
               "  @security secrets\n" +
               "  @security owasp";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args == null || args.trim().isEmpty()) {
            return AnsiColors.colorize("Error: Command required", AnsiColors.RED) + "\n" +
                   AnsiColors.colorize("Usage: " + getUsage(), AnsiColors.YELLOW);
        }

        String command = args.trim().split("\\s+")[0];

        return switch (command) {
            case "scan" -> fullScan(context);
            case "secrets" -> scanSecrets(context);
            case "deps" -> scanDependencies(context);
            case "owasp" -> owaspCheck(context);
            default -> AnsiColors.colorize("Error: Unknown command: " + command, AnsiColors.RED) + "\n" +
                       AnsiColors.colorize("Available: scan, secrets, deps, owasp", AnsiColors.YELLOW);
        };
    }

    private String fullScan(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🔒 Running full security scan...", AnsiColors.CYAN));

        StringBuilder result = new StringBuilder();
        result.append(AnsiColors.colorize("\n🔒 Security Scan Report:\n\n", AnsiColors.CYAN));

        // 1. Secrets scan
        try {
            List<SecurityIssue> secrets = findSecrets(context);
            result.append(AnsiColors.colorize("1. Secrets/Credentials: ", AnsiColors.YELLOW));
            if (secrets.isEmpty()) {
                result.append(AnsiColors.colorize("✅ None found\n", AnsiColors.GREEN));
            } else {
                result.append(AnsiColors.colorize("❌ " + secrets.size() + " found\n", AnsiColors.RED));
            }

            // 2. SQL Injection
            List<SecurityIssue> sqlInjection = findSqlInjection(context);
            result.append(AnsiColors.colorize("2. SQL Injection risks: ", AnsiColors.YELLOW));
            if (sqlInjection.isEmpty()) {
                result.append(AnsiColors.colorize("✅ None found\n", AnsiColors.GREEN));
            } else {
                result.append(AnsiColors.colorize("⚠️  " + sqlInjection.size() + " potential\n", AnsiColors.YELLOW));
            }

            // 3. XSS
            List<SecurityIssue> xss = findXss(context);
            result.append(AnsiColors.colorize("3. XSS vulnerabilities: ", AnsiColors.YELLOW));
            if (xss.isEmpty()) {
                result.append(AnsiColors.colorize("✅ None found\n", AnsiColors.GREEN));
            } else {
                result.append(AnsiColors.colorize("⚠️  " + xss.size() + " potential\n", AnsiColors.YELLOW));
            }

            // 4. Insecure deserialization
            List<SecurityIssue> deserialization = findDeserialization(context);
            result.append(AnsiColors.colorize("4. Deserialization issues: ", AnsiColors.YELLOW));
            if (deserialization.isEmpty()) {
                result.append(AnsiColors.colorize("✅ None found\n", AnsiColors.GREEN));
            } else {
                result.append(AnsiColors.colorize("⚠️  " + deserialization.size() + " potential\n", AnsiColors.YELLOW));
            }

            // Summary
            int totalIssues = secrets.size() + sqlInjection.size() + xss.size() + deserialization.size();

            result.append("\n");
            if (totalIssues == 0) {
                result.append(AnsiColors.colorize("✅ No security issues found!\n", AnsiColors.BOLD_GREEN));
            } else {
                result.append(AnsiColors.colorize("⚠️  Total issues: " + totalIssues + "\n", AnsiColors.YELLOW));
                result.append(AnsiColors.colorize("\nRun specific scans for details:\n", AnsiColors.BRIGHT_BLACK));
                result.append(AnsiColors.colorize("  @security secrets\n", AnsiColors.WHITE));
                result.append(AnsiColors.colorize("  @security owasp\n", AnsiColors.WHITE));
            }

        } catch (IOException e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }

        return result.toString();
    }

    private String scanSecrets(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🔍 Scanning for exposed secrets...", AnsiColors.CYAN));

        try {
            List<SecurityIssue> secrets = findSecrets(context);

            if (secrets.isEmpty()) {
                return AnsiColors.colorize("\n✅ No exposed secrets found!", AnsiColors.GREEN);
            }

            StringBuilder result = new StringBuilder();
            result.append(AnsiColors.colorize("\n❌ Found " + secrets.size() + " potential secrets:\n\n", AnsiColors.RED));

            for (SecurityIssue issue : secrets) {
                result.append(AnsiColors.colorize("  🚨 " + issue.file + ":" + issue.line + "\n", AnsiColors.RED));
                result.append(AnsiColors.colorize("     " + issue.description + "\n", AnsiColors.WHITE));
                result.append(AnsiColors.colorize("     → " + issue.suggestion + "\n\n", AnsiColors.BRIGHT_BLACK));
            }

            result.append(AnsiColors.colorize("⚠️  CRITICAL: Remove these secrets immediately!\n", AnsiColors.BOLD_RED));
            result.append(AnsiColors.colorize("Use environment variables or secret management tools.\n", AnsiColors.BRIGHT_BLACK));

            return result.toString();

        } catch (IOException e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    private String scanDependencies(ProjectContext context) {
        return AnsiColors.colorize("\n🔒 Dependency Security Scan:\n\n", AnsiColors.CYAN) +
               AnsiColors.colorize("Use dedicated tools:\n\n", AnsiColors.WHITE) +
               AnsiColors.colorize("Maven:\n", AnsiColors.YELLOW) +
               AnsiColors.colorize("  @deps security\n\n", AnsiColors.WHITE) +
               AnsiColors.colorize("npm:\n", AnsiColors.YELLOW) +
               AnsiColors.colorize("  npm audit\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  npm audit fix\n\n", AnsiColors.WHITE) +
               AnsiColors.colorize("Python:\n", AnsiColors.YELLOW) +
               AnsiColors.colorize("  pip-audit\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  safety check\n\n", AnsiColors.WHITE) +
               AnsiColors.colorize("Or use: @deps security\n", AnsiColors.BRIGHT_BLACK);
    }

    private String owaspCheck(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🔍 Running OWASP Top 10 checks...", AnsiColors.CYAN));

        StringBuilder result = new StringBuilder();
        result.append(AnsiColors.colorize("\n🔒 OWASP Top 10 Check:\n\n", AnsiColors.CYAN));

        try {
            // Check for various OWASP issues
            List<SecurityIssue> allIssues = new ArrayList<>();
            allIssues.addAll(findSqlInjection(context));
            allIssues.addAll(findXss(context));
            allIssues.addAll(findDeserialization(context));
            allIssues.addAll(findCommandInjection(context));
            allIssues.addAll(findPathTraversal(context));

            if (allIssues.isEmpty()) {
                result.append(AnsiColors.colorize("✅ No OWASP vulnerabilities detected!\n", AnsiColors.GREEN));
            } else {
                result.append(AnsiColors.colorize("⚠️  Found " + allIssues.size() + " potential issues:\n\n", AnsiColors.YELLOW));

                for (SecurityIssue issue : allIssues.stream().limit(10).toList()) {
                    result.append(AnsiColors.colorize("  • " + issue.severity + " ", issue.severity.equals("HIGH") ? AnsiColors.RED : AnsiColors.YELLOW));
                    result.append(AnsiColors.colorize(issue.description + "\n", AnsiColors.WHITE));
                    result.append(AnsiColors.colorize("    " + issue.file + ":" + issue.line + "\n", AnsiColors.BRIGHT_BLACK));
                    result.append("\n");
                }

                if (allIssues.size() > 10) {
                    result.append(AnsiColors.colorize("  ... and " + (allIssues.size() - 10) + " more\n", AnsiColors.BRIGHT_BLACK));
                }
            }

        } catch (IOException e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }

        return result.toString();
    }

    private List<SecurityIssue> findSecrets(ProjectContext context) throws IOException {
        List<SecurityIssue> issues = new ArrayList<>();

        Pattern[] patterns = {
            Pattern.compile("(?i)(password|passwd|pwd)\\s*=\\s*[\"']([^\"']+)[\"']"),
            Pattern.compile("(?i)(api[_-]?key|apikey)\\s*=\\s*[\"']([^\"']+)[\"']"),
            Pattern.compile("(?i)(secret|token)\\s*=\\s*[\"']([^\"']+)[\"']"),
            Pattern.compile("(?i)(access[_-]?key)\\s*=\\s*[\"']([^\"']+)[\"']"),
        };

        scanFiles(context, patterns, issues, "Hardcoded secret detected", "Use environment variables or secret vault");

        return issues;
    }

    private List<SecurityIssue> findSqlInjection(ProjectContext context) throws IOException {
        List<SecurityIssue> issues = new ArrayList<>();

        Pattern[] patterns = {
            Pattern.compile("\"SELECT .* \\+"),
            Pattern.compile("executeQuery\\([^?]*\\+"),
            Pattern.compile("createQuery\\([^?]*\\+"),
        };

        scanFiles(context, patterns, issues, "Potential SQL injection", "Use PreparedStatement or parameterized queries");

        return issues;
    }

    private List<SecurityIssue> findXss(ProjectContext context) throws IOException {
        List<SecurityIssue> issues = new ArrayList<>();

        Pattern[] patterns = {
            Pattern.compile("\\.innerHTML\\s*="),
            Pattern.compile("document\\.write\\("),
            Pattern.compile("eval\\("),
        };

        scanFiles(context, patterns, issues, "Potential XSS vulnerability", "Sanitize user input and use safe DOM methods");

        return issues;
    }

    private List<SecurityIssue> findDeserialization(ProjectContext context) throws IOException {
        List<SecurityIssue> issues = new ArrayList<>();

        Pattern[] patterns = {
            Pattern.compile("ObjectInputStream"),
            Pattern.compile("readObject\\("),
            Pattern.compile("XMLDecoder"),
        };

        scanFiles(context, patterns, issues, "Insecure deserialization", "Validate and whitelist deserialized classes");

        return issues;
    }

    private List<SecurityIssue> findCommandInjection(ProjectContext context) throws IOException {
        List<SecurityIssue> issues = new ArrayList<>();

        Pattern[] patterns = {
            Pattern.compile("Runtime\\.getRuntime\\(\\)\\.exec"),
            Pattern.compile("ProcessBuilder.*\\+"),
        };

        scanFiles(context, patterns, issues, "Command injection risk", "Validate and sanitize all external inputs");

        return issues;
    }

    private List<SecurityIssue> findPathTraversal(ProjectContext context) throws IOException {
        List<SecurityIssue> issues = new ArrayList<>();

        Pattern[] patterns = {
            Pattern.compile("new File\\([^)]*\\+"),
            Pattern.compile("Paths\\.get\\([^)]*\\+"),
        };

        scanFiles(context, patterns, issues, "Path traversal risk", "Validate file paths and use canonical paths");

        return issues;
    }

    private void scanFiles(ProjectContext context, Pattern[] patterns, List<SecurityIssue> issues,
                          String description, String suggestion) throws IOException {
        Files.walk(context.getRootPath())
            .filter(p -> p.toString().matches(".*\\.(java|js|ts|py)$"))
            .filter(p -> !p.toString().contains("/target/"))
            .filter(p -> !p.toString().contains("/node_modules/"))
            .filter(p -> !p.toString().contains("/build/"))
            .forEach(file -> {
                try {
                    String content = Files.readString(file);
                    String[] lines = content.split("\n");

                    for (int i = 0; i < lines.length; i++) {
                        for (Pattern pattern : patterns) {
                            Matcher matcher = pattern.matcher(lines[i]);
                            if (matcher.find()) {
                                issues.add(new SecurityIssue(
                                    "HIGH",
                                    context.getRootPath().relativize(file).toString(),
                                    i + 1,
                                    description,
                                    suggestion
                                ));
                            }
                        }
                    }
                } catch (IOException e) {
                    // Skip files that can't be read
                }
            });
    }

    private static class SecurityIssue {
        String severity;
        String file;
        int line;
        String description;
        String suggestion;

        SecurityIssue(String severity, String file, int line, String description, String suggestion) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
            this.suggestion = suggestion;
        }
    }
}
