package org.seba.agentcli;

import org.seba.agentcli.config.LLMConfig;
import org.seba.agentcli.context.ContextManager;
import org.seba.agentcli.model.ConversationContext;
import org.seba.agentcli.model.LLMProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;

@Service
public class CliService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final LLMConfig llmConfig;
    private final ContextManager contextManager;
    private org.seba.agentcli.context.PlanManager planManager;
    private final org.seba.agentcli.context.IncrementalContextLoader contextLoader;
    private ConversationContext conversationContext;
    private org.seba.agentcli.model.ProjectContext projectContext;
    private List<Integer> context = new ArrayList<>();

    public CliService(LLMConfig llmConfig,
                        ContextManager contextManager,
                        org.seba.agentcli.context.PlanManager planManager,
                        org.seba.agentcli.context.IncrementalContextLoader contextLoader) {
        this.llmConfig = llmConfig;
        this.contextManager = contextManager;
        this.planManager = planManager;
        this.contextLoader = contextLoader;
        this.webClient = WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
                .build();
        this.objectMapper = new ObjectMapper();

        System.out.println("LLM configured: " + llmConfig.toString());
    }

    public void initializeContext(String projectPath) {
        // Create a fresh context for this session, don't load from persistent storage
        this.conversationContext = new ConversationContext();
        this.conversationContext.setProjectPath(projectPath);
        this.conversationContext.setProjectName(new java.io.File(projectPath).getName());
        System.out.println("Session context initialized for project: " + conversationContext.getProjectName());
    }

    public void setProjectContext(org.seba.agentcli.model.ProjectContext projectContext) {
        this.projectContext = projectContext;
    }

    public org.seba.agentcli.model.ProjectContext getProjectContext() {
        return this.projectContext;
    }
    
    public String query(String prompt) {
        try {
            // Vérification pour éviter les boucles de répétition
            if (conversationContext != null && conversationContext.getConversationHistory() != null) {
                // Vérifier si la même question a été posée récemment (dernières 3 questions)
                int start = Math.max(0, conversationContext.getConversationHistory().size() - 3);
                for (int i = start; i < conversationContext.getConversationHistory().size(); i++) {
                    var entry = conversationContext.getConversationHistory().get(i);
                    if (entry.getUserQuery().equals(prompt)) {
                        return "Warning: The same question was asked recently. Please rephrase or ask a different question.";
                    }
                }
            }

            // Build a more contextual prompt using conversation history
            String contextualPrompt = buildContextualPrompt(prompt);

            String endpoint = llmConfig.getEndpoint();
            String apiPath = getApiPath();

            // Vérifier que l'endpoint est accessible avant d'envoyer la requête
            if (endpoint == null || endpoint.trim().isEmpty()) {
                return "Error: Incorrect LLM configuration - endpoint not defined";
            }

            Object requestBody;
            if (usesChatFormat()) {
                // Format Chat API (Ollama Cloud, LLM Studio, OpenAI)
                requestBody = new ChatRequest(
                        llmConfig.getModel(),
                        java.util.List.of(new ChatMessage("user", contextualPrompt)),
                        false
                );
            } else {
                // Format Generate API (Ollama Local)
                requestBody = new OllamaRequest(
                        llmConfig.getModel(),
                        contextualPrompt,
                        context,
                        false
                );
            }

            var requestSpec = webClient.post()
                    .uri(endpoint + apiPath)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody);

            // Ajouter l'API key si configurée
            if (llmConfig.getApiKey() != null && !llmConfig.getApiKey().isEmpty()) {
                requestSpec = requestSpec.header("Authorization", "Bearer " + llmConfig.getApiKey());
            }

            String responseText = null;
            if (usesChatFormat()) {
                // Response format pour Chat API
                ChatResponse chatResponse = requestSpec
                        .retrieve()
                        .bodyToMono(ChatResponse.class)
                        .timeout(Duration.ofSeconds(llmConfig.getTimeout()))
                        .block();

                if (chatResponse != null && chatResponse.message != null) {
                    responseText = chatResponse.message.content;
                }
            } else {
                // Response format pour Generate API
                OllamaResponse response = requestSpec
                        .retrieve()
                        .bodyToMono(OllamaResponse.class)
                        .timeout(Duration.ofSeconds(llmConfig.getTimeout()))
                        .block();

                if (response != null) {
                    this.context = response.context;
                    responseText = response.response;
                }
            }

            if (responseText != null) {
                // Vérifier si la réponse est vide ou répétitive
                if (responseText.trim().isEmpty()) {
                    return "Error: The LLM could not provide a useful response. Please rephrase your question.";
                }

                // Save the conversation to context
                saveConversationToContext(prompt, responseText);
                return responseText;
            } else {
                return "Error: No response from LLM";
            }

        } catch (Exception e) {
            // Améliorer le message d'erreur pour aider au diagnostic
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("timeout") || errorMessage.toLowerCase().contains("time"))) {
                return "Error: Response timeout. The LLM is taking too long to respond. Please try again.";
            } else if (errorMessage != null && errorMessage.contains("Connection refused")) {
                return "Error: Unable to connect to LLM service. Verify that " +
                       llmConfig.getProviderEnum().getDisplayName() +
                       " is correctly configured and running.";
            } else if (errorMessage != null && errorMessage.contains("404")) {
                return "Error 404: The LLM service URL is incorrect. Check your configuration.";
            } else if (errorMessage != null && (errorMessage.contains("401") || errorMessage.contains("403"))) {
                return "Error: Authentication failed. Check your API key for the LLM service.";
            } else {
                return "Error: Connection to LLM failed: " + e.getMessage() +
                       "\nVerify that the service is started and accessible.";
            }
        }
    }

    private String buildContextualPrompt(String currentPrompt) {
        if (conversationContext == null) {
            return addSystemInstructions(currentPrompt);
        }

        StringBuilder contextualPrompt = new StringBuilder();

        // Add system instructions for file operations
        contextualPrompt.append(getSystemInstructions()).append("\n\n");

        // Add project context if available
        if (projectContext != null) {
            contextualPrompt.append("PROJECT CONTEXT:\n");
            contextualPrompt.append("Type: ").append(projectContext.getProjectType().getDisplayName()).append("\n");
            contextualPrompt.append("Root: ").append(projectContext.getRootPath()).append("\n");

            if (!projectContext.getFrameworks().isEmpty()) {
                contextualPrompt.append("Frameworks: ").append(String.join(", ", projectContext.getFrameworks())).append("\n");
            }

            if (!projectContext.getSourceFiles().isEmpty()) {
                contextualPrompt.append("Files indexed: ").append(projectContext.getSourceFiles().size()).append("\n");
            }

            contextualPrompt.append("\n");

            // Load relevant file contexts incrementally
            org.seba.agentcli.context.IncrementalContextLoader.ContextBundle bundle =
                    contextLoader.loadRelevantContext(currentPrompt, projectContext);

            if (!bundle.getFiles().isEmpty()) {
                contextualPrompt.append(bundle.formatForPrompt(projectContext.getRootPath()));
                contextualPrompt.append("\n");
            }
        }

        // Add current plan if available
        if (planManager != null && planManager.hasPlan()) {
            contextualPrompt.append(planManager.getPlanSummaryForLLM()).append("\n");
        }

        // Add project insights if available
        if (conversationContext.getProjectInsights() != null && !conversationContext.getProjectInsights().isEmpty()) {
            contextualPrompt.append("Project insights: ")
                           .append(String.join("; ", conversationContext.getProjectInsights()))
                           .append("\n\n");
        }

        // Add decisions made if available
        if (conversationContext.getDecisionsMade() != null && !conversationContext.getDecisionsMade().isEmpty()) {
            contextualPrompt.append("Previous decisions: ")
                           .append(String.join("; ", conversationContext.getDecisionsMade()))
                           .append("\n\n");
        }

        // Add recent conversation history (last 5 interactions)
        if (conversationContext.getConversationHistory() != null && !conversationContext.getConversationHistory().isEmpty()) {
            int start = Math.max(0, conversationContext.getConversationHistory().size() - 5);
            contextualPrompt.append("Recent conversation history:\n");
            for (int i = start; i < conversationContext.getConversationHistory().size(); i++) {
                var entry = conversationContext.getConversationHistory().get(i);
                contextualPrompt.append("User: ").append(entry.getUserQuery()).append("\n");
                contextualPrompt.append("Assistant: ").append(entry.getAiResponse()).append("\n\n");
            }
            contextualPrompt.append("Current question: ").append(currentPrompt).append("\n");
        } else {
            // If no history, just return the current prompt
            contextualPrompt.append(currentPrompt);
        }

        return contextualPrompt.toString();
    }

    /**
     * Ajoute les instructions système pour les opérations de fichiers
     */
    private String addSystemInstructions(String prompt) {
        return getSystemInstructions() + "\n\n" + prompt;
    }

    /**
     * Retourne les instructions système pour le LLM
     */
    private String getSystemInstructions() {
        return """
        Tu es un assistant de développement professionnel et méthodique, similaire à Claude Code.

        PHILOSOPHIE:
        - Sois prudent, réfléchi et professionnel
        - NE génère du code que si explicitement demandé
        - Planifie les tâches complexes avant d'agir
        - Lis toujours un fichier avant de le modifier
        - Demande des clarifications si nécessaire
        - Adopte un ton professionnel et objectif
        - Évite les emojis excessifs (sauf si l'utilisateur le demande)

        OUTILS DISPONIBLES:
        L'utilisateur peut utiliser ces commandes directement:
        - @read <fichier> : Lire un fichier avec numérotation
        - @write <fichier> : Créer/écraser un fichier
        - @edit <fichier> : Modifier une partie d'un fichier
        - @search <terme> : Rechercher dans le projet
        - @tree : Voir l'arborescence
        - @analyze-project : Analyser l'architecture
        - @plan : Créer et gérer des plans de tâches
        - @help : Liste complète des commandes

        TASK PLANNING SYSTEM:
        For complex tasks (3+ steps), you can use the planning system:
        - If a plan is active, you'll see it in the CURRENT PLAN section above
        - Reference the plan in your responses (e.g., "As per step 2 of the plan...")
        - Remind the user to update plan progress with @plan complete <N>
        - If no plan exists but task is complex, suggest creating one
        - Example: "This task is complex. I recommend using @plan create to organize the work."

        COMPORTEMENT POUR LES MODIFICATIONS DE FICHIERS:

        PRINCIPE CLÉ: SOIS PROACTIF ET AUTONOME
        - Quand l'utilisateur demande quelque chose, FAIS-LE directement
        - N'attends PAS de confirmation supplémentaire
        - Ne demande PAS "Voulez-vous que je..." ou "Souhaitez-vous..."
        - AGIS immédiatement selon la demande

        1. Si l'utilisateur pose une QUESTION THÉORIQUE:
           - Réponds avec l'explication
           - N'implémente PAS de code sauf s'il le demande

           Exemple:
           User: "Comment créer une classe User?"
           Assistant: "Pour créer une classe User en Java:
           1. Définir les champs avec annotations JPA
           2. Ajouter getters/setters
           3. Implémenter equals/hashCode"

        2. Si l'utilisateur demande de CRÉER/MODIFIER du code:
           - CRÉE IMMÉDIATEMENT le code avec les balises XML
           - N'attends PAS de confirmation
           - Explique brièvement ce que tu as créé

           Exemple:
           User: "Crée la classe User"
           Assistant: "Je crée la classe User:

           <file path="src/model/User.java">
           [code complet]
           </file>

           ✓ Classe User créée avec champs name et email."

        3. Si l'utilisateur dit "ok", "oui", "continue", "la suite":
           - CONTINUE IMMÉDIATEMENT la tâche en cours
           - Ne demande PAS ce qu'il faut faire
           - Passe à l'ÉTAPE SUIVANTE du plan ou de la tâche

           Exemple:
           User: "ok"
           Assistant: [Crée directement le fichier suivant avec les balises XML]

        4. Pour les PLANS EN COURS:
           - Référence le plan ("Étape X/Y: ...")
           - CRÉE DIRECTEMENT les fichiers nécessaires
           - Ne demande PAS "Voulez-vous que je procède?"
           - FAIS-LE automatiquement

        RÈGLE D'OR:
        - Si l'utilisateur demande = TU FAIS
        - Si l'utilisateur dit "oui"/"ok"/"continue" = TU CONTINUES
        - JAMAIS de "Voulez-vous que je..." sauf si vraiment ambiguë

        5. Pour MODIFIER un fichier existant:
           - Mentionne que le fichier devrait être lu d'abord si ce n'est pas déjà fait
           - Suggère l'utilisation de @read si nécessaire
           - Utilise <edit> pour des modifications précises

           Exemple:
           User: "Ajoute un champ 'age' à User"
           Assistant: "Pour ajouter le champ 'age', je dois d'abord voir le contenu actuel.
           Si vous n'avez pas encore affiché le fichier, utilisez: @read src/model/User.java

           Sinon, voici les modifications à appliquer:

           <edit file="src/model/User.java" old="private String email;" new="private String email;
               private int age;"/>

           Note: Il faudra aussi ajouter les getters/setters pour age et mettre à jour le constructeur."

        BALISES XML POUR LES OPÉRATIONS:

        1. CRÉER/ÉCRASER un fichier:
           <file path="chemin/relatif/fichier.java">
           [contenu complet du fichier]
           </file>

        2. ÉDITER une partie d'un fichier:
           <edit file="chemin/relatif/fichier.java" old="texte exact à remplacer" new="nouveau texte"/>

        3. SUPPRIMER un fichier:
           <delete file="chemin/relatif/fichier.java"/>

        RÈGLES IMPORTANTES:
        - Utilise des chemins relatifs depuis la racine du projet
        - N'utilise PAS de balises markdown (```) dans les balises XML
        - Pour <edit>, le texte "old" doit matcher EXACTEMENT ce qui existe dans le fichier
        - Sois précis et complet dans le code généré
        - Préfère suggérer l'utilisation des outils (@read, @write, @edit) quand approprié

        CONTEXTE DU PROJET:
        Tu as accès au contexte du projet (type, frameworks, fichiers).
        Utilise ces informations pour donner des réponses pertinentes et adaptées.

        ERREURS ET BLOCAGES:
        - Si tu ne peux pas faire quelque chose, explique pourquoi clairement
        - Suggère des alternatives
        - Demande plus d'informations si nécessaire
        - Reste toujours professionnel et objectif
        """;
    }

    private void saveConversationToContext(String prompt, String response) {
        if (conversationContext != null) {
            conversationContext.addConversationEntry(prompt, response);
            // For session-only memory, we don't auto-save to persistent storage
            // The context only lives during the session
        }
    }

    public void addProjectInsight(String insight) {
        if (conversationContext != null) {
            conversationContext.addProjectInsight(insight);
        }
    }

    public void addDecision(String decision) {
        if (conversationContext != null) {
            conversationContext.addDecision(decision);
        }
    }

    public void clearConversationContext() {
        if (conversationContext != null) {
            // Create a fresh context during the same session
            String projectPath = conversationContext.getProjectPath();
            String projectName = conversationContext.getProjectName();
            this.conversationContext = new ConversationContext();
            this.conversationContext.setProjectPath(projectPath);
            this.conversationContext.setProjectName(projectName);
        }
    }

    public ConversationContext getConversationContext() {
        return conversationContext;
    }

    private String getApiPath() {
        LLMProvider provider = llmConfig.getProviderEnum();
        return switch (provider) {
            case OLLAMA_LOCAL -> "/api/generate";
            case OLLAMA_CLOUD -> "/api/chat";  // Ollama Cloud uses chat API
            case LLM_STUDIO -> "/v1/chat/completions";  // API compatible OpenAI
            case OPENAI -> "/chat/completions";
            case CUSTOM -> "/api/generate";  // Par défaut
        };
    }

    private boolean usesChatFormat() {
        LLMProvider provider = llmConfig.getProviderEnum();
        return provider == LLMProvider.OLLAMA_CLOUD ||
               provider == LLMProvider.LLM_STUDIO ||
               provider == LLMProvider.OPENAI;
    }
    
    public void clearContext() {
        this.context.clear();
    }
    
    // DTOs internes - Generate API (Ollama Local)
    static class OllamaRequest {
        public String model;
        public String prompt;
        public List<Integer> context;
        public boolean stream;

        public OllamaRequest(String model, String prompt, List<Integer> context, boolean stream) {
            this.model = model;
            this.prompt = prompt;
            this.context = context;
            this.stream = stream;
        }
    }

    static class OllamaResponse {
        public String response;
        public List<Integer> context;
    }

    // DTOs pour Chat API (Ollama Cloud, LLM Studio, OpenAI)
    static class ChatRequest {
        public String model;
        public java.util.List<ChatMessage> messages;
        public boolean stream;

        public ChatRequest(String model, java.util.List<ChatMessage> messages, boolean stream) {
            this.model = model;
            this.messages = messages;
            this.stream = stream;
        }
    }

    static class ChatMessage {
        public String role;
        public String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    static class ChatResponse {
        public ChatMessage message;
        public String created_at;
        public boolean done;
    }
}