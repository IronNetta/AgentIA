package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.model.ProjectType;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CI/CD Pipeline Generator.
 *
 * Commands:
 *   @ci setup github       - Generate GitHub Actions workflows
 *   @ci setup gitlab       - Generate GitLab CI configuration
 *   @ci test               - Generate test pipeline
 *   @ci deploy             - Generate deployment pipeline
 */
@Component
public class CiCdTool extends AbstractTool {

    public CiCdTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@ci";
    }

    @Override
    public String getDescription() {
        return "CI/CD pipeline generator for GitHub Actions & GitLab CI";
    }

    @Override
    public String getUsage() {
        return "@ci <command>\n\n" +
               "Commands:\n" +
               "  setup github       Generate GitHub Actions workflows\n" +
               "  setup gitlab       Generate GitLab CI configuration\n" +
               "  test               Generate test pipeline only\n" +
               "  deploy             Generate deployment pipeline\n\n" +
               "Examples:\n" +
               "  @ci setup github\n" +
               "  @ci setup gitlab\n" +
               "  @ci test";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args == null || args.trim().isEmpty()) {
            return AnsiColors.colorize("Error: Command required", AnsiColors.RED) + "\n" +
                   AnsiColors.colorize("Usage: " + getUsage(), AnsiColors.YELLOW);
        }

        String[] parts = args.trim().split("\\s+");
        String command = parts[0];

        return switch (command) {
            case "setup" -> {
                if (parts.length < 2) {
                    yield AnsiColors.colorize("Error: Platform required (github/gitlab)", AnsiColors.RED);
                }
                yield setupPipeline(parts[1], context);
            }
            case "test" -> generateTestPipeline(context);
            case "deploy" -> generateDeployPipeline(context);
            default -> AnsiColors.colorize("Error: Unknown command: " + command, AnsiColors.RED) + "\n" +
                       AnsiColors.colorize("Available: setup, test, deploy", AnsiColors.YELLOW);
        };
    }

    private String setupPipeline(String platform, ProjectContext context) {
        System.out.println(AnsiColors.colorize("🔧 Generating " + platform + " pipeline...", AnsiColors.CYAN));

        return switch (platform.toLowerCase()) {
            case "github" -> setupGitHubActions(context);
            case "gitlab" -> setupGitLabCI(context);
            default -> AnsiColors.colorize("Error: Unknown platform: " + platform, AnsiColors.RED) + "\n" +
                       AnsiColors.colorize("Supported: github, gitlab", AnsiColors.YELLOW);
        };
    }

    private String setupGitHubActions(ProjectContext context) {
        String workflow = generateGitHubWorkflow(context);

        Path workflowsDir = context.getRootPath().resolve(".github/workflows");
        Path workflowFile = workflowsDir.resolve("ci.yml");

        try {
            Files.createDirectories(workflowsDir);

            if (Files.exists(workflowFile)) {
                return AnsiColors.colorize("\n⚠️  Workflow already exists!\n", AnsiColors.YELLOW) +
                       AnsiColors.colorize("\nGenerated workflow (not saved):\n\n", AnsiColors.BRIGHT_BLACK) +
                       workflow;
            }

            Files.writeString(workflowFile, workflow);

            return AnsiColors.colorize("\n✅ GitHub Actions workflow created!\n\n", AnsiColors.GREEN) +
                   AnsiColors.colorize("Location: .github/workflows/ci.yml\n", AnsiColors.CYAN) +
                   AnsiColors.colorize("\nWorkflow will run on:\n", AnsiColors.BRIGHT_BLACK) +
                   AnsiColors.colorize("  • Push to main/develop\n", AnsiColors.WHITE) +
                   AnsiColors.colorize("  • Pull requests\n", AnsiColors.WHITE) +
                   AnsiColors.colorize("\nCommit and push to trigger!\n", AnsiColors.BRIGHT_BLACK);

        } catch (IOException e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    private String setupGitLabCI(ProjectContext context) {
        String pipeline = generateGitLabPipeline(context);

        Path ciFile = context.getRootPath().resolve(".gitlab-ci.yml");

        try {
            if (Files.exists(ciFile)) {
                return AnsiColors.colorize("\n⚠️  .gitlab-ci.yml already exists!\n", AnsiColors.YELLOW) +
                       AnsiColors.colorize("\nGenerated pipeline (not saved):\n\n", AnsiColors.BRIGHT_BLACK) +
                       pipeline;
            }

            Files.writeString(ciFile, pipeline);

            return AnsiColors.colorize("\n✅ GitLab CI pipeline created!\n\n", AnsiColors.GREEN) +
                   AnsiColors.colorize("Location: .gitlab-ci.yml\n", AnsiColors.CYAN) +
                   AnsiColors.colorize("\nPipeline stages:\n", AnsiColors.BRIGHT_BLACK) +
                   AnsiColors.colorize("  • build\n", AnsiColors.WHITE) +
                   AnsiColors.colorize("  • test\n", AnsiColors.WHITE) +
                   AnsiColors.colorize("  • deploy\n", AnsiColors.WHITE);

        } catch (IOException e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    private String generateTestPipeline(ProjectContext context) {
        return AnsiColors.colorize("\n🧪 Test Pipeline Template:\n\n", AnsiColors.CYAN) +
               generateGitHubWorkflow(context) +
               AnsiColors.colorize("\nUse: @ci setup github\n", AnsiColors.BRIGHT_BLACK);
    }

    private String generateDeployPipeline(ProjectContext context) {
        return AnsiColors.colorize("\n🚀 Deployment Pipeline:\n\n", AnsiColors.CYAN) +
               AnsiColors.colorize("Coming soon! Will include:\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  • Docker build & push\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  • Kubernetes deployment\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  • Environment management\n", AnsiColors.WHITE);
    }

    private String generateGitHubWorkflow(ProjectContext context) {
        ProjectType type = context.getProjectType();

        return switch (type) {
            case JAVA_MAVEN -> generateMavenWorkflow();
            case JAVA_GRADLE -> generateGradleWorkflow();
            case NODE_JS, TYPESCRIPT -> generateNodeWorkflow();
            case PYTHON -> generatePythonWorkflow();
            default -> generateGenericWorkflow();
        };
    }

    private String generateMavenWorkflow() {
        return """
               name: CI

               on:
                 push:
                   branches: [ main, develop ]
                 pull_request:
                   branches: [ main ]

               jobs:
                 build:
                   runs-on: ubuntu-latest

                   steps:
                   - uses: actions/checkout@v4

                   - name: Set up JDK 17
                     uses: actions/setup-java@v4
                     with:
                       java-version: '17'
                       distribution: 'temurin'
                       cache: maven

                   - name: Build with Maven
                     run: mvn clean package -DskipTests

                   - name: Run tests
                     run: mvn test

                   - name: Generate coverage report
                     run: mvn jacoco:report

                   - name: Upload coverage
                     uses: codecov/codecov-action@v3
                     with:
                       files: ./target/site/jacoco/jacoco.xml
               """;
    }

    private String generateGradleWorkflow() {
        return """
               name: CI

               on:
                 push:
                   branches: [ main, develop ]
                 pull_request:
                   branches: [ main ]

               jobs:
                 build:
                   runs-on: ubuntu-latest

                   steps:
                   - uses: actions/checkout@v4

                   - name: Set up JDK 17
                     uses: actions/setup-java@v4
                     with:
                       java-version: '17'
                       distribution: 'temurin'

                   - name: Setup Gradle
                     uses: gradle/gradle-build-action@v2

                   - name: Build with Gradle
                     run: ./gradlew build

                   - name: Run tests
                     run: ./gradlew test

                   - name: Generate coverage report
                     run: ./gradlew jacocoTestReport
               """;
    }

    private String generateNodeWorkflow() {
        return """
               name: CI

               on:
                 push:
                   branches: [ main, develop ]
                 pull_request:
                   branches: [ main ]

               jobs:
                 build:
                   runs-on: ubuntu-latest

                   steps:
                   - uses: actions/checkout@v4

                   - name: Setup Node.js
                     uses: actions/setup-node@v4
                     with:
                       node-version: '20'
                       cache: 'npm'

                   - name: Install dependencies
                     run: npm ci

                   - name: Run linter
                     run: npm run lint --if-present

                   - name: Run tests
                     run: npm test

                   - name: Build
                     run: npm run build --if-present
               """;
    }

    private String generatePythonWorkflow() {
        return """
               name: CI

               on:
                 push:
                   branches: [ main, develop ]
                 pull_request:
                   branches: [ main ]

               jobs:
                 build:
                   runs-on: ubuntu-latest

                   steps:
                   - uses: actions/checkout@v4

                   - name: Set up Python
                     uses: actions/setup-python@v5
                     with:
                       python-version: '3.11'
                       cache: 'pip'

                   - name: Install dependencies
                     run: |
                       pip install -r requirements.txt
                       pip install pytest pytest-cov

                   - name: Run tests
                     run: pytest --cov

                   - name: Lint with flake8
                     run: |
                       pip install flake8
                       flake8 . --count --select=E9,F63,F7,F82 --show-source --statistics
               """;
    }

    private String generateGenericWorkflow() {
        return """
               name: CI

               on:
                 push:
                   branches: [ main, develop ]
                 pull_request:
                   branches: [ main ]

               jobs:
                 build:
                   runs-on: ubuntu-latest

                   steps:
                   - uses: actions/checkout@v4

                   - name: Run build
                     run: echo "Configure your build command here"

                   - name: Run tests
                     run: echo "Configure your test command here"
               """;
    }

    private String generateGitLabPipeline(ProjectContext context) {
        ProjectType type = context.getProjectType();

        return switch (type) {
            case JAVA_MAVEN -> generateMavenGitLabCI();
            case NODE_JS, TYPESCRIPT -> generateNodeGitLabCI();
            case PYTHON -> generatePythonGitLabCI();
            default -> generateGenericGitLabCI();
        };
    }

    private String generateMavenGitLabCI() {
        return """
               stages:
                 - build
                 - test
                 - deploy

               variables:
                 MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

               cache:
                 paths:
                   - .m2/repository

               build:
                 stage: build
                 image: maven:3.9-eclipse-temurin-17
                 script:
                   - mvn clean package -DskipTests
                 artifacts:
                   paths:
                     - target/*.jar

               test:
                 stage: test
                 image: maven:3.9-eclipse-temurin-17
                 script:
                   - mvn test
                   - mvn jacoco:report
                 artifacts:
                   reports:
                     junit: target/surefire-reports/TEST-*.xml
                     coverage_report:
                       coverage_format: jacoco
                       path: target/site/jacoco/jacoco.xml

               deploy:
                 stage: deploy
                 script:
                   - echo "Deploy to production"
                 only:
                   - main
               """;
    }

    private String generateNodeGitLabCI() {
        return """
               stages:
                 - build
                 - test
                 - deploy

               cache:
                 paths:
                   - node_modules/

               build:
                 stage: build
                 image: node:20-alpine
                 script:
                   - npm ci
                   - npm run build --if-present
                 artifacts:
                   paths:
                     - dist/
                     - build/

               test:
                 stage: test
                 image: node:20-alpine
                 script:
                   - npm ci
                   - npm test

               deploy:
                 stage: deploy
                 script:
                   - echo "Deploy to production"
                 only:
                   - main
               """;
    }

    private String generatePythonGitLabCI() {
        return """
               stages:
                 - test
                 - deploy

               test:
                 stage: test
                 image: python:3.11-slim
                 before_script:
                   - pip install -r requirements.txt
                   - pip install pytest pytest-cov
                 script:
                   - pytest --cov

               deploy:
                 stage: deploy
                 script:
                   - echo "Deploy to production"
                 only:
                   - main
               """;
    }

    private String generateGenericGitLabCI() {
        return """
               stages:
                 - build
                 - test
                 - deploy

               build:
                 stage: build
                 script:
                   - echo "Configure your build"

               test:
                 stage: test
                 script:
                   - echo "Configure your tests"

               deploy:
                 stage: deploy
                 script:
                   - echo "Configure deployment"
                 only:
                   - main
               """;
    }
}
