package org.sid.application_service.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sid.application_service.client.CompanyServiceClient;
import org.sid.application_service.client.FreelancerServiceClient;
import org.sid.application_service.client.MissionServiceClient;
import org.sid.application_service.dto.FreelancerProfileDTO;
import org.sid.application_service.dto.MissionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:application_security_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "KEYCLOAK_URL=http://localhost:8080",
        "KEYCLOAK_REALM=b2b-platform",
        "internal.service-token=test-internal-token",
        "services.mission.url=http://localhost:8084",
        "services.freelancer.url=http://localhost:8082",
        "services.company.url=http://localhost:8083"
})
class ApplicationSecurityTest {

    private static final String APPLICATION_BODY = """
            {
              "missionId": 1,
              "coverLetter": "I can do this mission"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private MissionServiceClient missionServiceClient;

    @MockitoBean
    private FreelancerServiceClient freelancerServiceClient;

    @MockitoBean
    private CompanyServiceClient companyServiceClient;

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void applyWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void applyWithCompanyRoleIsForbidden() throws Exception {
        when(jwtDecoder.decode("company-token")).thenReturn(jwt("company-token", "company-1", "COMPANY"));

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer company-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void applyWithFreelancerRoleIsAllowed() throws Exception {
        when(jwtDecoder.decode("freelancer-token")).thenReturn(jwt("freelancer-token", "freelancer-1", "FREELANCER"));
        when(missionServiceClient.getMissionById(1L)).thenReturn(publishedMission());
        when(freelancerServiceClient.getMyProfile()).thenReturn(freelancer());

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer freelancer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void adminEndpointWithFreelancerRoleIsForbidden() throws Exception {
        when(jwtDecoder.decode("freelancer-token")).thenReturn(jwt("freelancer-token", "freelancer-1", "FREELANCER"));

        mockMvc.perform(get("/api/applications/admin/all")
                        .header("Authorization", "Bearer freelancer-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointWithAdminRoleIsAllowed() throws Exception {
        when(jwtDecoder.decode("admin-token")).thenReturn(jwt("admin-token", "admin-1", "ADMIN"));

        mockMvc.perform(get("/api/applications/admin/all")
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

    private MissionResponse publishedMission() {
        MissionResponse mission = new MissionResponse();
        mission.setId(1L);
        mission.setCompanyId(10L);
        mission.setStatus("PUBLIEE");
        mission.setRequiredSkills(List.of("Java", "Spring"));
        return mission;
    }

    private FreelancerProfileDTO freelancer() {
        FreelancerProfileDTO freelancer = new FreelancerProfileDTO();
        freelancer.setFullname("Freelancer One");
        freelancer.setSkills(List.of("Java", "Spring"));
        return freelancer;
    }
}
