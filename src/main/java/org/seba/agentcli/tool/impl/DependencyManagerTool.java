package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.model.ProjectType;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Smart dependency manager supporting multiple build tools.
 *
 * Commands:
 *   @deps check              - Analyze current dependencies
 *   @deps outdated           - Find outdated dependencies
 *   @deps update <dep>       - Update specific dependency
 *   @deps security           - Security vulnerability scan
 *   @deps unused             - Detect unused dependencies
 */
@Component
public class DependencyManagerTool extends AbstractTool {

    private static final int COMMAND_TIMEOUT = 60; // seconds

    public DependencyManagerTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@deps";
    }

    @Override
    public String getDescription() {
        return "Smart dependency manager with security scanning";
    }

    @Override
    public String getUsage() {
        return "@deps <command> [args]\n\n" +
               "Commands:\n" +
               "  check              Analyze current dependencies\n" +
               "  outdated           Find outdated dependencies\n" +
               "  update <name>      Update specific dependency (with tests)\n" +
               "  security           Scan for security vulnerabilities\n" +
               "  unused             Detect unused dependencies\n\n" +
               "Examples:\n" +
               "  @deps check\n" +
               "  @deps outdated\n" +
               "  @deps update spring-boot\n" +
               "  @deps security";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args == null || args.trim().isEmpty()) {
            return AnsiColors.colorize("Error: Command required", AnsiColors.RED) + "\n" +
                   AnsiColors.colorize("Usage: " + getUsage(), AnsiColors.YELLOW);
        }

        String[] parts = args.trim().split("\\s+", 2);
        String command = parts[0];
        String commandArgs = parts.length > 1 ? parts[1] : "";

        return switch (command) {
            case "check" -> checkDependencies(context);
            case "outdated" -> findOutdated(context);
            case "update" -> updateDependency(commandArgs, context);
            case "security" -> securityScan(context);
            case "unused" -> findUnused(context);
            default -> AnsiColors.colorize("Error: Unknown command: " + command, AnsiColors.RED) + "\n" +
                       AnsiColors.colorize("Available: check, outdated, update, security, unused", AnsiColors.YELLOW);
        };
    }

    /**
     * Analyze current dependencies
     */
    private String checkDependencies(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🔍 Analyzing dependencies...", AnsiColors.CYAN));

        ProjectType type = context.getProjectType();

        try {
            return switch (type) {
                case JAVA_MAVEN -> checkMavenDependencies(context);
                case JAVA_GRADLE -> checkGradleDependencies(context);
                case NODE_JS, TYPESCRIPT -> checkNpmDependencies(context);
                case PYTHON -> checkPipDependencies(context);
                default -> AnsiColors.colorize("Dependency management not supported for: " + type, AnsiColors.YELLOW);
            };
        } catch (Exception e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    /**
     * Find outdated dependencies
     */
    private String findOutdated(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🔍 Checking for outdated dependencies...", AnsiColors.CYAN));

        ProjectType type = context.getProjectType();

        try {
            return switch (type) {
                case JAVA_MAVEN -> findOutdatedMaven(context);
                case JAVA_GRADLE -> findOutdatedGradle(context);
                case NODE_JS, TYPESCRIPT -> findOutdatedNpm(context);
                case PYTHON -> findOutdatedPip(context);
                default -> AnsiColors.colorize("Not supported for: " + type, AnsiColors.YELLOW);
            };
        } catch (Exception e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    /**
     * Update a specific dependency
     */
    private String updateDependency(String depName, ProjectContext context) {
        if (depName == null || depName.isEmpty()) {
            return AnsiColors.colorize("Error: Dependency name required", AnsiColors.RED) + "\n" +
                   AnsiColors.colorize("Usage: @deps update <dependency-name>", AnsiColors.YELLOW);
        }

        System.out.println(AnsiColors.colorize("🔄 Updating dependency: " + depName, AnsiColors.CYAN));

        return AnsiColors.colorize("Automatic dependency update coming soon!", AnsiColors.YELLOW) + "\n" +
               AnsiColors.colorize("For now, update manually and run: @execute test", AnsiColors.BRIGHT_BLACK);
    }

    /**
     * Security vulnerability scan
     */
    private String securityScan(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🔒 Running security scan...", AnsiColors.CYAN));

        ProjectType type = context.getProjectType();

        try {
            return switch (type) {
                case JAVA_MAVEN -> securityScanMaven(context);
                case JAVA_GRADLE -> securityScanGradle(context);
                case NODE_JS, TYPESCRIPT -> securityScanNpm(context);
                case PYTHON -> securityScanPip(context);
                default -> AnsiColors.colorize("Security scan not supported for: " + type, AnsiColors.YELLOW);
            };
        } catch (Exception e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    /**
     * Find unused dependencies
     */
    private String findUnused(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🔍 Analyzing for unused dependencies...", AnsiColors.CYAN));

        ProjectType type = context.getProjectType();

        try {
            return switch (type) {
                case JAVA_MAVEN -> findUnusedMaven(context);
                case JAVA_GRADLE -> findUnusedGradle(context);
                case NODE_JS, TYPESCRIPT -> findUnusedNpm(context);
                default -> AnsiColors.colorize("Unused detection not supported for: " + type, AnsiColors.YELLOW);
            };
        } catch (Exception e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    // ==================== MAVEN ====================

    private String checkMavenDependencies(ProjectContext context) throws IOException, InterruptedException {
        String output = runCommand(context.getRootPath(), "mvn", "dependency:list");

        // Parse dependencies
        List<String> deps = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[INFO\\]\\s+([^:]+:[^:]+:[^:]+:[^:]+:[^:]+)");
        Matcher matcher = pattern.matcher(output);

        while (matcher.find()) {
            deps.add(matcher.group(1));
        }

        StringBuilder result = new StringBuilder();
        result.append(AnsiColors.colorize("\n📦 Maven Dependencies (" + deps.size() + " total):\n\n", AnsiColors.GREEN));

        // Group by groupId
        Map<String, List<String>> grouped = new HashMap<>();
        for (String dep : deps) {
            String[] parts = dep.split(":");
            if (parts.length >= 2) {
                String groupId = parts[0];
                grouped.computeIfAbsent(groupId, k -> new ArrayList<>()).add(dep);
            }
        }

        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            result.append(AnsiColors.colorize("  " + entry.getKey() + ":\n", AnsiColors.CYAN));
            for (String dep : entry.getValue().stream().limit(5).toList()) {
                String[] parts = dep.split(":");
                if (parts.length >= 4) {
                    result.append(AnsiColors.colorize("    • " + parts[1] + " ", AnsiColors.WHITE));
                    result.append(AnsiColors.colorize("(" + parts[3] + ")\n", AnsiColors.BRIGHT_BLACK));
                }
            }
            if (entry.getValue().size() > 5) {
                result.append(AnsiColors.colorize("    ... +" + (entry.getValue().size() - 5) + " more\n", AnsiColors.BRIGHT_BLACK));
            }
        }

        return result.toString();
    }

    private String findOutdatedMaven(ProjectContext context) throws IOException, InterruptedException {
        String output = runCommand(context.getRootPath(), "mvn", "versions:display-dependency-updates");

        // Parse output for outdated dependencies
        List<String> outdated = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[INFO\\]\\s+([^\\s]+\\s+\\.+\\s+[^\\s]+\\s+->\\s+[^\\s]+)");
        Matcher matcher = pattern.matcher(output);

        while (matcher.find()) {
            outdated.add(matcher.group(1));
        }

        if (outdated.isEmpty()) {
            return AnsiColors.colorize("\n✅ All dependencies are up to date!", AnsiColors.GREEN);
        }

        StringBuilder result = new StringBuilder();
        result.append(AnsiColors.colorize("\n⚠️  Found " + outdated.size() + " outdated dependencies:\n\n", AnsiColors.YELLOW));

        for (String dep : outdated) {
            result.append(AnsiColors.colorize("  • " + dep + "\n", AnsiColors.WHITE));
        }

        result.append(AnsiColors.colorize("\nTo update: @deps update <dependency-name>\n", AnsiColors.BRIGHT_BLACK));

        return result.toString();
    }

    private String securityScanMaven(ProjectContext context) throws IOException, InterruptedException {
        // Try to use OWASP Dependency Check plugin if available
        try {
            String output = runCommand(context.getRootPath(), "mvn", "org.owasp:dependency-check-maven:check");

            return AnsiColors.colorize("\n🔒 Security scan completed!\n", AnsiColors.GREEN) +
                   AnsiColors.colorize("Check target/dependency-check-report.html for details\n", AnsiColors.BRIGHT_BLACK);
        } catch (Exception e) {
            return AnsiColors.colorize("\n⚠️  OWASP Dependency Check not configured\n", AnsiColors.YELLOW) +
                   AnsiColors.colorize("Add to pom.xml:\n", AnsiColors.BRIGHT_BLACK) +
                   "<plugin>\n" +
                   "  <groupId>org.owasp</groupId>\n" +
                   "  <artifactId>dependency-check-maven</artifactId>\n" +
                   "  <version>8.4.0</version>\n" +
                   "</plugin>\n";
        }
    }

    private String findUnusedMaven(ProjectContext context) throws IOException, InterruptedException {
        String output = runCommand(context.getRootPath(), "mvn", "dependency:analyze");

        // Parse unused dependencies
        List<String> unused = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[WARNING\\] Unused declared dependencies found:.*?(?=\\[|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(output);

        if (matcher.find()) {
            String section = matcher.group();
            Pattern depPattern = Pattern.compile("\\[WARNING\\]\\s+([^:]+:[^:]+:[^:]+:[^:]+:[^:]+)");
            Matcher depMatcher = depPattern.matcher(section);

            while (depMatcher.find()) {
                unused.add(depMatcher.group(1));
            }
        }

        if (unused.isEmpty()) {
            return AnsiColors.colorize("\n✅ No unused dependencies found!", AnsiColors.GREEN);
        }

        StringBuilder result = new StringBuilder();
        result.append(AnsiColors.colorize("\n⚠️  Found " + unused.size() + " unused dependencies:\n\n", AnsiColors.YELLOW));

        for (String dep : unused) {
            String[] parts = dep.split(":");
            if (parts.length >= 2) {
                result.append(AnsiColors.colorize("  • " + parts[0] + ":" + parts[1] + "\n", AnsiColors.WHITE));
            }
        }

        result.append(AnsiColors.colorize("\nConsider removing these from pom.xml\n", AnsiColors.BRIGHT_BLACK));

        return result.toString();
    }

    // ==================== GRADLE ====================

    private String checkGradleDependencies(ProjectContext context) {
        try {
            System.out.println(AnsiColors.colorize("Running Gradle dependencies...", AnsiColors.BRIGHT_BLACK));
            String output = runCommand(context.getRootPath(), "./gradlew", "dependencies", "--configuration", "runtimeClasspath");

            // Parse dependencies
            List<String> deps = new ArrayList<>();
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.contains("---") && !line.contains("\\---")) {
                    String dep = line.replaceAll("^[^a-zA-Z]+", "").split(" ")[0];
                    if (dep.contains(":")) {
                        deps.add(dep);
                    }
                }
            }

            StringBuilder result = new StringBuilder();
            result.append(AnsiColors.colorize("\n📦 Gradle Dependencies (" + deps.size() + " total):\n\n", AnsiColors.GREEN));

            for (String dep : deps.stream().limit(20).toList()) {
                result.append(AnsiColors.colorize("  • " + dep + "\n", AnsiColors.WHITE));
            }

            if (deps.size() > 20) {
                result.append(AnsiColors.colorize("  ... +" + (deps.size() - 20) + " more\n", AnsiColors.BRIGHT_BLACK));
            }

            return result.toString();
        } catch (Exception e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED) + "\n" +
                   AnsiColors.colorize("Make sure Gradle wrapper exists: ./gradlew", AnsiColors.BRIGHT_BLACK);
        }
    }

    private String findOutdatedGradle(ProjectContext context) {
        try {
            System.out.println(AnsiColors.colorize("Checking for outdated dependencies...", AnsiColors.BRIGHT_BLACK));
            String output = runCommand(context.getRootPath(), "./gradlew", "dependencyUpdates");

            return AnsiColors.colorize("\n📊 Gradle Outdated Dependencies:\n\n", AnsiColors.CYAN) + output;
        } catch (Exception e) {
            return AnsiColors.colorize("\n⚠️  dependencyUpdates plugin not configured\n\n", AnsiColors.YELLOW) +
                   AnsiColors.colorize("Add to build.gradle:\n", AnsiColors.BRIGHT_BLACK) +
                   "plugins {\n" +
                   "  id 'com.github.ben-manes.versions' version '0.50.0'\n" +
                   "}\n\n" +
                   AnsiColors.colorize("Then run: ./gradlew dependencyUpdates\n", AnsiColors.BRIGHT_BLACK);
        }
    }

    private String securityScanGradle(ProjectContext context) {
        try {
            System.out.println(AnsiColors.colorize("Running security scan...", AnsiColors.BRIGHT_BLACK));
            String output = runCommand(context.getRootPath(), "./gradlew", "dependencyCheckAnalyze");

            return AnsiColors.colorize("\n🔒 Security Scan Complete!\n\n", AnsiColors.GREEN) +
                   AnsiColors.colorize("Check: build/reports/dependency-check-report.html\n", AnsiColors.BRIGHT_BLACK);
        } catch (Exception e) {
            return AnsiColors.colorize("\n⚠️  OWASP Dependency Check not configured\n\n", AnsiColors.YELLOW) +
                   AnsiColors.colorize("Add to build.gradle:\n", AnsiColors.BRIGHT_BLACK) +
                   "plugins {\n" +
                   "  id 'org.owasp.dependencycheck' version '9.0.0'\n" +
                   "}\n\n" +
                   AnsiColors.colorize("Alternative: Use Snyk or Trivy for scanning\n", AnsiColors.BRIGHT_BLACK);
        }
    }

    private String findUnusedGradle(ProjectContext context) {
        return AnsiColors.colorize("\n🔍 Gradle Unused Dependencies:\n\n", AnsiColors.CYAN) +
               AnsiColors.colorize("Use the Gradle Dependency Analyzer:\n\n", AnsiColors.WHITE) +
               AnsiColors.colorize("Add to build.gradle:\n", AnsiColors.BRIGHT_BLACK) +
               "plugins {\n" +
               "  id 'ca.cutterslade.analyze' version '1.9.2'\n" +
               "}\n\n" +
               AnsiColors.colorize("Then run: ./gradlew analyzeClassesDependencies\n", AnsiColors.BRIGHT_BLACK);
    }

    // ==================== NPM ====================

    private String checkNpmDependencies(ProjectContext context) throws IOException {
        Path packageJson = context.getRootPath().resolve("package.json");

        if (!Files.exists(packageJson)) {
            return AnsiColors.colorize("package.json not found", AnsiColors.RED);
        }

        String content = Files.readString(packageJson);

        // Simple JSON parsing (could use Jackson for more robust parsing)
        int depsCount = countOccurrences(content, "\"dependencies\"");
        int devDepsCount = countOccurrences(content, "\"devDependencies\"");

        StringBuilder result = new StringBuilder();
        result.append(AnsiColors.colorize("\n📦 NPM Dependencies:\n\n", AnsiColors.GREEN));
        result.append(AnsiColors.colorize("  • Dependencies: " + depsCount + "\n", AnsiColors.WHITE));
        result.append(AnsiColors.colorize("  • Dev Dependencies: " + devDepsCount + "\n", AnsiColors.WHITE));

        return result.toString();
    }

    private String findOutdatedNpm(ProjectContext context) throws IOException, InterruptedException {
        String output = runCommand(context.getRootPath(), "npm", "outdated");

        if (output.trim().isEmpty()) {
            return AnsiColors.colorize("\n✅ All dependencies are up to date!", AnsiColors.GREEN);
        }

        return AnsiColors.colorize("\n⚠️  Outdated npm dependencies:\n\n", AnsiColors.YELLOW) +
               output +
               AnsiColors.colorize("\nRun: npm update\n", AnsiColors.BRIGHT_BLACK);
    }

    private String securityScanNpm(ProjectContext context) throws IOException, InterruptedException {
        String output = runCommand(context.getRootPath(), "npm", "audit");

        return AnsiColors.colorize("\n🔒 NPM Security Audit:\n\n", AnsiColors.CYAN) + output;
    }

    private String findUnusedNpm(ProjectContext context) {
        return AnsiColors.colorize("NPM unused detection coming soon!", AnsiColors.YELLOW) + "\n" +
               AnsiColors.colorize("Try: npx depcheck", AnsiColors.BRIGHT_BLACK);
    }

    // ==================== PIP ====================

    private String checkPipDependencies(ProjectContext context) throws IOException {
        Path requirements = context.getRootPath().resolve("requirements.txt");

        if (!Files.exists(requirements)) {
            return AnsiColors.colorize("requirements.txt not found", AnsiColors.RED);
        }

        List<String> lines = Files.readAllLines(requirements);

        StringBuilder result = new StringBuilder();
        result.append(AnsiColors.colorize("\n📦 Python Dependencies (" + lines.size() + " total):\n\n", AnsiColors.GREEN));

        for (String line : lines.stream().limit(20).toList()) {
            if (!line.trim().isEmpty() && !line.startsWith("#")) {
                result.append(AnsiColors.colorize("  • " + line + "\n", AnsiColors.WHITE));
            }
        }

        if (lines.size() > 20) {
            result.append(AnsiColors.colorize("  ... +" + (lines.size() - 20) + " more\n", AnsiColors.BRIGHT_BLACK));
        }

        return result.toString();
    }

    private String findOutdatedPip(ProjectContext context) throws IOException, InterruptedException {
        String output = runCommand(context.getRootPath(), "pip", "list", "--outdated");

        if (output.trim().isEmpty()) {
            return AnsiColors.colorize("\n✅ All dependencies are up to date!", AnsiColors.GREEN);
        }

        return AnsiColors.colorize("\n⚠️  Outdated pip packages:\n\n", AnsiColors.YELLOW) + output;
    }

    private String securityScanPip(ProjectContext context) throws IOException, InterruptedException {
        try {
            String output = runCommand(context.getRootPath(), "pip-audit");
            return AnsiColors.colorize("\n🔒 Security Audit:\n\n", AnsiColors.CYAN) + output;
        } catch (Exception e) {
            return AnsiColors.colorize("\n⚠️  pip-audit not installed\n", AnsiColors.YELLOW) +
                   AnsiColors.colorize("Install: pip install pip-audit\n", AnsiColors.BRIGHT_BLACK);
        }
    }

    // ==================== UTILITIES ====================

    private String runCommand(Path workingDir, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(COMMAND_TIMEOUT, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Command timed out after " + COMMAND_TIMEOUT + " seconds");
        }

        if (process.exitValue() != 0) {
            throw new IOException("Command failed with exit code: " + process.exitValue());
        }

        return output.toString();
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
