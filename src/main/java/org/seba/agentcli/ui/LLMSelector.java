package org.seba.agentcli.ui;

import org.seba.agentcli.config.LLMConfig;
import org.seba.agentcli.io.ConsoleReader;
import org.seba.agentcli.model.LLMProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Component
public class LLMSelector {

    private final LLMConfig llmConfig;

    public LLMSelector(LLMConfig llmConfig) {
        this.llmConfig = llmConfig;
    }

    public void showSelectionMenu(ConsoleReader reader) {
        System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║           🤖 Configuration LLM - Agent CLI               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        // Check if config already exists in user home
        Path userConfig = getUserConfigPath();
        if (Files.exists(userConfig)) {
            System.out.println("📋 Configuration sauvegardée trouvée\n");
            System.out.println("1. Utiliser la config sauvegardée (" + llmConfig.getProviderEnum().getDisplayName() + ")");
            System.out.println("2. Changer de provider");
            System.out.println("3. Continuer sans changer\n");

            String choice = reader.readLine("Votre choix [1-3] (défaut: 1): ");
            if (choice == null) {
                System.out.println("\n❌ Annulé par l'utilisateur ou erreur de lecture\n");
                return;
            }
            choice = choice.trim();

            if (choice.isEmpty() || choice.equals("1")) {
                System.out.println("\n✓ Utilisation de la config sauvegardée\n");
                return;
            } else if (choice.equals("3")) {
                return;
            }
            // Si choix 2, continue vers le menu de sélection
        }

        // Show provider selection menu
        selectProvider(reader);
    }

    private void selectProvider(ConsoleReader reader) {
        System.out.println("\n📋 Sélectionnez votre provider LLM:\n");
        System.out.println("1. Ollama Local    (Gratuit, Privé, Recommandé)");
        System.out.println("2. LLM Studio      (GUI, Facile à utiliser)");
        System.out.println("3. Ollama Cloud    (Puissant, Modèles énormes)");
        System.out.println("4. OpenAI          (GPT-4, Payant)");
        System.out.println("5. Custom          (Votre propre service)");
        System.out.println();

        String choice = reader.readLine("Votre choix [1-5] (défaut: 1): ");
        if (choice == null) {
            System.out.println("\n❌ Annulé par l'utilisateur ou erreur de lecture\n");
            return;
        }
        choice = choice.trim();
        if (choice.isEmpty()) choice = "1";

        LLMProvider provider = switch (choice) {
            case "1" -> LLMProvider.OLLAMA_LOCAL;
            case "2" -> LLMProvider.LLM_STUDIO;
            case "3" -> LLMProvider.OLLAMA_CLOUD;
            case "4" -> LLMProvider.OPENAI;
            case "5" -> LLMProvider.CUSTOM;
            default -> {
                System.out.println("⚠️  Choix invalide, utilisation de Ollama Local");
                yield LLMProvider.OLLAMA_LOCAL;
            }
        };

        configureProvider(provider, reader);
    }

    private void configureProvider(LLMProvider provider, ConsoleReader reader) {
        System.out.println("\n🔧 Configuration de " + provider.getDisplayName() + "\n");

        String endpoint = llmConfig.getEndpoint();
        String model = llmConfig.getModel();
        String apiKey = llmConfig.getApiKey();

        // Endpoint
        if (provider.getDefaultEndpoint() != null) {
            String defaultEndpoint = provider.getDefaultEndpoint();
            String input = reader.readLine("Endpoint [" + defaultEndpoint + "]: ");
            if (input == null) {
                System.out.println("\n❌ Annulé par l'utilisateur ou erreur de lecture\n");
                return;
            }
            input = input.trim();
            endpoint = input.isEmpty() ? defaultEndpoint : input;
        } else {
            endpoint = reader.readLine("Endpoint: ");
            if (endpoint == null) {
                System.out.println("\n❌ Annulé par l'utilisateur ou erreur de lecture\n");
                return;
            }
            endpoint = endpoint.trim();
        }

        // Model
        String defaultModel = getDefaultModel(provider);
        String input = reader.readLine("Modèle [" + defaultModel + "]: ");
        if (input == null) {
            System.out.println("\n❌ Annulé par l'utilisateur ou erreur de lecture\n");
            return;
        }
        input = input.trim();
        model = input.isEmpty() ? defaultModel : input;

        // API Key (if needed)
        if (provider == LLMProvider.OLLAMA_CLOUD || provider == LLMProvider.OPENAI) {
            apiKey = reader.readLine("API Key: ", '*');
            if (apiKey == null) {
                System.out.println("\n❌ Annulé par l'utilisateur ou erreur de lecture\n");
                return;
            }
            apiKey = apiKey.trim();
        }

        // Update config
        llmConfig.setProvider(provider.name());
        llmConfig.setEndpoint(endpoint);
        llmConfig.setModel(model);
        if (apiKey != null && !apiKey.isEmpty()) {
            llmConfig.setApiKey(apiKey);
        }

        // Ask to save
        System.out.println();
        String save = reader.readLine("Sauvegarder cette configuration ? [O/n]: ");
        if (save == null) {
            System.out.println("\n❌ Annulé par l'utilisateur ou erreur de lecture\n");
            return;
        }
        save = save.trim().toLowerCase();
        if (save.isEmpty() || save.equals("o") || save.equals("y") || save.equals("oui") || save.equals("yes")) {
            saveConfiguration();
        }

        System.out.println("\n✓ Configuration appliquée: " + llmConfig.toString());
        showQuickHelp(provider);
    }

