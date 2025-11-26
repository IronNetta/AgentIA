package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.config.LLMConfig;
import org.seba.agentcli.model.LLMProvider;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

@Component
public class LLMTool extends AbstractTool {

    private final LLMConfig llmConfig;

    public LLMTool(CliService cliService, LLMConfig llmConfig) {
        super(cliService);
        this.llmConfig = llmConfig;
    }

    @Override
    public String getName() {
        return "@llm";
    }

    @Override
    public String getDescription() {
        return "Affiche ou teste la configuration LLM";
    }

    @Override
    public String getUsage() {
        return "@llm [info|test|providers]";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        String action = args.isEmpty() ? "info" : args.trim().toLowerCase();

        return switch (action) {
            case "info" -> showInfo();
            case "test" -> testConnection();
            case "providers" -> listProviders();
            default -> formatError("Action invalide. Usage: " + getUsage());
        };
    }

    private String showInfo() {
        StringBuilder info = new StringBuilder();
        info.append("🤖 Configuration LLM actuelle\n\n");
        info.append("Provider: ").append(llmConfig.getProviderEnum().getDisplayName()).append("\n");
        info.append("Endpoint: ").append(llmConfig.getEndpoint()).append("\n");
        info.append("Modèle: ").append(llmConfig.getModel()).append("\n");
        info.append("Timeout: ").append(llmConfig.getTimeout()).append("s\n");

        if (llmConfig.getApiKey() != null && !llmConfig.getApiKey().isEmpty()) {
            info.append("API Key: ").append(maskApiKey(llmConfig.getApiKey())).append("\n");
        }

        info.append("\n💡 Pour changer: Éditez src/main/resources/application.yml");

        return info.toString();
    }

    private String testConnection() {
        try {
            StringBuilder result = new StringBuilder();
            result.append(formatInfo("Test de connexion au LLM...\n\n"));

            String testPrompt = "Réponds juste 'OK' pour confirmer que tu es opérationnel.";
            String response = cliService.query(testPrompt);

            if (response.contains("❌")) {
                return formatError("Échec du test\n\n") + response;
            }

            return formatSuccess("Connexion réussie !\n\n") +
                   "Provider: " + llmConfig.getProviderEnum().getDisplayName() + "\n" +
                   "Modèle: " + llmConfig.getModel() + "\n" +
                   "Réponse: " + response.substring(0, Math.min(response.length(), 100)) + "...";

        } catch (Exception e) {
            return formatError("Erreur lors du test: " + e.getMessage());
        }
    }

    private String listProviders() {
        StringBuilder list = new StringBuilder();
        list.append("📋 Providers LLM disponibles\n\n");

        for (LLMProvider provider : LLMProvider.values()) {
            list.append("▸ ").append(provider.name()).append("\n");
            list.append("  Nom: ").append(provider.getDisplayName()).append("\n");
            if (provider.getDefaultEndpoint() != null) {
                list.append("  Endpoint par défaut: ").append(provider.getDefaultEndpoint()).append("\n");
            }

            // Ajouter des exemples de configuration
            list.append("  Configuration:\n");
            list.append(getProviderExample(provider));
            list.append("\n");
        }

        list.append("💡 Pour configurer: Éditez application.yml avec un des exemples ci-dessus\n");
        list.append("📖 Voir aussi: application-example.yml pour tous les exemples");

        return list.toString();
    }

    private String getProviderExample(LLMProvider provider) {
        return switch (provider) {
            case OLLAMA_LOCAL -> """
                  llm:
                    provider: OLLAMA_LOCAL
                    endpoint: http://localhost:11434
                    model: qwen2.5-coder:7b
                """;
            case OLLAMA_CLOUD -> """
                  llm:
                    provider: OLLAMA_CLOUD
                    endpoint: https://votre-instance.cloud
                    api-key: votre-cle-api
                    model: gpt-oss:120b-cloud
                """;
            case LLM_STUDIO -> """
                  llm:
                    provider: LLM_STUDIO
                    endpoint: http://localhost:1234
                    model: votre-modele
                """;
            case OPENAI -> """
                  llm:
                    provider: OPENAI
                    api-key: sk-votre-cle
                    model: gpt-4
                """;
            case CUSTOM -> """
                  llm:
                    provider: CUSTOM
                    endpoint: http://votre-service:port
                    model: votre-modele
                """;
        };
    }

    private String maskApiKey(String apiKey) {
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
