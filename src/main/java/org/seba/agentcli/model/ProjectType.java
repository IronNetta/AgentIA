package org.seba.agentcli.model;

public enum ProjectType {
    JAVA_MAVEN("Java Maven", "pom.xml", "**/*.java"),
    JAVA_GRADLE("Java Gradle", "build.gradle", "**/*.java"),
    PYTHON("Python", "requirements.txt", "**/*.py"),
    NODE_JS("Node.js", "package.json", "**/*.js"),
    TYPESCRIPT("TypeScript", "tsconfig.json", "**/*.ts"),
    GO("Go", "go.mod", "**/*.go"),
    RUST("Rust", "Cargo.toml", "**/*.rs"),
    CSHARP("C#", "*.csproj", "**/*.cs"),
    PHP("PHP", "composer.json", "**/*.php"),
    RUBY("Ruby", "Gemfile", "**/*.rb"),
    UNKNOWN("Unknown", null, "**/*");

    private final String displayName;
    private final String markerFile;
    private final String filePattern;

    ProjectType(String displayName, String markerFile, String filePattern) {
        this.displayName = displayName;
        this.markerFile = markerFile;
        this.filePattern = filePattern;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMarkerFile() {
        return markerFile;
    }

    public String getFilePattern() {
        return filePattern;
    }
}
