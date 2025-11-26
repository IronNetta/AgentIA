package org.seba.agentcli.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Component
public class ConfigLoader {

    private static final String CONFIG_FILE = ".agentcli.yml";
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public Optional<AgentConfig> loadConfig(Path projectRoot) {
        Path configPath = projectRoot.resolve(CONFIG_FILE);

        if (!Files.exists(configPath)) {
            return Optional.empty();
        }

        try {
            AgentConfig config = yamlMapper.readValue(configPath.toFile(), AgentConfig.class);
            System.out.println("✓ Configuration chargée depuis " + CONFIG_FILE);
            return Optional.of(config);
        } catch (IOException e) {
            System.err.println("⚠ Erreur lors du chargement de " + CONFIG_FILE + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    public void saveConfig(AgentConfig config, Path projectRoot) throws IOException {
        Path configPath = projectRoot.resolve(CONFIG_FILE);
        yamlMapper.writeValue(configPath.toFile(), config);
        System.out.println("✓ Configuration sauvegardée dans " + CONFIG_FILE);
    }

    public AgentConfig createDefaultConfig(String projectName, String projectType) {
        AgentConfig config = new AgentConfig();
        config.setProjectName(projectName);
        config.setProjectType(projectType);

        // Default ignore paths
        List<String> ignorePaths = new ArrayList<>(Arrays.asList(
            ".git", ".svn", "node_modules", "target", "build",
            "dist", "out", ".idea", ".vscode", "__pycache__",
            "*.class", "*.pyc", "*.o", "*.so"
        ));
        config.setIgnorePaths(ignorePaths);

        return config;
    }

    public String generateExampleConfig() {
        return """
            # Agent CLI Configuration
            # This file configures the behavior of the Agent CLI for your project

            projectName: "My Project"
            projectType: "auto"  # auto, java, python, node, go, rust, etc.

            # Paths to ignore during indexing
            ignorePaths:
              - ".git"
              - "node_modules"
              - "target"
              - "build"
              - "__pycache__"

            # Specific paths to include (optional)
            includePaths:
              - "src"
              - "lib"

            # Maximum file size to analyze (in bytes)
            maxFileSize: 1000000  # 1MB

            # Custom commands (shortcuts)
            customCommands:
              "@build": "@execute build"
              "@test": "@execute test"

            # Tool-specific settings
            toolSettings:
              search:
                maxResults: 20
              tree:
                defaultDepth: 3
            """;
    }
}
