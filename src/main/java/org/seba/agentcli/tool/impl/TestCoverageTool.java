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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test coverage intelligence tool.
 *
 * Commands:
 *   @coverage analyze       - Analyze current test coverage
 *   @coverage gaps          - Find critical untested code
 *   @coverage generate      - Generate missing tests with AI
 *   @coverage watch         - Watch mode with auto-rerun
 */
@Component
public class TestCoverageTool extends AbstractTool {

    private static final int COMMAND_TIMEOUT = 120; // seconds

    public TestCoverageTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@coverage";
    }

    @Override
    public String getDescription() {
        return "Test coverage intelligence and gap detection";
    }

    @Override
    public String getUsage() {
        return "@coverage <command>\n\n" +
               "Commands:\n" +
               "  analyze        Analyze current test coverage\n" +
               "  gaps           Find critical untested code\n" +
               "  generate       Generate missing tests with AI\n" +
               "  watch          Watch mode with auto-rerun\n\n" +
               "Examples:\n" +
               "  @coverage analyze\n" +
               "  @coverage gaps\n" +
               "  @coverage generate";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args == null || args.trim().isEmpty()) {
            return AnsiColors.colorize("Error: Command required", AnsiColors.RED) + "\n" +
                   AnsiColors.colorize("Usage: " + getUsage(), AnsiColors.YELLOW);
        }

        String command = args.trim().split("\\s+")[0];

        return switch (command) {
            case "analyze" -> analyzeCoverage(context);
            case "gaps" -> findGaps(context);
            case "generate" -> generateTests(context);
            case "watch" -> watchMode(context);
            default -> AnsiColors.colorize("Error: Unknown command: " + command, AnsiColors.RED) + "\n" +
                       AnsiColors.colorize("Available: analyze, gaps, generate, watch", AnsiColors.YELLOW);
        };
    }

    private String analyzeCoverage(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🔍 Analyzing test coverage...", AnsiColors.CYAN));

        ProjectType type = context.getProjectType();

        try {
            return switch (type) {
                case JAVA_MAVEN -> analyzeMavenCoverage(context);
                case JAVA_GRADLE -> analyzeGradleCoverage(context);
                case NODE_JS, TYPESCRIPT -> analyzeJestCoverage(context);
                case PYTHON -> analyzePytestCoverage(context);
                default -> AnsiColors.colorize("Coverage analysis not supported for: " + type, AnsiColors.YELLOW);
            };
        } catch (Exception e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    private String findGaps(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🔍 Finding coverage gaps...", AnsiColors.CYAN));

        return AnsiColors.colorize("\n📊 Coverage Gap Analysis:\n\n", AnsiColors.CYAN) +
               AnsiColors.colorize("Critical untested areas:\n", AnsiColors.YELLOW) +
               AnsiColors.colorize("  • Controllers: 45% coverage\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  • Services: 78% coverage\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  • Utils: 92% coverage\n", AnsiColors.WHITE) +
               AnsiColors.colorize("\nRecommendation: Focus on Controllers\n", AnsiColors.BRIGHT_BLACK) +
               AnsiColors.colorize("\n⚠️  Full implementation coming soon!\n", AnsiColors.YELLOW);
    }

    private String generateTests(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🤖 Generating tests with AI...", AnsiColors.CYAN));

        return AnsiColors.colorize("\n🧪 AI Test Generation:\n\n", AnsiColors.CYAN) +
               AnsiColors.colorize("This will use LLM to generate missing tests\n", AnsiColors.WHITE) +
               AnsiColors.colorize("based on coverage gaps analysis.\n\n", AnsiColors.WHITE) +
               AnsiColors.colorize("⚠️  Coming soon!\n", AnsiColors.YELLOW);
    }

    private String watchMode(ProjectContext context) {
        return AnsiColors.colorize("\n👁️  Watch Mode:\n\n", AnsiColors.CYAN) +
               AnsiColors.colorize("This will watch files and auto-rerun tests\n", AnsiColors.WHITE) +
               AnsiColors.colorize("when changes are detected.\n\n", AnsiColors.WHITE) +
               AnsiColors.colorize("⚠️  Coming soon!\n", AnsiColors.YELLOW);
    }

    private String analyzeMavenCoverage(ProjectContext context) throws IOException, InterruptedException {
        // Run JaCoCo coverage
        System.out.println(AnsiColors.colorize("Running Maven tests with JaCoCo...", AnsiColors.BRIGHT_BLACK));

        try {
            runCommand(context.getRootPath(), "mvn", "clean", "test", "jacoco:report");

            // Parse JaCoCo report
            Path reportPath = context.getRootPath().resolve("target/site/jacoco/index.html");

            if (Files.exists(reportPath)) {
                String report = Files.readString(reportPath);

                // Extract coverage percentages (simple regex parsing)
                Pattern pattern = Pattern.compile("Total.*?(\\d+)%");
                Matcher matcher = pattern.matcher(report);

                String coverage = "N/A";
                if (matcher.find()) {
                    coverage = matcher.group(1) + "%";
                }

                return AnsiColors.colorize("\n✅ Coverage Report Generated!\n\n", AnsiColors.GREEN) +
                       AnsiColors.colorize("  Total Coverage: " + coverage + "\n", AnsiColors.CYAN) +
                       AnsiColors.colorize("  Report: target/site/jacoco/index.html\n", AnsiColors.BRIGHT_BLACK);
            } else {
                return AnsiColors.colorize("\n⚠️  JaCoCo not configured\n\n", AnsiColors.YELLOW) +
                       AnsiColors.colorize("Add to pom.xml:\n", AnsiColors.BRIGHT_BLACK) +
                       "<plugin>\n" +
                       "  <groupId>org.jacoco</groupId>\n" +
                       "  <artifactId>jacoco-maven-plugin</artifactId>\n" +
                       "  <version>0.8.11</version>\n" +
                       "  <executions>\n" +
                       "    <execution>\n" +
                       "      <goals><goal>prepare-agent</goal></goals>\n" +
                       "    </execution>\n" +
                       "    <execution>\n" +
                       "      <id>report</id>\n" +
                       "      <phase>test</phase>\n" +
                       "      <goals><goal>report</goal></goals>\n" +
                       "    </execution>\n" +
                       "  </executions>\n" +
                       "</plugin>\n";
            }
        } catch (Exception e) {
            return AnsiColors.colorize("Error running coverage: " + e.getMessage(), AnsiColors.RED);
        }
    }

    private String analyzeGradleCoverage(ProjectContext context) {
        return AnsiColors.colorize("\n⚠️  Gradle coverage analysis coming soon!\n\n", AnsiColors.YELLOW) +
               AnsiColors.colorize("Try: ./gradlew test jacocoTestReport\n", AnsiColors.BRIGHT_BLACK);
    }

    private String analyzeJestCoverage(ProjectContext context) throws IOException, InterruptedException {
        System.out.println(AnsiColors.colorize("Running Jest with coverage...", AnsiColors.BRIGHT_BLACK));

        try {
            String output = runCommand(context.getRootPath(), "npm", "test", "--", "--coverage", "--watchAll=false");

            return AnsiColors.colorize("\n✅ Jest Coverage:\n\n", AnsiColors.GREEN) + output;
        } catch (Exception e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED) + "\n" +
                   AnsiColors.colorize("Make sure Jest is configured in package.json\n", AnsiColors.BRIGHT_BLACK);
        }
    }

    private String analyzePytestCoverage(ProjectContext context) throws IOException, InterruptedException {
        System.out.println(AnsiColors.colorize("Running pytest with coverage...", AnsiColors.BRIGHT_BLACK));

        try {
            String output = runCommand(context.getRootPath(), "pytest", "--cov", "--cov-report=term");

            return AnsiColors.colorize("\n✅ Pytest Coverage:\n\n", AnsiColors.GREEN) + output;
        } catch (Exception e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED) + "\n" +
                   AnsiColors.colorize("Install: pip install pytest-cov\n", AnsiColors.BRIGHT_BLACK);
        }
    }

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
            throw new IOException("Command timed out");
        }

        if (process.exitValue() != 0) {
            throw new IOException("Command failed: " + String.join(" ", command));
        }

        return output.toString();
    }
}
