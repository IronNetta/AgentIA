package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.config.AgentConfig;
import org.seba.agentcli.config.ConfigLoader;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
public class ConfigTool extends AbstractTool {

    private final ConfigLoader configLoader;

    public ConfigTool(CliService cliService, ConfigLoader configLoader) {
        super(cliService);
        this.configLoader = configLoader;
    }

    @Override
    public String getName() {
        return "@config";
    }

    @Override
    public String getDescription() {
        return "Gère la configuration du projet (.agentcli.yml)";
    }

    @Override
    public String getUsage() {
        return "@config [init|show|example]";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        String action = args.isEmpty() ? "show" : args.trim().toLowerCase();

        return switch (action) {
            case "init" -> initConfig(context);
            case "show" -> showConfig(context);
            case "example" -> showExample();
            default -> formatError("Action invalide. Usage: " + getUsage());
        };
    }

    private String initConfig(ProjectContext context) {
        try {
            Path configPath = context.getRootPath().resolve(".agentcli.yml");

            if (Files.exists(configPath)) {
                return formatError("Fichier .agentcli.yml existe déjà. Utilisez '@config show' pour le voir.");
            }

            AgentConfig config = configLoader.createDefaultConfig(
                context.getRootPath().getFileName().toString(),
                context.getProjectType().name()
            );

            configLoader.saveConfig(config, context.getRootPath());

            return formatSuccess("Configuration initialisée dans .agentcli.yml\n\n") +
                   "Vous pouvez maintenant éditer ce fichier pour personnaliser le comportement.\n" +
                   "Utilisez '@config example' pour voir toutes les options disponibles.";

        } catch (Exception e) {
            return formatError("Erreur lors de l'initialisation: " + e.getMessage());
        }
    }

    private String showConfig(ProjectContext context) {
        try {
            Optional<AgentConfig> config = configLoader.loadConfig(context.getRootPath());

            if (config.isEmpty()) {
                return formatInfo("Aucun fichier .agentcli.yml trouvé.\n\n") +
                       "Utilisez '@config init' pour créer une configuration par défaut.";
            }

            AgentConfig cfg = config.get();
            StringBuilder output = new StringBuilder();
            output.append("📋 Configuration actuelle:\n\n");
            output.append("Nom du projet: ").append(cfg.getProjectName()).append("\n");
            output.append("Type: ").append(cfg.getProjectType()).append("\n");
            output.append("Chemins ignorés: ").append(cfg.getIgnorePaths().size()).append("\n");
            output.append("Taille max fichier: ").append(cfg.getMaxFileSize() / 1024).append(" KB\n");

            if (!cfg.getCustomCommands().isEmpty()) {
                output.append("\nCommandes personnalisées:\n");
                cfg.getCustomCommands().forEach((k, v) ->
                    output.append("  ").append(k).append(" → ").append(v).append("\n")
                );
            }

            return output.toString();

        } catch (Exception e) {
            return formatError("Erreur lors de la lecture: " + e.getMessage());
        }
    }

    private String showExample() {
        return "📄 Exemple de fichier .agentcli.yml:\n\n" +
               configLoader.generateExampleConfig() +
               "\n\nUtilisez '@config init' pour créer un fichier de configuration.";
    }
}
