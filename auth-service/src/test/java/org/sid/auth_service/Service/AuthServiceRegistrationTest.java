package org.sid.auth_service.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sid.auth_service.Client.CompanyServiceClient;
import org.sid.auth_service.Client.FreelancerServiceClient;
import org.sid.auth_service.DTO.AuthRequest;
import org.sid.auth_service.DTO.CompanyRegisterRequest;
import org.sid.auth_service.DTO.CompanyRequest;
import org.sid.auth_service.DTO.CompanyResponse;
import org.sid.auth_service.DTO.FreelancerRegisterRequest;
import org.sid.auth_service.DTO.FreelancerRequest;
import org.sid.auth_service.DTO.FreelancerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegistrationTest {

    @Mock
    private FreelancerServiceClient freelancerServiceClient;

    @Mock
    private CompanyServiceClient companyServiceClient;

    private FakeKeycloakService keycloakService;
    private FakeCompensationService compensationService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        keycloakService = new FakeKeycloakService();
        compensationService = new FakeCompensationService(keycloakService);
        authService = new AuthService(
                keycloakService,
                freelancerServiceClient,
                companyServiceClient,
                compensationService
        );
    }

    @Test
    void registerFreelancerCreatesBusinessProfileAfterKeycloakUser() {
        keycloakService.createdUserId = "kc-user-1";
        when(freelancerServiceClient.createFreelancer(any(FreelancerRequest.class)))
                .thenReturn(ResponseEntity.ok(new FreelancerResponse()));

        ResponseEntity<String> response = authService.registerFreelancer(validFreelancerRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(keycloakService.lastRoleName).isEqualTo("FREELANCER");
        assertThat(compensationService.called).isFalse();

        ArgumentCaptor<FreelancerRequest> captor = ArgumentCaptor.forClass(FreelancerRequest.class);
        verify(freelancerServiceClient).createFreelancer(captor.capture());
        assertThat(captor.getValue().getKeycloakUserId()).isEqualTo("kc-user-1");
        assertThat(captor.getValue().getEmail()).isEqualTo("freelancer@example.com");
    }

    @Test
    void registerFreelancerCompensatesWhenBusinessProfileFails() {
        keycloakService.createdUserId = "kc-user-1";
        when(freelancerServiceClient.createFreelancer(any(FreelancerRequest.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        ResponseEntity<String> response = authService.registerFreelancer(validFreelancerRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(compensationService.called).isTrue();
        assertThat(compensationService.userId).isEqualTo("kc-user-1");
        assertThat(compensationService.email).isEqualTo("freelancer@example.com");
    }

    @Test
    void registerFreelancerCompensatesWhenBusinessProfileCallThrows() {
        keycloakService.createdUserId = "kc-user-1";
        when(freelancerServiceClient.createFreelancer(any(FreelancerRequest.class)))
                .thenThrow(new RuntimeException("freelancer-service unavailable"));

        ResponseEntity<String> response = authService.registerFreelancer(validFreelancerRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(compensationService.called).isTrue();
        assertThat(compensationService.userId).isEqualTo("kc-user-1");
        assertThat(compensationService.email).isEqualTo("freelancer@example.com");
    }

    @Test
    void registerCompanyCompensatesWhenBusinessProfileFails() {
        keycloakService.createdUserId = "kc-company-1";
        when(companyServiceClient.createCompany(any(CompanyRequest.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).build());

        ResponseEntity<String> response = authService.registerCompany(validCompanyRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(keycloakService.lastRoleName).isEqualTo("COMPANY");
        assertThat(compensationService.called).isTrue();
        assertThat(compensationService.userId).isEqualTo("kc-company-1");
        assertThat(compensationService.email).isEqualTo("company@example.com");
    }

    @Test
    void registerCompanyDoesNotCallBusinessServiceWhenKeycloakFails() {
        keycloakService.createdUserId = null;

        ResponseEntity<String> response = authService.registerCompany(validCompanyRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(compensationService.called).isFalse();
        verifyNoInteractions(companyServiceClient);
        verify(freelancerServiceClient, never()).createFreelancer(any());
    }

    private static FreelancerRegisterRequest validFreelancerRequest() {
        FreelancerRegisterRequest request = new FreelancerRegisterRequest();
        request.setEmail("freelancer@example.com");
        request.setPassword("password123");
        request.setFirstName("Free");
        request.setLastName("Lancer");
        request.setPhone("+33123456789");
        request.setSummary("Backend developer");
        return request;
    }

    private static CompanyRegisterRequest validCompanyRequest() {
        CompanyRegisterRequest request = new CompanyRegisterRequest();
        request.setEmail("company@example.com");
        request.setPassword("password123");
        request.setCompanyName("SubIT");
        request.setSiret("12345678901234");
        request.setContactFirstName("Com");
        request.setContactLastName("Pany");
        return request;
    }

    private static class FakeKeycloakService extends KeycloakService {
        private String createdUserId;
        private String lastRoleName;

        FakeKeycloakService() {
            super(null, new RestTemplate());
        }

        @Override
        public String createUser(String email, String password, String firstName, String lastName, String roleName) {
            this.lastRoleName = roleName;
            return createdUserId;
        }

        @Override
        public ResponseEntity<?> login(AuthRequest request) {
            return ResponseEntity.ok().build();
        }

        @Override
        public ResponseEntity<?> logout(String refreshToken) {
            return ResponseEntity.noContent().build();
        }

        @Override
        public boolean deleteUser(String userId) {
            return true;
        }
    }

    private static class FakeCompensationService extends CompensationService {
        private boolean called;
        private String userId;
        private String email;

        FakeCompensationService(KeycloakService keycloakService) {
            super(keycloakService);
        }

        @Override
        public void compensate(String userId, String email) {
            this.called = true;
            this.userId = userId;
            this.email = email;
        }
    }
}
