package org.sid.messaging_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sid.messaging_service.domain.Conversation;
import org.sid.messaging_service.repository.ConversationRepository;
import org.sid.messaging_service.security.MessagingUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationServiceAccessControlTest {

    private ConversationRepository conversationRepository;
    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        conversationService = new ConversationService(conversationRepository);
    }

    @Test
    void companySeesOnlyItsConversations() {
        MessagingUser user = MessagingUser.fromJwt(jwt("COMPANY", "companyId", 10L, "company-kc-1"));
        when(conversationRepository.findByCompanyKeycloakId("company-kc-1"))
                .thenReturn(List.of(conversation("c1", 1L, 10L, 20L, "company-kc-1", "freelancer-kc-1")));

        List<Conversation> conversations = conversationService.findVisibleFor(user);

        assertThat(conversations).hasSize(1);
        verify(conversationRepository).findByCompanyKeycloakId("company-kc-1");
    }

    @Test
    void freelancerSeesOnlyItsConversations() {
        MessagingUser user = MessagingUser.fromJwt(jwt("FREELANCER", "freelancerId", 20L, "freelancer-kc-1"));
        when(conversationRepository.findByFreelancerKeycloakId("freelancer-kc-1"))
                .thenReturn(List.of(conversation("c1", 1L, 10L, 20L, "company-kc-1", "freelancer-kc-1")));

        List<Conversation> conversations = conversationService.findVisibleFor(user);

        assertThat(conversations).hasSize(1);
        verify(conversationRepository).findByFreelancerKeycloakId("freelancer-kc-1");
    }

    @Test
    void adminCanSeeAllConversations() {
        MessagingUser user = MessagingUser.fromJwt(jwt("ADMIN", "adminId", 1L, "admin-kc-1"));
        when(conversationRepository.findAll()).thenReturn(List.of(conversation("c1", 1L, 10L, 20L)));

        List<Conversation> conversations = conversationService.findVisibleFor(user);

        assertThat(conversations).hasSize(1);
        verify(conversationRepository).findAll();
    }

    @Test
    void companyCannotAccessAnotherCompanyConversation() {
        MessagingUser user = MessagingUser.fromJwt(jwt("COMPANY", "companyId", 99L, "company-kc-99"));
        when(conversationRepository.findById("c1"))
                .thenReturn(Optional.of(conversation("c1", 1L, 10L, 20L, "company-kc-1", "freelancer-kc-1")));

        assertThatThrownBy(() -> conversationService.getVisibleConversation("c1", user))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void freelancerCannotCreateConversationForAnotherFreelancer() {
        MessagingUser user = MessagingUser.fromJwt(jwt("FREELANCER", "freelancerId", 99L, "freelancer-kc-99"));

        assertThatThrownBy(() -> conversationService.createOrGet(1L, 10L, 20L, "company-kc-1", "freelancer-kc-1", user))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    private static Conversation conversation(String id, Long missionId, Long companyId, Long freelancerId) {
        return conversation(id, missionId, companyId, freelancerId, null, null);
    }

    private static Conversation conversation(String id, Long missionId, Long companyId, Long freelancerId,
                                             String companyKeycloakId, String freelancerKeycloakId) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setMissionId(missionId);
        conversation.setCompanyId(companyId);
        conversation.setFreelancerId(freelancerId);
        conversation.setCompanyKeycloakId(companyKeycloakId);
        conversation.setFreelancerKeycloakId(freelancerKeycloakId);
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
