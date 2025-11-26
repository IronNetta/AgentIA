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
 * Docker integration tool.
 *
 * Commands:
 *   @docker init           - Generate optimal Dockerfile
 *   @docker compose        - Generate docker-compose.yml
 *   @docker optimize       - Optimize image size
 *   @docker security       - Security scan
 */
@Component
public class DockerTool extends AbstractTool {

    public DockerTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@docker";
    }

    @Override
    public String getDescription() {
        return "Docker integration and optimization";
    }

    @Override
    public String getUsage() {
        return "@docker <command>\n\n" +
               "Commands:\n" +
               "  init           Generate optimal Dockerfile\n" +
               "  compose        Generate docker-compose.yml\n" +
               "  optimize       Optimize Docker image size\n" +
               "  security       Security scan for vulnerabilities\n\n" +
               "Examples:\n" +
               "  @docker init\n" +
               "  @docker compose\n" +
               "  @docker optimize";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args == null || args.trim().isEmpty()) {
            return AnsiColors.colorize("Error: Command required", AnsiColors.RED) + "\n" +
                   AnsiColors.colorize("Usage: " + getUsage(), AnsiColors.YELLOW);
        }

        String command = args.trim().split("\\s+")[0];

        return switch (command) {
            case "init" -> initDockerfile(context);
            case "compose" -> initCompose(context);
            case "optimize" -> optimize(context);
            case "security" -> securityScan(context);
            default -> AnsiColors.colorize("Error: Unknown command: " + command, AnsiColors.RED) + "\n" +
                       AnsiColors.colorize("Available: init, compose, optimize, security", AnsiColors.YELLOW);
        };
    }

    private String initDockerfile(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🐳 Generating Dockerfile...", AnsiColors.CYAN));

        ProjectType type = context.getProjectType();

        String dockerfile = generateDockerfile(type, context);

        Path dockerfilePath = context.getRootPath().resolve("Dockerfile");

        if (Files.exists(dockerfilePath)) {
            return AnsiColors.colorize("\n⚠️  Dockerfile already exists!\n", AnsiColors.YELLOW) +
                   AnsiColors.colorize("\nGenerated Dockerfile (not saved):\n\n", AnsiColors.BRIGHT_BLACK) +
                   dockerfile +
                   AnsiColors.colorize("\nTo overwrite, delete existing Dockerfile first.\n", AnsiColors.BRIGHT_BLACK);
        }

        try {
            Files.writeString(dockerfilePath, dockerfile);
            return AnsiColors.colorize("\n✅ Dockerfile created successfully!\n\n", AnsiColors.GREEN) +
                   AnsiColors.colorize("Location: Dockerfile\n", AnsiColors.CYAN) +
                   AnsiColors.colorize("\nBuild with:\n", AnsiColors.BRIGHT_BLACK) +
                   AnsiColors.colorize("  docker build -t myapp .\n", AnsiColors.WHITE) +
                   AnsiColors.colorize("  docker run -p 8080:8080 myapp\n", AnsiColors.WHITE);
        } catch (IOException e) {
            return AnsiColors.colorize("Error writing Dockerfile: " + e.getMessage(), AnsiColors.RED);
        }
    }

    private String initCompose(ProjectContext context) {
        System.out.println(AnsiColors.colorize("🐳 Generating docker-compose.yml...", AnsiColors.CYAN));

        String compose = generateDockerCompose(context);

        Path composePath = context.getRootPath().resolve("docker-compose.yml");

        if (Files.exists(composePath)) {
            return AnsiColors.colorize("\n⚠️  docker-compose.yml already exists!\n", AnsiColors.YELLOW) +
                   AnsiColors.colorize("\nGenerated compose (not saved):\n\n", AnsiColors.BRIGHT_BLACK) +
                   compose;
        }

        try {
            Files.writeString(composePath, compose);
            return AnsiColors.colorize("\n✅ docker-compose.yml created!\n\n", AnsiColors.GREEN) +
                   AnsiColors.colorize("Location: docker-compose.yml\n", AnsiColors.CYAN) +
                   AnsiColors.colorize("\nRun with:\n", AnsiColors.BRIGHT_BLACK) +
                   AnsiColors.colorize("  docker-compose up\n", AnsiColors.WHITE);
        } catch (IOException e) {
            return AnsiColors.colorize("Error writing docker-compose.yml: " + e.getMessage(), AnsiColors.RED);
        }
    }

    private String optimize(ProjectContext context) {
        return AnsiColors.colorize("\n🔧 Docker Image Optimization Tips:\n\n", AnsiColors.CYAN) +
               AnsiColors.colorize("1. Use multi-stage builds\n", AnsiColors.WHITE) +
               AnsiColors.colorize("   → Separate build and runtime stages\n\n", AnsiColors.BRIGHT_BLACK) +
               AnsiColors.colorize("2. Use .dockerignore\n", AnsiColors.WHITE) +
               AnsiColors.colorize("   → Exclude node_modules, .git, etc.\n\n", AnsiColors.BRIGHT_BLACK) +
               AnsiColors.colorize("3. Minimize layers\n", AnsiColors.WHITE) +
               AnsiColors.colorize("   → Combine RUN commands\n\n", AnsiColors.BRIGHT_BLACK) +
               AnsiColors.colorize("4. Use specific base images\n", AnsiColors.WHITE) +
               AnsiColors.colorize("   → Use alpine or slim variants\n\n", AnsiColors.BRIGHT_BLACK) +
               AnsiColors.colorize("5. Clean up after installation\n", AnsiColors.WHITE) +
               AnsiColors.colorize("   → Remove caches and temp files\n\n", AnsiColors.BRIGHT_BLACK) +
               AnsiColors.colorize("\n⚠️  Automated optimization coming soon!\n", AnsiColors.YELLOW);
    }

    private String securityScan(ProjectContext context) {
        return AnsiColors.colorize("\n🔒 Docker Security Scan:\n\n", AnsiColors.CYAN) +
               AnsiColors.colorize("Use these tools for security scanning:\n\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  • Docker Scout:  docker scout cves myapp:latest\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  • Trivy:         trivy image myapp:latest\n", AnsiColors.WHITE) +
               AnsiColors.colorize("  • Snyk:          snyk container test myapp:latest\n", AnsiColors.WHITE) +
               AnsiColors.colorize("\n⚠️  Integrated scanning coming soon!\n", AnsiColors.YELLOW);
    }

    private String generateDockerfile(ProjectType type, ProjectContext context) {
        return switch (type) {
            case JAVA_MAVEN -> generateJavaMavenDockerfile();
            case JAVA_GRADLE -> generateJavaGradleDockerfile();
            case NODE_JS, TYPESCRIPT -> generateNodeDockerfile();
            case PYTHON -> generatePythonDockerfile();
            case GO -> generateGoDockerfile();
            default -> generateGenericDockerfile(type);
        };
    }

    private String generateJavaMavenDockerfile() {
        return """
               # Multi-stage build for Maven project
               FROM maven:3.9-eclipse-temurin-17 AS build
               WORKDIR /app

               # Copy pom.xml and download dependencies (cached layer)
               COPY pom.xml .
               RUN mvn dependency:go-offline

               # Copy source and build
               COPY src ./src
               RUN mvn clean package -DskipTests

               # Runtime stage
               FROM eclipse-temurin:17-jre-alpine
               WORKDIR /app

               # Copy jar from build stage
               COPY --from=build /app/target/*.jar app.jar

               # Expose port
               EXPOSE 8080

               # Run application
               ENTRYPOINT ["java", "-jar", "app.jar"]
               """;
    }

    private String generateJavaGradleDockerfile() {
        return """
               # Multi-stage build for Gradle project
               FROM gradle:8-jdk17 AS build
               WORKDIR /app

               # Copy gradle files (cached layer)
               COPY build.gradle settings.gradle ./
               COPY gradle ./gradle

               # Copy source and build
               COPY src ./src
               RUN gradle build --no-daemon -x test

               # Runtime stage
               FROM eclipse-temurin:17-jre-alpine
               WORKDIR /app

               # Copy jar from build stage
               COPY --from=build /app/build/libs/*.jar app.jar

               # Expose port
               EXPOSE 8080

               # Run application
               ENTRYPOINT ["java", "-jar", "app.jar"]
               """;
    }

    private String generateNodeDockerfile() {
        return """
               # Multi-stage build for Node.js
               FROM node:20-alpine AS build
               WORKDIR /app

               # Copy package files (cached layer)
               COPY package*.json ./
               RUN npm ci --only=production

               # Copy source
               COPY . .

               # Build if needed (for TypeScript, etc.)
               # RUN npm run build

               # Runtime stage
               FROM node:20-alpine
               WORKDIR /app

               # Copy from build stage
               COPY --from=build /app/node_modules ./node_modules
               COPY --from=build /app .

               # Expose port
               EXPOSE 3000

               # Run application
               CMD ["node", "index.js"]
               """;
    }

    private String generatePythonDockerfile() {
        return """
               FROM python:3.11-slim
               WORKDIR /app

               # Install dependencies
               COPY requirements.txt .
               RUN pip install --no-cache-dir -r requirements.txt

               # Copy application
               COPY . .

               # Expose port
               EXPOSE 8000

               # Run application
               CMD ["python", "app.py"]
               """;
    }

    private String generateGoDockerfile() {
        return """
               # Multi-stage build for Go
               FROM golang:1.21-alpine AS build
               WORKDIR /app

               # Copy go mod files
               COPY go.mod go.sum ./
               RUN go mod download

               # Copy source and build
               COPY . .
               RUN CGO_ENABLED=0 GOOS=linux go build -o main .

               # Runtime stage
               FROM alpine:latest
               WORKDIR /app

               # Copy binary from build stage
               COPY --from=build /app/main .

               # Expose port
               EXPOSE 8080

               # Run application
               CMD ["./main"]
               """;
    }

    private String generateGenericDockerfile(ProjectType type) {
        return "# Dockerfile for " + type + "\n" +
               "# TODO: Customize for your project\n\n" +
               "FROM ubuntu:latest\n" +
               "WORKDIR /app\n" +
               "COPY . .\n" +
               "CMD [\"bash\"]\n";
    }

    private String generateDockerCompose(ProjectContext context) {
        String projectName = context.getRootPath().getFileName().toString();

        return """
               version: '3.8'

               services:
                 app:
                   build: .
                   ports:
                     - "8080:8080"
                   environment:
                     - NODE_ENV=production
                   # volumes:
                   #   - ./data:/app/data
                   restart: unless-stopped

                 # Add database service if needed
                 # db:
                 #   image: postgres:15-alpine
                 #   environment:
                 #     POSTGRES_DB: myapp
                 #     POSTGRES_USER: user
                 #     POSTGRES_PASSWORD: password
                 #   volumes:
                 #     - db-data:/var/lib/postgresql/data
                 #   ports:
                 #     - "5432:5432"

               # volumes:
               #   db-data:
               """.replace("myapp", projectName);
    }
}
