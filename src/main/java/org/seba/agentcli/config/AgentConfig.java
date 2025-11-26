package org.seba.agentcli.config;

import java.util.*;

public class AgentConfig {
    private String projectName;
    private String projectType;
    private List<String> ignorePaths = new ArrayList<>();
    private List<String> includePaths = new ArrayList<>();
    private Map<String, String> customCommands = new HashMap<>();
    private Map<String, Object> toolSettings = new HashMap<>();
    private int maxFileSize = 1_000_000; // 1MB default

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public List<String> getIgnorePaths() {
        return ignorePaths;
    }

    public void setIgnorePaths(List<String> ignorePaths) {
        this.ignorePaths = ignorePaths;
    }

    public List<String> getIncludePaths() {
        return includePaths;
    }

    public void setIncludePaths(List<String> includePaths) {
        this.includePaths = includePaths;
    }

    public Map<String, String> getCustomCommands() {
        return customCommands;
    }

    public void setCustomCommands(Map<String, String> customCommands) {
        this.customCommands = customCommands;
    }

    public Map<String, Object> getToolSettings() {
        return toolSettings;
    }

    public void setToolSettings(Map<String, Object> toolSettings) {
        this.toolSettings = toolSettings;
    }

    public int getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(int maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    @Override
    public String toString() {
        return String.format("AgentConfig{project='%s', type='%s', ignore=%d paths}",
                projectName, projectType, ignorePaths.size());
    }
}
