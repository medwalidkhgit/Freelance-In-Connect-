package org.sid.application_service.service;

import feign.FeignException;
import org.sid.application_service.client.CompanyServiceClient;
import org.sid.application_service.client.FreelancerServiceClient;
import org.sid.application_service.client.MissionServiceClient;
import org.sid.application_service.client.MessagingServiceClient;
import org.sid.application_service.dto.ApplicationRequest;
import org.sid.application_service.dto.ApplicationResponse;
import org.sid.application_service.dto.ApplicationStatusRequest;
import org.sid.application_service.dto.CompanyResponse;
import org.sid.application_service.dto.ConversationResponse;
import org.sid.application_service.dto.CreateConversationRequest;
import org.sid.application_service.dto.FreelancerProfileDTO;
import org.sid.application_service.dto.MessageRequest;
import org.sid.application_service.dto.MissionResponse;
import org.sid.application_service.dto.AssignFreelancerRequest;
import org.sid.application_service.entity.Application;
import org.sid.application_service.entity.ApplicationStatus;
import org.sid.application_service.repository.ApplicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class ApplicationService {

    private final ApplicationRepository repository;
    private final MissionServiceClient missionServiceClient;
    private final FreelancerServiceClient freelancerServiceClient;
    private final CompanyServiceClient companyServiceClient;
    private final MessagingServiceClient messagingServiceClient;

    public ApplicationService(ApplicationRepository repository,
                              MissionServiceClient missionServiceClient,
                              FreelancerServiceClient freelancerServiceClient,
                              CompanyServiceClient companyServiceClient,
                              MessagingServiceClient messagingServiceClient) {
        this.repository = repository;
        this.missionServiceClient = missionServiceClient;
        this.freelancerServiceClient = freelancerServiceClient;
        this.companyServiceClient = companyServiceClient;
        this.messagingServiceClient = messagingServiceClient;
    }

    public ApplicationResponse apply(Jwt jwt, ApplicationRequest request) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application payload is required");
        }
        if (request.getMissionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missionId is required");
        }

        MissionResponse mission = getPublishedMission(request.getMissionId());
        FreelancerProfileDTO freelancer = getCurrentFreelancer();
        String freelancerKeycloakId = jwt.getSubject();

        if (repository.existsByMissionIdAndFreelancerKeycloakId(mission.getId(), freelancerKeycloakId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Freelancer already applied to this mission");
        }

        Application application = Application.builder()
                .missionId(mission.getId())
                .missionCompanyId(mission.getCompanyId())
                .freelancerKeycloakId(freelancerKeycloakId)
                .freelancerId(freelancer.getId())
                .freelancerFullname(freelancer.displayName())
                .coverLetter(request.getCoverLetter())
                .compatibilityScore(calculateCompatibilityScore(freelancer, mission))
                .status(ApplicationStatus.PENDING)
                .build();

        Application saved = repository.save(application);
        createApplicationMessage(saved, mission, freelancer);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(Jwt jwt) {
        return repository.findByFreelancerKeycloakIdOrderByCreatedAtDesc(jwt.getSubject())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(Long id, Jwt jwt) {
        Application application = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        if (hasRole(jwt, "ADMIN")
                || application.getFreelancerKeycloakId().equals(jwt.getSubject())
                || (hasRole(jwt, "COMPANY") && ownsMission(application.getMissionCompanyId()))) {
            return toResponse(application);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this application");
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForMission(Long missionId) {
        MissionResponse mission = getPublishedMission(missionId);
        assertCurrentCompanyOwns(mission.getCompanyId());

        return repository.findByMissionIdOrderByCompatibilityScoreDescCreatedAtAsc(missionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForCurrentCompany() {
        CompanyResponse company = companyServiceClient.getMyCompany();
        return repository.findByMissionCompanyIdOrderByCreatedAtDesc(company.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean companyHasApplicationForFreelancer(Long freelancerId) {
        if (freelancerId == null) {
            return false;
        }
        CompanyResponse company = getCurrentCompany();
        return company != null
                && company.getId() != null
                && repository.existsByMissionCompanyIdAndFreelancerId(company.getId(), freelancerId);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAcceptedApplicationsForMission(Long missionId) {
        MissionResponse mission = getPublishedMission(missionId);
        assertCurrentCompanyOwns(mission.getCompanyId());

        return repository.findByMissionIdAndStatusOrderByCompatibilityScoreDescCreatedAtAsc(
                        missionId,
                        ApplicationStatus.ACCEPTED
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ApplicationResponse updateStatus(Long id, ApplicationStatusRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        if (request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        if (request.getStatus() == ApplicationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use ACCEPTED, REJECTED or WAITLISTED");
        }

        Application application = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        assertCurrentCompanyOwns(application.getMissionCompanyId());

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending applications can be updated");
        }
        if (request.getStatus() == ApplicationStatus.ACCEPTED
                && repository.existsByMissionIdAndStatus(application.getMissionId(), ApplicationStatus.ACCEPTED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mission already has an accepted application");
        }

        application.setStatus(request.getStatus());
        Application saved = repository.save(application);
        if (request.getStatus() == ApplicationStatus.ACCEPTED) {
            AssignFreelancerRequest assignRequest = new AssignFreelancerRequest();
            assignRequest.setFreelancerKeycloakId(application.getFreelancerKeycloakId());
            missionServiceClient.assignFreelancer(application.getMissionId(), assignRequest);
            sendAcceptanceMessage(saved);
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplicationsForAdmin() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private MissionResponse getPublishedMission(Long missionId) {
        try {
            MissionResponse mission = missionServiceClient.getMissionById(missionId);
            if (mission == null || !"PUBLIEE".equalsIgnoreCase(mission.getStatus())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Published mission not found");
            }
            return mission;
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Published mission not found");
        }
    }

    private FreelancerProfileDTO getCurrentFreelancer() {
        try {
            return freelancerServiceClient.getMyProfile();
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Freelancer profile not found");
        }
    }

    private void assertCurrentCompanyOwns(Long companyId) {
        if (!ownsMission(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mission does not belong to current company");
        }
    }

    private boolean ownsMission(Long companyId) {
        CompanyResponse company = getCurrentCompany();
        return company != null && company.getId() != null && company.getId().equals(companyId);
    }

    private CompanyResponse getCurrentCompany() {
        try {
            return companyServiceClient.getMyCompany();
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company profile not found");
        }
    }

    private int calculateCompatibilityScore(FreelancerProfileDTO freelancer, MissionResponse mission) {
        Set<String> freelancerSkills = normalize(freelancer.getSkills());
        Set<String> requiredSkills = normalize(mission.getRequiredSkills());
        if (requiredSkills.isEmpty()) {
            return 50;
        }

        long matched = requiredSkills.stream()
                .filter(freelancerSkills::contains)
                .count();
        int score = (int) Math.round((matched * 100.0) / requiredSkills.size());

        String summary = freelancer.getSummary() == null ? "" : freelancer.getSummary().toLowerCase(Locale.ROOT);
        for (String skill : requiredSkills) {
            if (summary.contains(skill)) {
                score = Math.min(100, score + 5);
            }
        }
        return score;
    }

    private void createApplicationMessage(Application application, MissionResponse mission, FreelancerProfileDTO freelancer) {
        try {
            if (application.getFreelancerId() == null) {
                return;
            }
            CreateConversationRequest conversationRequest = new CreateConversationRequest();
            conversationRequest.setMissionId(application.getMissionId());
            conversationRequest.setCompanyId(application.getMissionCompanyId());
            conversationRequest.setFreelancerId(application.getFreelancerId());
            conversationRequest.setCompanyKeycloakId(mission.getCompanyKeycloakId());
            conversationRequest.setFreelancerKeycloakId(application.getFreelancerKeycloakId());

            ConversationResponse conversation = messagingServiceClient.createConversation(conversationRequest);
            if (conversation == null || conversation.getId() == null) {
                return;
            }

            MessageRequest messageRequest = new MessageRequest();
            messageRequest.setConversationId(conversation.getId());
            messageRequest.setContent(applicationMessage(application, mission, freelancer));
            messagingServiceClient.sendMessage(messageRequest);
        } catch (Exception ignored) {
            // Messaging should not invalidate an otherwise valid application.
        }
    }

    private String applicationMessage(Application application, MissionResponse mission, FreelancerProfileDTO freelancer) {
        String coverLetter = application.getCoverLetter() == null || application.getCoverLetter().isBlank()
                ? "Candidature envoyee sans lettre de motivation."
                : application.getCoverLetter();
        return "Candidature pour la mission \"" + mission.getTitle() + "\" (#" + mission.getId() + ")\n"
                + "Profil: " + freelancer.displayName() + "\n\n"
                + coverLetter;
    }

    private void sendAcceptanceMessage(Application application) {
        try {
            MissionResponse mission = missionServiceClient.getMissionById(application.getMissionId());
            CompanyResponse company = getCurrentCompany();

            CreateConversationRequest conversationRequest = new CreateConversationRequest();
            conversationRequest.setMissionId(application.getMissionId());
            conversationRequest.setCompanyId(application.getMissionCompanyId());
            conversationRequest.setFreelancerId(application.getFreelancerId());
            conversationRequest.setCompanyKeycloakId(
                    mission.getCompanyKeycloakId() == null || mission.getCompanyKeycloakId().isBlank()
                            ? company.getKeycloakId()
                            : mission.getCompanyKeycloakId()
            );
            conversationRequest.setFreelancerKeycloakId(application.getFreelancerKeycloakId());

            ConversationResponse conversation = messagingServiceClient.createConversation(conversationRequest);
            if (conversation == null || conversation.getId() == null) {
                return;
            }

            String companyName = company.getCompanyName() == null || company.getCompanyName().isBlank()
                    ? "l'entreprise"
                    : company.getCompanyName();
            MessageRequest messageRequest = new MessageRequest();
            messageRequest.setConversationId(conversation.getId());
            messageRequest.setContent("Vous avez ete accepte pour la mission : "
                    + mission.getTitle()
                    + " de la societe "
                    + companyName
                    + ".");
            messagingServiceClient.sendMessage(messageRequest);
        } catch (Exception ignored) {
            // Acceptance remains valid even if the notification message cannot be sent.
        }
    }

    private Set<String> normalize(Collection<String> values) {
        if (values == null) {
            return Collections.emptySet();
        }
        Set<String> normalized = new HashSet<>();
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .forEach(normalized::add);
        return normalized;
    }

    private boolean hasRole(Jwt jwt, String role) {
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

    private ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getMissionId(),
                application.getMissionCompanyId(),
                application.getFreelancerId(),
                application.getFreelancerKeycloakId(),
                application.getFreelancerFullname(),
                application.getCoverLetter(),
                application.getCompatibilityScore(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
