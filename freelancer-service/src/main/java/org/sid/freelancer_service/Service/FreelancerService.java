package org.sid.freelancer_service.Service;

import org.sid.freelancer_service.DTO.FreelancerRequest;
import org.sid.freelancer_service.DTO.FreelancerResponse;
import org.sid.freelancer_service.DTO.FreelancerAdminDTO;
import org.sid.freelancer_service.DTO.FreelancerProfileDTO;
import org.sid.freelancer_service.DTO.FreelancerUpdateRequest;
import org.sid.freelancer_service.DTO.KeycloakProfileUpdateRequest;
import org.sid.freelancer_service.DTO.MissionResponse;
import org.sid.freelancer_service.DTO.WorkMode;
import org.sid.freelancer_service.DTO.StripeAccountOnboardingRequest;
import org.sid.freelancer_service.DTO.StripeAccountOnboardingResponse;
import org.sid.freelancer_service.Entity.Freelancer;
import org.sid.freelancer_service.Repository.FreelancerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FreelancerService {

    private final FreelancerRepository repository;
    private final MissionServiceClient missionServiceClient;
    private final AuthServiceClient authServiceClient;
    private final ApplicationServiceClient applicationServiceClient;
    private final RestTemplate restTemplate;
    private final String paymentServiceBaseUrl;
    private final boolean paymentServiceEnabled;

    private static final int MAX_PAGE_SIZE = 100;

    public FreelancerService(FreelancerRepository repository,
                             MissionServiceClient missionServiceClient,
                             AuthServiceClient authServiceClient,
                             ApplicationServiceClient applicationServiceClient,
                             RestTemplate restTemplate,
                             @Value("${payment-service.base-url}") String paymentServiceBaseUrl,
                             @Value("${payment-service.enabled:false}") boolean paymentServiceEnabled) {
        this.repository = repository;
        this.missionServiceClient = missionServiceClient;
        this.authServiceClient = authServiceClient;
        this.applicationServiceClient = applicationServiceClient;
        this.restTemplate = restTemplate;
        this.paymentServiceBaseUrl = paymentServiceBaseUrl;
        this.paymentServiceEnabled = paymentServiceEnabled;
    }

    // ✅ Remplace getAllProfiles() — paginé, trié par lastName
    public Page<Freelancer> getAllProfiles(int page, int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        return repository.findBySuspendedFalse(
                PageRequest.of(page, safeSize, Sort.by("lastName").ascending())
        );
    }

    public List<FreelancerAdminDTO> getAllProfilesForAdmin() {
        return repository.findAll().stream()
                .map(this::toAdminDto)
                .toList();
    }

    public boolean emailExists(String email) {
        return email != null && repository.findByEmail(email).isPresent();
    }

    public Freelancer findByKeycloakUserId(String keycloakUserId) {
        return repository.findByKeycloakUserId(keycloakUserId);
    }

    public Optional<Freelancer> getProfileByKeycloakId(String keycloakUserId) {
        return repository.findByKeycloakUserIdAndSuspendedFalse(keycloakUserId);
    }

    public Optional<Freelancer> getProfile(Long id) {
        return repository.findByIdAndSuspendedFalse(id);
    }

    public FreelancerProfileDTO getProfileForViewer(Long id, Jwt jwt) {
        Freelancer freelancer = repository.findByIdAndSuspendedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Freelancer profile not found"));

        if (hasRole(jwt, "ADMIN")) {
            return toProfileDto(freelancer);
        }
        if (hasRole(jwt, "FREELANCER") && jwt.getSubject().equals(freelancer.getKeycloakUserId())) {
            return toProfileDto(freelancer);
        }
        if (hasRole(jwt, "COMPANY") && companyHasApplicationForFreelancer(id)) {
            return toProfileDto(freelancer);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this freelancer profile");
    }

    public boolean profileExists(Long id) {
        return repository.existsById(id);
    }

    public Freelancer saveProfile(Freelancer profile) {
        return repository.save(profile);
    }

    public Freelancer updateMyProfile(String keycloakUserId, FreelancerUpdateRequest request) {
        validateUpdateRequest(request);
        Freelancer existing = repository.findByKeycloakUserIdAndSuspendedFalse(keycloakUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Freelancer profile not found"));

        existing.setFirstName(request.getFirstName());
        existing.setLastName(request.getLastName());
        existing.setPhone(request.getPhone());
        existing.setSummary(request.getSummary());
        existing.setCvUrl(request.getCvUrl());
        existing.setPfpUrl(request.getPfpUrl());
        existing.setSkills(request.getSkills());
        existing.setExperiences(request.getExperiences());
        existing.setProjects(request.getProjects());
        Freelancer saved = repository.save(existing);
        syncKeycloakProfile(saved);
        return saved;
    }

    @Transactional
    public FreelancerResponse createFreelancer(FreelancerRequest request) {
        validateCreateRequest(request);
        if (repository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already used");
        }
        if (repository.existsByKeycloakUserId(request.getKeycloakUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Keycloak user already linked");
        }

        Freelancer freelancer = new Freelancer();
        freelancer.setKeycloakUserId(request.getKeycloakUserId());
        freelancer.setEmail(request.getEmail());
        freelancer.setFirstName(request.getFirstName());
        freelancer.setLastName(request.getLastName());
        freelancer.setPhone(request.getPhone());
        freelancer.setSummary(request.getSummary());
        freelancer.setCvUrl(request.getCvUrl());
        freelancer.setPfpUrl(request.getPfpUrl());
        freelancer.setSkills(new ArrayList<>());
        freelancer.setExperiences(new ArrayList<>());
        freelancer.setProjects(new ArrayList<>());

        Freelancer saved = repository.save(freelancer);
        StripeAccountOnboardingResponse stripeResponse = null;
        if (paymentServiceEnabled) {
            stripeResponse = createStripeAccount(saved);
            saved.setStripeAccountId(stripeResponse.getAccountId());
            repository.save(saved);
        }

        FreelancerResponse response = new FreelancerResponse();
        response.setId(saved.getId());
        response.setKeycloakUserId(saved.getKeycloakUserId());
        response.setEmail(saved.getEmail());
        response.setFirstName(saved.getFirstName());
        response.setLastName(saved.getLastName());
        if (stripeResponse != null) {
            response.setStripeAccountId(stripeResponse.getAccountId());
            response.setStripeOnboardingUrl(stripeResponse.getOnboardingUrl());
        }
        return response;
    }

    private StripeAccountOnboardingResponse createStripeAccount(Freelancer freelancer) {
        String url = paymentServiceBaseUrl + "/api/stripe/accounts/freelancers/" + freelancer.getKeycloakUserId();
        StripeAccountOnboardingRequest payload = new StripeAccountOnboardingRequest();
        payload.setEmail(freelancer.getEmail());
        StripeAccountOnboardingResponse response = restTemplate.postForObject(
                url, payload, StripeAccountOnboardingResponse.class
        );
        if (response == null || response.getAccountId() == null || response.getOnboardingUrl() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe account creation failed");
        }
        return response;
    }

    public void deleteProfile(Long id) {
        repository.deleteById(id);
    }

    public void suspendProfile(Long id) {
        repository.findById(id).ifPresent(profile -> {
            profile.setSuspended(true);
            repository.save(profile);
        });
    }

    public Freelancer updateProfileById(Long id, FreelancerUpdateRequest request) {
        validateUpdateRequest(request);
        Freelancer existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Freelancer not found"));

        existing.setFirstName(request.getFirstName());
        existing.setLastName(request.getLastName());
        existing.setPhone(request.getPhone());
        existing.setSummary(request.getSummary());
        existing.setCvUrl(request.getCvUrl());
        existing.setPfpUrl(request.getPfpUrl());
        existing.setSkills(request.getSkills());
        existing.setExperiences(request.getExperiences());
        existing.setProjects(request.getProjects());
        Freelancer saved = repository.save(existing);
        syncKeycloakProfile(saved);
        return saved;
    }

    public FreelancerProfileDTO toProfileDto(Freelancer freelancer) {
        return new FreelancerProfileDTO(
                freelancer.getId(),
                freelancer.getKeycloakUserId(),
                freelancer.getFirstName(),
                freelancer.getLastName(),
                freelancer.getEmail(),
                freelancer.getPhone(),
                freelancer.getFirstName() + " " + freelancer.getLastName(),
                freelancer.getSummary(),
                freelancer.getCvUrl(),
                freelancer.getPfpUrl(),
                freelancer.getSkills(),
                freelancer.getExperiences(),
                freelancer.getProjects()
        );
    }

    private void syncKeycloakProfile(Freelancer freelancer) {
        try {
            authServiceClient.updateKeycloakProfile(
                    freelancer.getKeycloakUserId(),
                    new KeycloakProfileUpdateRequest(
                            freelancer.getFirstName(),
                            freelancer.getLastName(),
                            freelancer.getPhone()
                    )
            );
        } catch (Exception ignored) {
            // The business profile remains the source of truth; Keycloak sync is retried on the next update.
        }
    }

    private boolean companyHasApplicationForFreelancer(Long freelancerId) {
        try {
            return Boolean.TRUE.equals(applicationServiceClient.companyHasApplicationForFreelancer(freelancerId));
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasRole(Jwt jwt, String role) {
        if (jwt == null) {
            return false;
        }
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return false;
        }
        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof Collection<?> roles)) {
            return false;
        }
        return roles.stream().anyMatch(item -> role.equals(item));
    }

    private FreelancerAdminDTO toAdminDto(Freelancer freelancer) {
        return new FreelancerAdminDTO(
                freelancer.getId(),
                freelancer.getKeycloakUserId(),
                freelancer.getFirstName() + " " + freelancer.getLastName(),
                freelancer.getEmail(),
                freelancer.isSuspended()
        );
    }

    public List<MissionResponse> getAllMissions() {
        return missionServiceClient.getAllMissions();
    }

    public MissionResponse getMissionById(Long id) {
        return missionServiceClient.getMissionById(id);
    }

    public List<MissionResponse> getMissionsPublished() {
        return missionServiceClient.getMissionsPublished();
    }

    public List<MissionResponse> searchMissions(String skill, String keyword, WorkMode workMode) {
        return missionServiceClient.searchMissions(skill, keyword, workMode);
    }

    public List<MissionResponse> getMissionsByCompany(Long companyId) {
        return missionServiceClient.getMissionsByCompany(companyId);
    }

    private void validateCreateRequest(FreelancerRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Freelancer payload is required");
        }
        if (request.getKeycloakUserId() == null || request.getKeycloakUserId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "keycloakUserId is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "firstName is required");
        }
        if (request.getLastName() == null || request.getLastName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lastName is required");
        }
    }

    private void validateUpdateRequest(FreelancerUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Freelancer update payload is required");
        }
    }
}
