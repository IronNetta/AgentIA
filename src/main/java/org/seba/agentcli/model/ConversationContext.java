package org.seba.agentcli.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the persistent conversation context for the CLI agent
 */
public class ConversationContext {
    
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;
    
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime lastAccessed;
    
    private String projectName;
    private String projectPath;
    private List<ConversationEntry> conversationHistory;
    private List<String> projectInsights;
    private List<String> decisionsMade;
    
    public ConversationContext() {
        this.createdAt = LocalDateTime.now();
        this.lastAccessed = LocalDateTime.now();
        this.conversationHistory = new ArrayList<>();
        this.projectInsights = new ArrayList<>();
        this.decisionsMade = new ArrayList<>();
    }
    
    public void addConversationEntry(String userQuery, String aiResponse) {
        this.conversationHistory.add(new ConversationEntry(userQuery, aiResponse, LocalDateTime.now()));
        updateLastAccessed();
    }
    
    public void addProjectInsight(String insight) {
        if (!this.projectInsights.contains(insight)) {
            this.projectInsights.add(insight);
        }
        updateLastAccessed();
    }
    
    public void addDecision(String decision) {
        if (!this.decisionsMade.contains(decision)) {
            this.decisionsMade.add(decision);
        }
        updateLastAccessed();
    }
    
    private void updateLastAccessed() {
        this.lastAccessed = LocalDateTime.now();
    }
    
    // Getters and setters
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastAccessed() {
        return lastAccessed;
    }
    
    public void setLastAccessed(LocalDateTime lastAccessed) {
        this.lastAccessed = lastAccessed;
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public String getProjectPath() {
        return projectPath;
    }
    
    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }
    
    public List<ConversationEntry> getConversationHistory() {
        return conversationHistory;
    }
    
    public void setConversationHistory(List<ConversationEntry> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }
    
    public List<String> getProjectInsights() {
        return projectInsights;
    }
    
    public void setProjectInsights(List<String> projectInsights) {
        this.projectInsights = projectInsights;
    }
    
    public List<String> getDecisionsMade() {
        return decisionsMade;
    }
    
    public void setDecisionsMade(List<String> decisionsMade) {
        this.decisionsMade = decisionsMade;
    }
}