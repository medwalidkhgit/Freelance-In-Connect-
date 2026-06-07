package org.sid.messaging_service.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.sid.messaging_service.domain.Conversation;
import org.sid.messaging_service.domain.Message;
import org.sid.messaging_service.dto.MessageRequest;
import org.sid.messaging_service.dto.MessageResponse;
import org.sid.messaging_service.security.MessagingUser;
import org.sid.messaging_service.service.ConversationService;
import org.sid.messaging_service.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(
            MessageService messageService,
            ConversationService conversationService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping({"/conversation/{conversationId}", "/conversations/{conversationId}"})
    public List<MessageResponse> history(@PathVariable String conversationId, @AuthenticationPrincipal Jwt jwt) {
        conversationService.getVisibleConversation(conversationId, MessagingUser.fromJwt(jwt));
        return messageService.getHistory(conversationId).stream().map(this::toResponse).toList();
    }

    @PostMapping
    public MessageResponse sendRestMessage(@Valid @RequestBody MessageRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        MessagingUser user = MessagingUser.fromJwt(jwt);
        Conversation conversation = conversationService.getVisibleConversation(request.getConversationId(), user);

        Message saved = messageService.saveMessage(conversation, user, request.getContent());
        MessageResponse response = toResponse(saved);

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversation.getId(),
                response
        );

        return response;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Valid @Payload MessageRequest request, Principal principal) {
        MessagingUser user = MessagingUser.fromPrincipal(principal);
        Conversation conversation = conversationService.getVisibleConversation(request.getConversationId(), user);

        Message saved = messageService.saveMessage(conversation, user, request.getContent());

        MessageResponse response = toResponse(saved);
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversation.getId(),
                response
        );
    }

    private MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getSenderKeycloakId(),
                message.getSenderRole(),
                message.getContent(),
                message.getSentAt()
        );
    }
}
