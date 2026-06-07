package org.sid.application_service.client;

import org.sid.application_service.dto.ConversationResponse;
import org.sid.application_service.dto.CreateConversationRequest;
import org.sid.application_service.dto.MessageRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "messaging-service", url = "${services.messaging.url:http://messaging-service:8087}")
public interface MessagingServiceClient {

    @PostMapping("/api/conversations")
    ConversationResponse createConversation(@RequestBody CreateConversationRequest request);

    @PostMapping("/api/messages")
    void sendMessage(@RequestBody MessageRequest request);
}
