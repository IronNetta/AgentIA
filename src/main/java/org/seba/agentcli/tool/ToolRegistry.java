package org.seba.agentcli.tool;

import org.seba.agentcli.tool.impl.HelpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ToolRegistry {
    private final List<Tool> tools;
    private final Map<String, Tool> toolMap;

    public ToolRegistry(List<Tool> tools, HelpTool helpTool) {
        this.tools = new ArrayList<>(tools);
        this.toolMap = new HashMap<>();

        // Register all tools
        for (Tool tool : tools) {
            toolMap.put(tool.getName().toLowerCase(), tool);
        }

        // Give HelpTool access to all tools
        helpTool.setAllTools(this.tools);

        System.out.println("✓ Registered " + tools.size() + " tools");
    }

    public Optional<Tool> findTool(String command) {
        String trimmed = command.trim().toLowerCase();

        // First, try exact match
        for (Tool tool : tools) {
            if (trimmed.startsWith(tool.getName().toLowerCase())) {
                return Optional.of(tool);
            }
        }

        return Optional.empty();
    }

    public List<Tool> getAllTools() {
        return new ArrayList<>(tools);
    }

    public void printAvailableTools() {
        System.out.println("\nOutils disponibles:");
        tools.stream()
            .sorted(Comparator.comparing(Tool::getName))
            .forEach(tool -> System.out.println("  " + tool.getName() + " - " + tool.getDescription()));
    }
}
