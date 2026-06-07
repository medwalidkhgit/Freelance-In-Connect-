package org.sid.messaging_service.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.sid.messaging_service.domain.Conversation;
import org.sid.messaging_service.dto.ConversationResponse;
import org.sid.messaging_service.dto.CreateConversationRequest;
import org.sid.messaging_service.security.MessagingUser;
import org.sid.messaging_service.service.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse create(@Valid @RequestBody CreateConversationRequest request,
                                       @AuthenticationPrincipal Jwt jwt) {
        Conversation conversation = conversationService.createOrGet(
            request.getMissionId(), request.getCompanyId(), request.getFreelancerId(),
            request.getCompanyKeycloakId(), request.getFreelancerKeycloakId(),
            MessagingUser.fromJwt(jwt)
        );
        return toResponse(conversation);
    }

    @GetMapping
    public List<ConversationResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return conversationService.findVisibleFor(MessagingUser.fromJwt(jwt)).stream().map(this::toResponse).toList();
    }

    private ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
            conversation.getId().toString(),
            conversation.getMissionId(),
            conversation.getCompanyId(),
            conversation.getFreelancerId(),
            conversation.getCompanyKeycloakId(),
            conversation.getFreelancerKeycloakId(),
            conversation.getCreatedAt()
        );
    }
}
