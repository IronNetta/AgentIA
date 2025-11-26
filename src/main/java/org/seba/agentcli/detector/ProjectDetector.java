package org.seba.agentcli.detector;

import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.model.ProjectType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

@Component
public class ProjectDetector {

    public ProjectContext detectProject(Path rootPath) throws IOException {
        ProjectType type = detectProjectType(rootPath);
        ProjectContext context = new ProjectContext(rootPath, type);

        // Index source files
        indexSourceFiles(context);

        // Detect frameworks
        detectFrameworks(context);

        return context;
    }

    private ProjectType detectProjectType(Path rootPath) {
        for (ProjectType type : ProjectType.values()) {
            if (type == ProjectType.UNKNOWN) continue;

            String marker = type.getMarkerFile();
            if (marker != null) {
                Path markerPath = rootPath.resolve(marker);
                if (Files.exists(markerPath)) {
                    return type;
                }

                // Check with glob pattern for markers like *.csproj
                if (marker.contains("*")) {
                    try (Stream<Path> paths = Files.walk(rootPath, 2)) {
                        PathMatcher matcher = FileSystems.getDefault()
                                .getPathMatcher("glob:" + marker);
                        if (paths.anyMatch(p -> matcher.matches(p.getFileName()))) {
                            return type;
                        }
                    } catch (IOException e) {
                        // Continue checking
                    }
                }
            }
        }
        return ProjectType.UNKNOWN;
    }

    private void indexSourceFiles(ProjectContext context) throws IOException {
        String pattern = context.getProjectType().getFilePattern();
        PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + pattern);

        try (Stream<Path> paths = Files.walk(context.getRootPath(), FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::shouldIncludeFile)  // Filtre pour ignorer certains fichiers/dossiers
                    .filter(p -> matcher.matches(context.getRootPath().relativize(p)))
                    .forEach(context::addSourceFile);
        }
    }

    private boolean shouldIncludeFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();

        // Fichiers à ignorer
        if (fileName.startsWith(".") || fileName.startsWith("_")) {
            return false;
        }

        // Dossiers à ignorer
        for (Path element : path) {
            String elementName = element.toString().toLowerCase();
            if (elementName.equals("node_modules") ||
                elementName.equals("target") ||
                elementName.equals("build") ||
                elementName.equals("dist") ||
                elementName.equals("out") ||
                elementName.equals(".git") ||
                elementName.equals(".svn") ||
                elementName.equals(".idea") ||
                elementName.equals(".vscode") ||
                elementName.equals("__pycache__") ||
                elementName.equals("venv") ||
                elementName.equals(".venv")) {
                return false;
            }
        }

        return true;
    }

    private void detectFrameworks(ProjectContext context) throws IOException {
        ProjectType type = context.getProjectType();

        switch (type) {
            case JAVA_MAVEN:
            case JAVA_GRADLE:
                detectJavaFrameworks(context);
                break;
            case PYTHON:
                detectPythonFrameworks(context);
                break;
            case NODE_JS:
            case TYPESCRIPT:
                detectNodeFrameworks(context);
                break;
            case GO:
                detectGoFrameworks(context);
                break;
            default:
                break;
        }
    }

    private void detectJavaFrameworks(ProjectContext context) throws IOException {
        Path pomPath = context.getRootPath().resolve("pom.xml");
        if (Files.exists(pomPath)) {
            String content = Files.readString(pomPath);
            if (content.contains("spring-boot")) {
                context.addFramework("Spring Boot");
            }
            if (content.contains("jakarta.persistence") || content.contains("javax.persistence")) {
                context.addFramework("JPA");
            }
            if (content.contains("junit")) {
                context.addFramework("JUnit");
            }
        }
    }

    private void detectPythonFrameworks(ProjectContext context) throws IOException {
        Path reqPath = context.getRootPath().resolve("requirements.txt");
        if (Files.exists(reqPath)) {
            String content = Files.readString(reqPath);
            if (content.contains("django")) context.addFramework("Django");
            if (content.contains("flask")) context.addFramework("Flask");
            if (content.contains("fastapi")) context.addFramework("FastAPI");
            if (content.contains("pytest")) context.addFramework("pytest");
        }
    }

    private void detectNodeFrameworks(ProjectContext context) throws IOException {
        Path pkgPath = context.getRootPath().resolve("package.json");
        if (Files.exists(pkgPath)) {
            String content = Files.readString(pkgPath);
            if (content.contains("\"react\"")) context.addFramework("React");
            if (content.contains("\"vue\"")) context.addFramework("Vue");
            if (content.contains("\"angular\"")) context.addFramework("Angular");
            if (content.contains("\"express\"")) context.addFramework("Express");
            if (content.contains("\"next\"")) context.addFramework("Next.js");
            if (content.contains("\"jest\"")) context.addFramework("Jest");
        }
    }

    private void detectGoFrameworks(ProjectContext context) throws IOException {
        Path goModPath = context.getRootPath().resolve("go.mod");
        if (Files.exists(goModPath)) {
            String content = Files.readString(goModPath);
            if (content.contains("gin-gonic/gin")) context.addFramework("Gin");
            if (content.contains("gorilla/mux")) context.addFramework("Gorilla Mux");
            if (content.contains("fiber")) context.addFramework("Fiber");
        }
    }
}
