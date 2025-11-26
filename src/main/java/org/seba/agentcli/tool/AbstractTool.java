package org.seba.agentcli.tool;

import org.seba.agentcli.CliService;
import org.seba.agentcli.model.ProjectContext;

public abstract class AbstractTool implements Tool {
    protected final CliService cliService;
    protected ProjectContext projectContext;

    protected AbstractTool(CliService cliService) {
        this.cliService = cliService;
    }

    public void setProjectContext(ProjectContext projectContext) {
        this.projectContext = projectContext;
    }

    public ProjectContext getProjectContext() {
        return projectContext;
    }

    protected String formatError(String message) {
        return "❌ Erreur: " + message;
    }

    protected String formatSuccess(String message) {
        return "✓ " + message;
    }

    protected String formatInfo(String message) {
        return "ℹ " + message;
    }
}
