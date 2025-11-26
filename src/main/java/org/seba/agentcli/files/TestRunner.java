package org.seba.agentcli.files;

import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.ConsoleReader;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.model.ProjectType;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Runs tests automatically after code modifications
 * Suggests appropriate test commands based on project type
 */
@Component
public class TestRunner {

    /**
     * Suggests running tests after a file modification
     * Returns true if tests should be run
     */
    public boolean suggestRunningTests(String modifiedFile, ProjectContext projectContext, ConsoleReader consoleReader) {
        // Only suggest for source files, not config files
        if (!isSourceFile(modifiedFile)) {
            return false;
        }

        System.out.println();
        System.out.println(AnsiColors.colorize("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", AnsiColors.YELLOW));
        System.out.println(AnsiColors.colorize("  CODE MODIFIED", AnsiColors.BOLD_YELLOW));
        System.out.println(AnsiColors.colorize("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", AnsiColors.YELLOW));
        System.out.println();
        System.out.println("A source file has been modified: " + modifiedFile);
        System.out.println();
        System.out.println("RECOMMENDATION:");
        System.out.println("  Verify that changes don't break existing functionality");
        System.out.println();
        System.out.println("Would you like me to:");
        System.out.println("  " + AnsiColors.colorize("a)", AnsiColors.GREEN) + " Run tests to verify changes");
        System.out.println("  " + AnsiColors.colorize("b)", AnsiColors.CYAN) + " Just compile/check syntax");
        System.out.println("  " + AnsiColors.colorize("c)", AnsiColors.YELLOW) + " Skip verification");
        System.out.println();

        String choice = consoleReader.readLine(AnsiColors.colorize("Your choice [a/b/c]: ", AnsiColors.BOLD_WHITE)).trim().toLowerCase();

        if (choice.equals("a") || choice.equals("")) {
            return runTests(projectContext);
        } else if (choice.equals("b")) {
            return compileOnly(projectContext);
        }

        return false;
    }

    /**
     * Runs tests based on project type
     */
    private boolean runTests(ProjectContext projectContext) {
        if (projectContext == null) {
            System.out.println(AnsiColors.warning("No project context available"));
            return false;
        }

        ProjectType projectType = projectContext.getProjectType();
        String command = getTestCommand(projectType);

        if (command == null) {
            System.out.println(AnsiColors.warning("Test command not configured for project type: " + projectType));
            return false;
        }

        System.out.println();
        System.out.println(AnsiColors.colorize("Running tests...", AnsiColors.CYAN));
        System.out.println(AnsiColors.colorize("Command: " + command, AnsiColors.BRIGHT_BLACK));
        System.out.println();

        return executeCommand(command, 60);
    }

    /**
     * Compiles/checks syntax only
     */
    private boolean compileOnly(ProjectContext projectContext) {
        if (projectContext == null) {
            System.out.println(AnsiColors.warning("No project context available"));
            return false;
        }

        ProjectType projectType = projectContext.getProjectType();
        String command = getCompileCommand(projectType);

        if (command == null) {
            System.out.println(AnsiColors.warning("Compile command not configured for project type: " + projectType));
            return false;
        }

        System.out.println();
        System.out.println(AnsiColors.colorize("Checking syntax...", AnsiColors.CYAN));
        System.out.println(AnsiColors.colorize("Command: " + command, AnsiColors.BRIGHT_BLACK));
        System.out.println();

        return executeCommand(command, 30);
    }

    /**
     * Returns test command based on project type
     */
    private String getTestCommand(ProjectType projectType) {
        switch (projectType) {
            case JAVA_MAVEN:
                if (Files.exists(Paths.get("mvnw"))) {
                    return "./mvnw test";
                }
                return "mvn test";
            case JAVA_GRADLE:
                if (Files.exists(Paths.get("gradlew"))) {
                    return "./gradlew test";
                }
                return "gradle test";
            case PYTHON:
                return "pytest";
            case NODE_JS:
            case TYPESCRIPT:
                if (hasScript("package.json", "test")) {
                    return "npm test";
                }
                return null;
            case GO:
                return "go test ./...";
            case RUST:
                return "cargo test";
            default:
                return null;
        }
    }

    /**
     * Returns compile command based on project type
     */
    private String getCompileCommand(ProjectType projectType) {
        switch (projectType) {
            case JAVA_MAVEN:
                if (Files.exists(Paths.get("mvnw"))) {
                    return "./mvnw compile";
                }
                return "mvn compile";
            case JAVA_GRADLE:
                if (Files.exists(Paths.get("gradlew"))) {
                    return "./gradlew compileJava";
                }
                return "gradle compileJava";
            case PYTHON:
                return "python -m py_compile";
            case NODE_JS:
            case TYPESCRIPT:
                return "npm run build";
            case GO:
                return "go build";
            case RUST:
                return "cargo check";
            default:
                return null;
        }
    }

    /**
     * Executes a command with timeout
     */
    private boolean executeCommand(String command, int timeoutSeconds) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int lineCount = 0;
            int maxLines = 50; // Limit output

            while ((line = reader.readLine()) != null && lineCount < maxLines) {
                System.out.println(line);
                lineCount++;
            }

            if (lineCount >= maxLines) {
                System.out.println(AnsiColors.colorize("... (output truncated)", AnsiColors.BRIGHT_BLACK));
            }

            // Wait for completion with timeout
            boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                System.out.println();
                System.out.println(AnsiColors.warning("Command timed out after " + timeoutSeconds + " seconds"));
                return false;
            }

            int exitCode = process.exitValue();
            System.out.println();

            if (exitCode == 0) {
                System.out.println(AnsiColors.success("✓ SUCCESS"));
                return true;
            } else {
                System.out.println(AnsiColors.error("✗ FAILED (exit code: " + exitCode + ")"));
                return false;
            }

        } catch (Exception e) {
            System.out.println(AnsiColors.error("Error executing command: " + e.getMessage()));
            return false;
        }
    }

    /**
     * Checks if file is a source file
     */
    private boolean isSourceFile(String filePath) {
        String lower = filePath.toLowerCase();
        return lower.endsWith(".java") ||
               lower.endsWith(".py") ||
               lower.endsWith(".js") ||
               lower.endsWith(".ts") ||
               lower.endsWith(".tsx") ||
               lower.endsWith(".jsx") ||
               lower.endsWith(".go") ||
               lower.endsWith(".rs") ||
               lower.endsWith(".cpp") ||
               lower.endsWith(".c") ||
               lower.endsWith(".cs");
    }

    /**
     * Checks if package.json has a specific script
     */
    private boolean hasScript(String packageJsonPath, String scriptName) {
        try {
            Path path = Paths.get(packageJsonPath);
            if (!Files.exists(path)) {
                return false;
            }

            String content = Files.readString(path);
            return content.contains("\"" + scriptName + "\"");
        } catch (Exception e) {
            return false;
        }
    }
}
