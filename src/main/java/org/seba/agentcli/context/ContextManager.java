package org.seba.agentcli.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.seba.agentcli.model.ConversationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages the persistent conversation context for the CLI agent
 */
@Component
public class ContextManager {
    
    private static final String CONTEXT_DIR_NAME = ".agentcli_context";
    private static final String CONTEXT_FILE_NAME = "conversation_context.json";
    
    private final ObjectMapper objectMapper;
    
    public ContextManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Loads the conversation context for the current project
     * @param projectPath The path to the current project
     * @return The conversation context, or a new one if none exists
     */
    public ConversationContext loadContext(String projectPath) {
        try {
            Path contextDir = Paths.get(projectPath, CONTEXT_DIR_NAME);
            Path contextFile = contextDir.resolve(CONTEXT_FILE_NAME);
            
            if (Files.exists(contextFile)) {
                ConversationContext context = objectMapper.readValue(contextFile.toFile(), ConversationContext.class);
                context.setProjectPath(projectPath);
                return context;
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load conversation context: " + e.getMessage());
            // Return a new context if loading fails
        }
        
        // Create a new context if none exists
        ConversationContext newContext = new ConversationContext();
        newContext.setProjectPath(projectPath);
        newContext.setProjectName(getProjectName(projectPath));
        return newContext;
    }
    
    /**
     * Saves the conversation context for the current project
     * @param context The conversation context to save
     * @param projectPath The path to the current project
     */
    public void saveContext(ConversationContext context, String projectPath) {
        try {
            Path contextDir = Paths.get(projectPath, CONTEXT_DIR_NAME);
            
            // Create the context directory if it doesn't exist
            if (!Files.exists(contextDir)) {
                Files.createDirectories(contextDir);
            }
            
            Path contextFile = contextDir.resolve(CONTEXT_FILE_NAME);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(contextFile.toFile(), context);
        } catch (IOException e) {
            System.err.println("Warning: Could not save conversation context: " + e.getMessage());
        }
    }
    
    /**
     * Clears the conversation context for the current project
     * @param projectPath The path to the current project
     */
    public void clearContext(String projectPath) {
        try {
            Path contextDir = Paths.get(projectPath, CONTEXT_DIR_NAME);
            Path contextFile = contextDir.resolve(CONTEXT_FILE_NAME);
            
            if (Files.exists(contextFile)) {
                Files.delete(contextFile);
            }
            
            // Delete the context directory if it's empty
            if (Files.exists(contextDir) && isDirectoryEmpty(contextDir)) {
                Files.delete(contextDir);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not clear conversation context: " + e.getMessage());
        }
    }
    
    private String getProjectName(String projectPath) {
        Path path = Paths.get(projectPath);
        return path.getFileName().toString();
    }
    
    private boolean isDirectoryEmpty(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return !files.findFirst().isPresent();
        }
    }
}