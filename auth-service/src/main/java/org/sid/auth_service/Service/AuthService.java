package org.sid.auth_service.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.sid.auth_service.Client.CompanyServiceClient;
import org.sid.auth_service.Client.FreelancerServiceClient;
import org.sid.auth_service.DTO.AuthRequest;
import org.sid.auth_service.DTO.AuthResponse;
import org.sid.auth_service.DTO.CompanyRegisterRequest;
import org.sid.auth_service.DTO.CompanyRequest;
import org.sid.auth_service.DTO.CompanyResponse;
import org.sid.auth_service.DTO.FreelancerRegisterRequest;
import org.sid.auth_service.DTO.FreelancerRequest;
import org.sid.auth_service.DTO.FreelancerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
public class AuthService {

    private final KeycloakService keycloakService;
    private final FreelancerServiceClient freelancerServiceClient;
    private final CompanyServiceClient companyServiceClient;
    private final CompensationService compensationService;
    private final LoginOtpService loginOtpService;
    private final ObjectMapper objectMapper;

    public AuthService(KeycloakService keycloakService,
                       FreelancerServiceClient freelancerServiceClient,
                       CompanyServiceClient companyServiceClient,
                       CompensationService compensationService,
                       LoginOtpService loginOtpService,
                       ObjectMapper objectMapper) {
        this.keycloakService = keycloakService;
        this.freelancerServiceClient = freelancerServiceClient;
        this.companyServiceClient = companyServiceClient;
        this.compensationService = compensationService;
        this.loginOtpService = loginOtpService;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<String> registerFreelancer(FreelancerRegisterRequest request) {
        String userId = keycloakService.createUser(
                request.getEmail(), request.getPassword(),
                request.getFirstName(), request.getLastName(),
                "FREELANCER"
        );

        if (userId == null) {
            return ResponseEntity.badRequest()
                    .body("Email deja utilise ou erreur lors de la creation du compte.");
        }

        try {
            FreelancerRequest profileData = new FreelancerRequest(
                    userId, request.getEmail(),
                    request.getFirstName(), request.getLastName(),
                    request.getPhone(), request.getSummary(),
                    request.getCvUrl(), request.getPfpUrl()
            );

            ResponseEntity<FreelancerResponse> response =
                    freelancerServiceClient.createFreelancer(profileData);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Freelancer {} enregistre avec succes (Keycloak ID: {})",
                        request.getEmail(), userId);
                return ResponseEntity.ok("Inscription reussie");
            }

            log.error("Freelancer-service a retourne {} pour {} - compensation Keycloak",
                    response.getStatusCode(), request.getEmail());
            compensationService.compensate(userId, request.getEmail());
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de la creation du profil. Veuillez reessayer.");

        } catch (Exception e) {
            log.error("Erreur appel freelancer-service pour {} : {}", request.getEmail(), e.getMessage());
            compensationService.compensate(userId, request.getEmail());
            return ResponseEntity.internalServerError()
                    .body("Erreur technique. Veuillez reessayer.");
        }
    }

    public ResponseEntity<String> registerCompany(CompanyRegisterRequest request) {
        String userId = keycloakService.createUser(
                request.getEmail(), request.getPassword(),
                request.getContactFirstName(), request.getContactLastName(),
                "COMPANY"
        );

        if (userId == null) {
            return ResponseEntity.badRequest()
                    .body("Email deja utilise ou erreur lors de la creation du compte.");
        }

        try {
            CompanyRequest companyData = new CompanyRequest(
                    userId, request.getEmail(),
                    request.getCompanyName(), request.getSiret(),
                    request.getContactFirstName(),
                    request.getContactLastName(),
                    request.getCompanyAddress(),
                    request.getCompanyPhone(),
                    request.getDomaine(),
                    "PENDING",
                    request.getPfpUrl()
            );

            ResponseEntity<CompanyResponse> response =
                    companyServiceClient.createCompany(companyData);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Entreprise {} enregistree (Keycloak ID: {}), en attente de validation.",
                        request.getCompanyName(), userId);
                return ResponseEntity.ok("Inscription reussie. En attente de confirmation par l'administrateur.");
            }

            log.error("Company-service a retourne {} pour {} - compensation Keycloak",
                    response.getStatusCode(), request.getEmail());
            compensationService.compensate(userId, request.getEmail());
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de la creation du profil entreprise. Veuillez reessayer.");

        } catch (FeignException e) {
            log.error("Erreur appel company-service pour {} : {}", request.getEmail(), e.getMessage());
            compensationService.compensate(userId, request.getEmail());
            if (e.status() == HttpStatus.CONFLICT.value()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(feignBodyOrDefault(e, "Email deja utilise."));
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Erreur lors de la creation du profil entreprise. Veuillez reessayer.");
        } catch (Exception e) {
            log.error("Erreur appel company-service pour {} : {}", request.getEmail(), e.getMessage());
            compensationService.compensate(userId, request.getEmail());
            return ResponseEntity.internalServerError()
                    .body("Erreur technique. Veuillez reessayer.");
        }
    }

    public ResponseEntity<?> login(AuthRequest request) {
        ResponseEntity<?> loginResponse = keycloakService.login(request);
        if (!loginResponse.getStatusCode().is2xxSuccessful() || !(loginResponse.getBody() instanceof AuthResponse auth)) {
            return loginResponse;
        }

        if (!hasRealmRole(auth.getAccessToken(), "COMPANY")) {
            return loginOtpService.createChallenge(request.getEmail(), auth);
        }

        try {
            ResponseEntity<CompanyResponse> companyResponse =
                    companyServiceClient.getMyCompany("Bearer " + auth.getAccessToken());
            CompanyResponse company = companyResponse.getBody();
            String status = company == null ? null : company.getStatus();

            if (!isValidatedCompany(status)) {
                keycloakService.logout(auth.getRefreshToken());
                if ("Pending".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Compte non verifie par l'administrateur, veuillez patienter.");
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Compte entreprise non autorise. Contactez l'administrateur.");
            }

            return loginOtpService.createChallenge(request.getEmail(), auth);
        } catch (Exception e) {
            log.warn("Login company refuse pour {}: verification statut impossible: {}",
                    request.getEmail(), e.getMessage());
            keycloakService.logout(auth.getRefreshToken());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Compte entreprise introuvable ou non verifie par l'administrateur.");
        }
    }

    public ResponseEntity<?> logout(String refreshToken) {
        return keycloakService.logout(refreshToken);
    }

    private boolean isValidatedCompany(String status) {
        return "Validated".equalsIgnoreCase(status) || "VALIDATED".equalsIgnoreCase(status);
    }

    private String feignBodyOrDefault(FeignException e, String fallback) {
        String body = e.contentUTF8();
        if (body == null || body.isBlank()) {
            return fallback;
        }
        return body;
    }

    private boolean hasRealmRole(String accessToken, String roleName) {
        try {
            String[] chunks = accessToken.split("\\.");
            if (chunks.length < 2) {
                return false;
            }
            String payload = new String(Base64.getUrlDecoder().decode(chunks[1]), StandardCharsets.UTF_8);
            JsonNode roles = objectMapper.readTree(payload).path("realm_access").path("roles");
            if (!roles.isArray()) {
                return false;
            }
            for (JsonNode role : roles) {
                if (roleName.equalsIgnoreCase(role.asText())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("Impossible de lire les roles du token Keycloak: {}", e.getMessage());
            return false;
        }
    }
}
