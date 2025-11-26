package org.seba.agentcli.model;

import java.nio.file.Path;
import java.util.*;

public class ProjectContext {
    private final Path rootPath;
    private final ProjectType projectType;
    private final List<Path> sourceFiles;
    private final Map<String, Object> metadata;
    private final Set<String> frameworks;

    public ProjectContext(Path rootPath, ProjectType projectType) {
        this.rootPath = rootPath;
        this.projectType = projectType;
        this.sourceFiles = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.frameworks = new HashSet<>();
    }

    public Path getRootPath() {
        return rootPath;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public List<Path> getSourceFiles() {
        return sourceFiles;
    }

    public void addSourceFile(Path file) {
        this.sourceFiles.add(file);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public Set<String> getFrameworks() {
        return frameworks;
    }

    public void addFramework(String framework) {
        this.frameworks.add(framework);
    }

    @Override
    public String toString() {
        return String.format("ProjectContext{type=%s, files=%d, frameworks=%s}",
                projectType.getDisplayName(), sourceFiles.size(), frameworks);
    }
}
