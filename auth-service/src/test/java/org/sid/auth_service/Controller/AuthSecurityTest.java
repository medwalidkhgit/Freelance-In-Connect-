package org.sid.auth_service.Controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sid.auth_service.Config.SecurityConfig;
import org.sid.auth_service.DTO.AuthRequest;
import org.sid.auth_service.DTO.CompanyRegisterRequest;
import org.sid.auth_service.DTO.FreelancerRegisterRequest;
import org.sid.auth_service.Service.AuthService;
import org.sid.auth_service.Service.KeycloakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeAuthService authService;

    @Autowired
    private FakeKeycloakService keycloakService;

    @BeforeEach
    void resetFakes() {
        authService.loginResponse = ResponseEntity.ok().build();
        authService.logoutResponse = ResponseEntity.noContent().build();
        authService.logoutRefreshToken = null;
        keycloakService.refreshResponse = ResponseEntity.ok().build();
    }

    @Test
    void loginIsPublic() throws Exception {
        authService.loginResponse = ResponseEntity.ok(Map.of("access_token", "access-token"));

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void refreshIsPublic() throws Exception {
        keycloakService.refreshResponse = ResponseEntity.ok(Map.of("access_token", "access-token"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void logoutWithRefreshTokenIsPublicAndDelegates() throws Exception {
        authService.logoutResponse = ResponseEntity.noContent().build();

        mockMvc.perform(post("/auth/logout")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(authService.logoutRefreshToken).isEqualTo("refresh-token");
    }

    @Test
    void logoutWithoutRefreshTokenIsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": " "
                                }
                                """))
                .andExpect(status().isBadRequest());

        org.assertj.core.api.Assertions.assertThat(authService.logoutRefreshToken).isNull();
    }

    @Test
    void unknownEndpointsAreDenied() throws Exception {
        mockMvc.perform(get("/internal/test"))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        FakeAuthService authService(FakeKeycloakService keycloakService) {
            return new FakeAuthService(keycloakService);
        }

        @Bean
        FakeKeycloakService keycloakService() {
            return new FakeKeycloakService();
        }
    }

    static class FakeAuthService extends AuthService {
        private ResponseEntity<?> loginResponse = ResponseEntity.ok().build();
        private ResponseEntity<?> logoutResponse = ResponseEntity.noContent().build();
        private String logoutRefreshToken;

        FakeAuthService(KeycloakService keycloakService) {
            super(keycloakService, null, null, null);
        }

        @Override
        public ResponseEntity<String> registerFreelancer(FreelancerRegisterRequest request) {
            return ResponseEntity.ok("ok");
        }

        @Override
        public ResponseEntity<String> registerCompany(CompanyRegisterRequest request) {
            return ResponseEntity.ok("ok");
        }

        @Override
        public ResponseEntity<?> login(AuthRequest request) {
            return loginResponse;
        }

        @Override
        public ResponseEntity<?> logout(String refreshToken) {
            this.logoutRefreshToken = refreshToken;
            return logoutResponse;
        }
    }

    static class FakeKeycloakService extends KeycloakService {
        private ResponseEntity<?> refreshResponse = ResponseEntity.ok().build();

        FakeKeycloakService() {
            super(null, new RestTemplate());
        }

        @Override
        public ResponseEntity<?> refreshToken(String refreshToken) {
            return refreshResponse;
        }
    }
}
