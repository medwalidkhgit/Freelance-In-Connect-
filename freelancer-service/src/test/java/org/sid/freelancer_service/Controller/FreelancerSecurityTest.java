package org.sid.freelancer_service.Controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sid.freelancer_service.Entity.Freelancer;
import org.sid.freelancer_service.Repository.FreelancerRepository;
import org.sid.freelancer_service.Service.MissionServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:freelancer_security_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "KEYCLOAK_URL=http://localhost:8080",
        "KEYCLOAK_REALM=b2b-platform",
        "internal.service-token=test-internal-token",
        "payment-service.base-url=http://localhost:8088",
        "payment-service.enabled=false",
        "services.mission.url=http://localhost:8084"
})
class FreelancerSecurityTest {

    private static final String CREATE_BODY = """
            {
              "keycloakUserId": "freelancer-1",
              "email": "freelancer@example.com",
              "firstName": "Freelancer",
              "lastName": "One"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FreelancerRepository freelancerRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private MissionServiceClient missionServiceClient;

    @BeforeEach
    void setUp() {
        freelancerRepository.deleteAll();
    }

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void createFreelancerWithoutAuthenticationIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/freelances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createFreelancerWithInvalidInternalTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/freelances")
                        .header("X-Internal-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createFreelancerWithValidInternalTokenIsAllowed() throws Exception {
        mockMvc.perform(post("/api/freelances")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void getMyProfileWithCompanyRoleIsForbidden() throws Exception {
        when(jwtDecoder.decode("company-token")).thenReturn(jwt("company-token", "company-1", "COMPANY"));

        mockMvc.perform(get("/api/freelances/me")
                        .header("Authorization", "Bearer company-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyProfileWithFreelancerRoleIsAllowed() throws Exception {
        freelancerRepository.save(freelancer("freelancer-1"));
        when(jwtDecoder.decode("freelancer-token")).thenReturn(jwt("freelancer-token", "freelancer-1", "FREELANCER"));

        mockMvc.perform(get("/api/freelances/me")
                        .header("Authorization", "Bearer freelancer-token"))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpointWithFreelancerRoleIsForbidden() throws Exception {
        when(jwtDecoder.decode("freelancer-token")).thenReturn(jwt("freelancer-token", "freelancer-1", "FREELANCER"));

        mockMvc.perform(get("/api/freelances/admin")
                        .header("Authorization", "Bearer freelancer-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointWithAdminRoleIsAllowed() throws Exception {
        when(jwtDecoder.decode("admin-token")).thenReturn(jwt("admin-token", "admin-1", "ADMIN"));

        mockMvc.perform(get("/api/freelances/admin")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    private Jwt jwt(String token, String subject, String role) {
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", subject)
                .claim("realm_access", Map.of("roles", List.of(role)))
                .issuer("http://localhost:8080/realms/b2b-platform")
                .build();
    }

    private Freelancer freelancer(String keycloakUserId) {
        Freelancer freelancer = new Freelancer();
        freelancer.setKeycloakUserId(keycloakUserId);
        freelancer.setEmail(keycloakUserId + "@example.com");
        freelancer.setFirstName("Freelancer");
        freelancer.setLastName("One");
        freelancer.setSuspended(false);
        return freelancer;
    }
}
