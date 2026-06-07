package org.sid.messaging_service.controller;

import org.junit.jupiter.api.Test;
import org.sid.messaging_service.domain.Conversation;
import org.sid.messaging_service.repository.ConversationRepository;
import org.sid.messaging_service.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/messaging_security_test?serverSelectionTimeoutMS=100",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/b2b-platform",
        "app.security.enabled=true",
        "management.health.mongo.enabled=false"
})
@AutoConfigureMockMvc
class MessagingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ConversationRepository conversationRepository;

    @MockitoBean
    private MessageRepository messageRepository;

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void conversationsWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conversationsWithUnauthorizedRoleIsForbidden() throws Exception {
        when(jwtDecoder.decode("user-token")).thenReturn(jwtWithRole("user-token", "user-kc-1", "USER"));

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void conversationsWithCompanyRoleIsAllowed() throws Exception {
        when(jwtDecoder.decode("company-token")).thenReturn(jwtWithRole("company-token", "company-kc-1", "COMPANY"));
        when(conversationRepository.findByCompanyKeycloakId("company-kc-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer company-token"))
                .andExpect(status().isOk());
    }

    @Test
    void messageHistoryWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/messages/conversation/c1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void messageHistoryWithUnauthorizedRoleIsForbidden() throws Exception {
        when(jwtDecoder.decode("user-token")).thenReturn(jwtWithRole("user-token", "user-kc-1", "USER"));

        mockMvc.perform(get("/api/messages/conversation/c1")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void messageHistoryWithParticipantRoleIsAllowed() throws Exception {
        when(jwtDecoder.decode("freelancer-token")).thenReturn(jwtWithRole("freelancer-token", "freelancer-kc-1", "FREELANCER"));
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdOrderBySentAtAsc("c1")).thenReturn(List.of());

        mockMvc.perform(get("/api/messages/conversation/c1")
                        .header("Authorization", "Bearer freelancer-token"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownEndpointIsDenied() throws Exception {
        when(jwtDecoder.decode("admin-token")).thenReturn(jwtWithRole("admin-token", "admin-kc-1", "ADMIN"));

        mockMvc.perform(get("/internal/test")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isForbidden());
    }

    private static Jwt jwtWithRole(String token, String subject, String role) {
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", subject)
                .claim("realm_access", Map.of("roles", List.of(role)))
                .issuer("http://localhost:8080/realms/b2b-platform")
                .build();
    }

    private static Conversation conversation() {
        Conversation conversation = new Conversation();
        conversation.setId("c1");
        conversation.setMissionId(1L);
        conversation.setCompanyId(10L);
        conversation.setFreelancerId(20L);
        conversation.setCompanyKeycloakId("company-kc-1");
        conversation.setFreelancerKeycloakId("freelancer-kc-1");
        return conversation;
    }
}
