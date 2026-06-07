package org.sid.messaging_service.dto;

import java.time.Instant;

public class MessageResponse {

    private String id;
    private String conversationId;
    private Long senderId;
    private String senderKeycloakId;
    private String senderRole;
    private String content;
    private Instant sentAt;

    public MessageResponse() {
    }

    public MessageResponse(String id, String conversationId, Long senderId, String senderRole, String content, Instant sentAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.content = content;
        this.sentAt = sentAt;
    }

    public MessageResponse(String id, String conversationId, Long senderId, String senderKeycloakId,
                           String senderRole, String content, Instant sentAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderKeycloakId = senderKeycloakId;
        this.senderRole = senderRole;
        this.content = content;
        this.sentAt = sentAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderKeycloakId() {
        return senderKeycloakId;
    }

    public void setSenderKeycloakId(String senderKeycloakId) {
        this.senderKeycloakId = senderKeycloakId;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
