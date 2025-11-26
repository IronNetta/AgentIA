package org.seba.agentcli.model;

public enum LLMProvider {
    OLLAMA_LOCAL("Ollama Local", "http://localhost:11434"),
    OLLAMA_CLOUD("Ollama Cloud", "https://ollama.com"),  // Official Ollama Cloud
    LLM_STUDIO("LLM Studio", "http://localhost:1234"),
    OPENAI("OpenAI", "https://api.openai.com/v1"),
    CUSTOM("Custom", null);  // URL personnalisée

    private final String displayName;
    private final String defaultEndpoint;

    LLMProvider(String displayName, String defaultEndpoint) {
        this.displayName = displayName;
        this.defaultEndpoint = defaultEndpoint;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultEndpoint() {
        return defaultEndpoint;
    }
}
