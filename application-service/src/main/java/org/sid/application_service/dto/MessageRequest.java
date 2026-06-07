package org.sid.application_service.dto;

import lombok.Data;

@Data
public class MessageRequest {
    private String conversationId;
    private String content;
}
