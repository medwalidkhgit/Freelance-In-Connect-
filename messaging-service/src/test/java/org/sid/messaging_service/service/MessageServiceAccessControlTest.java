package org.sid.messaging_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sid.messaging_service.domain.Conversation;
import org.sid.messaging_service.domain.Message;
import org.sid.messaging_service.repository.MessageRepository;
import org.sid.messaging_service.security.MessagingUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageServiceAccessControlTest {

    private MessageRepository messageRepository;
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        messageService = new MessageService(messageRepository);
    }

    @Test
    void saveMessageUsesCompanyIdentityFromJwt() {
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message message = messageService.saveMessage(
                conversation("c1", 10L, 20L),
                MessagingUser.fromJwt(jwt("COMPANY", "companyId", 10L, "company-kc-1")),
                "Bonjour"
        );

        assertThat(message.getSenderId()).isEqualTo(10L);
        assertThat(message.getSenderKeycloakId()).isEqualTo("company-kc-1");
        assertThat(message.getSenderRole()).isEqualTo("COMPANY");
        assertThat(message.getContent()).isEqualTo("Bonjour");
    }

    @Test
    void saveMessageUsesFreelancerIdentityFromJwt() {
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message message = messageService.saveMessage(
                conversation("c1", 10L, 20L),
                MessagingUser.fromJwt(jwt("FREELANCER", "freelancerId", 20L, "freelancer-kc-1")),
                "Bonjour"
        );

        assertThat(message.getSenderId()).isEqualTo(20L);
        assertThat(message.getSenderKeycloakId()).isEqualTo("freelancer-kc-1");
        assertThat(message.getSenderRole()).isEqualTo("FREELANCER");
    }

    @Test
    void adminCannotSendAsConversationParticipant() {
        assertThatThrownBy(() -> messageService.saveMessage(
                conversation("c1", 10L, 20L),
                MessagingUser.fromJwt(jwt("ADMIN", "adminId", 1L, "admin-kc-1")),
                "Message admin"
        ))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private static Conversation conversation(String id, Long companyId, Long freelancerId) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setCompanyId(companyId);
        conversation.setFreelancerId(freelancerId);
        return conversation;
    }

    private static Jwt jwt(String role, String idClaim, Long id, String subject) {
        return Jwt.withTokenValue("token-" + role)
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim(idClaim, id)
                .claim("realm_access", Map.of("roles", List.of(role)))
                .build();
    }
}
