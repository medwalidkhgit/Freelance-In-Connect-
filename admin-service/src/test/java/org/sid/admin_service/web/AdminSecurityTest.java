package org.sid.admin_service.web;

import org.junit.jupiter.api.Test;
import org.sid.admin_service.Config.KeycloakJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "KEYCLOAK_URL=http://localhost:8080",
        "KEYCLOAK_REALM=b2b-platform",
        "SPRING_DATASOURCE_URL=jdbc:h2:mem:admin_security_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "SPRING_DATASOURCE_USERNAME=sa",
        "SPRING_DATASOURCE_PASSWORD=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "internal.service-token=test-internal-token",
        "services.company.url=http://localhost:8083",
        "services.freelancer.url=http://localhost:8082",
        "services.mission.url=http://localhost:8084"
})
@AutoConfigureMockMvc
class AdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpointWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointWithCompanyRoleIsForbidden() throws Exception {
        Jwt jwt = jwtWithRole("COMPANY");

        mockMvc.perform(get("/api/admin")
                        .with(jwt().jwt(jwt).authorities(converter.convert(jwt).getAuthorities())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointWithFreelancerRoleIsForbidden() throws Exception {
        Jwt jwt = jwtWithRole("FREELANCER");

        mockMvc.perform(get("/api/admin")
                        .with(jwt().jwt(jwt).authorities(converter.convert(jwt).getAuthorities())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointWithAdminRoleIsAllowed() throws Exception {
        Jwt jwt = jwtWithRole("ADMIN");

        mockMvc.perform(get("/api/admin")
                        .with(jwt().jwt(jwt).authorities(converter.convert(jwt).getAuthorities())))
                .andExpect(status().isOk());
    }

    @Test
    void unknownEndpointIsDenied() throws Exception {
        Jwt jwt = jwtWithRole("ADMIN");

        mockMvc.perform(get("/internal/test")
                        .with(jwt().jwt(jwt).authorities(converter.convert(jwt).getAuthorities())))
                .andExpect(status().isForbidden());
    }

    private static Jwt jwtWithRole(String role) {
        return Jwt.withTokenValue("token-" + role)
                .header("alg", "none")
                .subject("admin-subject")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("preferred_username", "admin")
                .claim("email", "admin@example.com")
                .claim("realm_access", Map.of("roles", List.of(role)))
                .build();
    }
}
