package org.sid.mission_service.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:mission_security_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "KEYCLOAK_URL=http://localhost:8080",
        "KEYCLOAK_REALM=b2b-platform",
        "internal.service-token=test-internal-token",
        "payment-service.base-url=http://localhost:8088"
})
class MissionSecurityTest {

    private static final String MISSION_BODY = """
            {
              "companyId": 10,
              "title": "Build API",
              "description": "Build a secure backend API",
              "requiredSkills": ["Java", "Spring"],
              "durationDays": 20,
              "budget": 1000,
              "workMode": "REMOTE"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void getMissionsIsPublic() throws Exception {
        mockMvc.perform(get("/api/missions"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void createMissionWithoutAuthenticationIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MISSION_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createMissionWithInvalidInternalTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/missions")
                        .header("X-Internal-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MISSION_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createMissionWithValidInternalTokenIsAllowed() throws Exception {
        mockMvc.perform(post("/api/missions")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MISSION_BODY))
                .andExpect(status().isOk());
    }
}
