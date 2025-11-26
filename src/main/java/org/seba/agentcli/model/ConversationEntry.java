package org.seba.agentcli.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;

/**
 * Represents a single entry in the conversation history
 */
public class ConversationEntry {
    private String userQuery;
    private String aiResponse;
    
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;
    
    public ConversationEntry(String userQuery, String aiResponse, LocalDateTime timestamp) {
        this.userQuery = userQuery;
        this.aiResponse = aiResponse;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public String getUserQuery() {
        return userQuery;
    }
    
    public void setUserQuery(String userQuery) {
        this.userQuery = userQuery;
    }
    
    public String getAiResponse() {
        return aiResponse;
    }
    
    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}