    private String getDefaultModel(LLMProvider provider) {
        return switch (provider) {
            case OLLAMA_LOCAL -> "qwen2.5-coder:7b";
            case OLLAMA_CLOUD -> "llama3.1:latest";  // Modèle par défaut Ollama Cloud
            case LLM_STUDIO -> "qwen2.5-coder-7b-instruct";
            case OPENAI -> "gpt-4";
            case CUSTOM -> "default-model";
        };
    }

    private void showQuickHelp(LLMProvider provider) {
        System.out.println("\n💡 Pour démarrer:\n");

        switch (provider) {
            case OLLAMA_LOCAL -> System.out.println("""
                1. Installez Ollama: curl -fsSL https://ollama.ai/install.sh | sh
                2. Démarrez: ollama serve
                3. Téléchargez le modèle: ollama pull qwen2.5-coder:7b
                4. Relancez Agent CLI
                """);
            case LLM_STUDIO -> System.out.println("""
                1. Téléchargez LLM Studio: https://lmstudio.ai/
                2. Installez et téléchargez un modèle
                3. Démarrez le serveur local dans l'app
                4. Relancez Agent CLI
                """);
            case OLLAMA_CLOUD -> System.out.println("""
                1. Créez votre compte sur: https://ollama.com
                2. Créez une clé API sur: https://ollama.com/settings/keys
                3. La clé est déjà configurée
                4. Relancez Agent CLI

                Documentation: https://docs.ollama.com/cloud
                """);
            case OPENAI -> System.out.println("""
                Votre clé API OpenAI est configurée.
                Attention: Service payant à l'usage.
                """);
            case CUSTOM -> System.out.println("""
                Assurez-vous que votre service est accessible.
                """);
        }
    }

    private void saveConfiguration() {
        try {
            Path userConfig = getUserConfigPath();
            Files.createDirectories(userConfig.getParent());

            StringBuilder config = new StringBuilder();
            config.append("# Agent CLI - Configuration LLM sauvegardée\n");
            config.append("# Générée automatiquement\n\n");
            config.append("llm:\n");
            config.append("  provider: ").append(llmConfig.getProvider()).append("\n");
            config.append("  endpoint: ").append(llmConfig.getEndpoint()).append("\n");
            config.append("  model: ").append(llmConfig.getModel()).append("\n");
            if (llmConfig.getApiKey() != null && !llmConfig.getApiKey().isEmpty()) {
                config.append("  api-key: ").append(llmConfig.getApiKey()).append("\n");
            }
            config.append("  timeout: ").append(llmConfig.getTimeout()).append("\n");

            Files.writeString(userConfig, config.toString());
            System.out.println("\n✓ Configuration sauvegardée dans: " + userConfig);
            System.out.println("  Pour changer: Supprimez ce fichier ou relancez avec --configure");

        } catch (IOException e) {
            System.err.println("⚠️  Impossible de sauvegarder: " + e.getMessage());
        }
    }

    private Path getUserConfigPath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".agentcli", "llm-config.yml");
    }

    public void loadSavedConfiguration() {
        Path userConfig = getUserConfigPath();
        if (Files.exists(userConfig)) {
            try {
                String content = Files.readString(userConfig);
                // Parse simple YAML (basique)
                Map<String, String> config = parseSimpleYaml(content);

                if (config.containsKey("provider")) {
                    llmConfig.setProvider(config.get("provider"));
                }
                if (config.containsKey("endpoint")) {
                    llmConfig.setEndpoint(config.get("endpoint"));
                }
                if (config.containsKey("model")) {
                    llmConfig.setModel(config.get("model"));
                }
                if (config.containsKey("api-key")) {
                    llmConfig.setApiKey(config.get("api-key"));
                }
                if (config.containsKey("timeout")) {
                    llmConfig.setTimeout(Integer.parseInt(config.get("timeout")));
                }

            } catch (Exception e) {
                System.err.println("⚠️  Erreur lors du chargement de la config sauvegardée: " + e.getMessage());
            }
        }
    }

    private Map<String, String> parseSimpleYaml(String yaml) {
        Map<String, String> result = new HashMap<>();
        String[] lines = yaml.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty() || line.equals("llm:")) {
                continue;
            }

            String[] parts = line.split(":", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                result.put(key, value);
            }
        }

        return result;
    }
}
