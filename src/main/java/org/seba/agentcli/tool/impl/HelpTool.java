package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.seba.agentcli.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HelpTool extends AbstractTool {

    private List<Tool> allTools;

    public HelpTool(CliService cliService) {
        super(cliService);
    }

    public void setAllTools(List<Tool> tools) {
        this.allTools = tools;
    }

    @Override
    public String getName() {
        return "@help";
    }

    @Override
    public String getDescription() {
        return "Affiche l'aide et la liste des commandes disponibles";
    }

    @Override
    public String getUsage() {
        return "@help [commande]";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (!args.isEmpty()) {
            return getCommandHelp(args);
        }

        StringBuilder help = new StringBuilder();
        help.append("\n╔══════════════════════════════════════════╗\n");
        help.append("║     🤖 Agent CLI - Commandes            ║\n");
        help.append("╚══════════════════════════════════════════╝\n\n");

        if (context != null) {
            help.append("📊 Projet: ").append(context.getProjectType().getDisplayName()).append("\n");
            help.append("📁 Fichiers: ").append(context.getSourceFiles().size()).append("\n");
            if (!context.getFrameworks().isEmpty()) {
                help.append("🔧 Frameworks: ").append(String.join(", ", context.getFrameworks())).append("\n");
            }
            help.append("\n");
        }

        help.append("Commandes disponibles:\n\n");

        if (allTools != null) {
            for (Tool tool : allTools) {
                if (tool instanceof HelpTool) continue;
                help.append(String.format("  %-20s %s\n", tool.getName(), tool.getDescription()));
                help.append(String.format("  %-20s Usage: %s\n\n", "", tool.getUsage()));
            }
        }

        help.append("Commandes système:\n");
        help.append("  exit                 Quitter l'application\n");
        help.append("  clear                Effacer le contexte de conversation\n\n");

        help.append("💡 Astuce: Tapez @help <commande> pour plus de détails\n");

        return help.toString();
    }

    private String getCommandHelp(String commandName) {
        if (allTools == null) {
            return formatError("Aucune commande disponible");
        }

        String searchName = commandName.startsWith("@") ? commandName : "@" + commandName;

        for (Tool tool : allTools) {
            if (tool.getName().equalsIgnoreCase(searchName)) {
                return String.format(
                    "\n📖 Aide pour %s\n\n" +
                    "Description: %s\n\n" +
                    "Usage: %s\n",
                    tool.getName(),
                    tool.getDescription(),
                    tool.getUsage()
                );
            }
        }

        return formatError("Commande inconnue: " + commandName);
    }
}
