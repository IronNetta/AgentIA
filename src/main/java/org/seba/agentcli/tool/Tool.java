package org.seba.agentcli.tool;

import org.seba.agentcli.model.ProjectContext;

public interface Tool {
    /**
     * The command name (e.g., "@file", "@search")
     */
    String getName();

    /**
     * Description of what this tool does
     */
    String getDescription();

    /**
     * Usage example
     */
    String getUsage();

    /**
     * Execute the tool with given arguments
     */
    String execute(String args, ProjectContext context);

    /**
     * Check if this tool can handle the given command
     */
    default boolean canHandle(String command) {
        return command.trim().startsWith(getName());
    }

    /**
     * Extract arguments from the command
     */
    default String extractArgs(String command) {
        String trimmed = command.trim();
        if (trimmed.length() > getName().length()) {
            return trimmed.substring(getName().length()).trim();
        }
        return "";
    }
}
