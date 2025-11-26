package org.seba.agentcli.config;

import org.seba.agentcli.model.LLMProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "llm")
public class LLMConfig {

    private String provider = "OLLAMA_LOCAL";  // Par défaut
    private String endpoint;
    private String apiKey;
    private String model;
    private int timeout = 120;  // secondes
    private int maxRetries = 3;

    // Getters et Setters

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public LLMProvider getProviderEnum() {
        try {
            return LLMProvider.valueOf(provider.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return LLMProvider.OLLAMA_LOCAL;
        }
    }

    public String getEndpoint() {
        if (endpoint != null && !endpoint.isEmpty()) {
            return endpoint;
        }
        // Utiliser l'endpoint par défaut du provider
        LLMProvider prov = getProviderEnum();
        return prov.getDefaultEndpoint() != null ? prov.getDefaultEndpoint() : "http://localhost:11434";
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model != null ? model : "qwen2.5-coder:7b";
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public String toString() {
        return String.format("LLMConfig{provider=%s, endpoint=%s, model=%s}",
                getProviderEnum().getDisplayName(), getEndpoint(), getModel());
    }
}
