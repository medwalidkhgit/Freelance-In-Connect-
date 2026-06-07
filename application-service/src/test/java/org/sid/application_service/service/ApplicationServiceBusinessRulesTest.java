package org.sid.application_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sid.application_service.client.CompanyServiceClient;
import org.sid.application_service.client.FreelancerServiceClient;
import org.sid.application_service.client.MissionServiceClient;
import org.sid.application_service.dto.ApplicationRequest;
import org.sid.application_service.dto.ApplicationStatusRequest;
import org.sid.application_service.dto.AssignFreelancerRequest;
import org.sid.application_service.dto.CompanyResponse;
import org.sid.application_service.dto.FreelancerProfileDTO;
import org.sid.application_service.dto.MissionResponse;
import org.sid.application_service.entity.Application;
import org.sid.application_service.entity.ApplicationStatus;
import org.sid.application_service.repository.ApplicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceBusinessRulesTest {

    @Mock
    private ApplicationRepository repository;

    @Mock
    private MissionServiceClient missionServiceClient;

    @Mock
    private FreelancerServiceClient freelancerServiceClient;

    @Mock
    private CompanyServiceClient companyServiceClient;

    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationService(
                repository,
                missionServiceClient,
                freelancerServiceClient,
                companyServiceClient
        );
    }

    @Test
    void applyToNonPublishedMissionIsRejected() {
        ApplicationRequest request = applicationRequest();
        MissionResponse mission = mission();
        mission.setStatus("EN_COURS");
        when(missionServiceClient.getMissionById(1L)).thenReturn(mission);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.apply(jwt("freelancer-1", "FREELANCER"), request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(Application.class));
    }

    @Test
    void duplicateApplicationIsRejected() {
        when(missionServiceClient.getMissionById(1L)).thenReturn(mission());
        when(freelancerServiceClient.getMyProfile()).thenReturn(freelancer());
        when(repository.existsByMissionIdAndFreelancerKeycloakId(1L, "freelancer-1")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.apply(jwt("freelancer-1", "FREELANCER"), applicationRequest())
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(Application.class));
    }

    @Test
    void nonPendingApplicationCannotBeUpdated() {
        Application application = application(ApplicationStatus.REJECTED);
        when(repository.findById(1L)).thenReturn(Optional.of(application));
        when(companyServiceClient.getMyCompany()).thenReturn(company(10L));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.updateStatus(1L, statusRequest(ApplicationStatus.ACCEPTED))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(repository, never()).save(application);
        verify(missionServiceClient, never()).assignFreelancer(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(AssignFreelancerRequest.class));
    }

    @Test
    void companyCannotUpdateApplicationForOtherCompanyMission() {
        Application application = application(ApplicationStatus.PENDING);
        when(repository.findById(1L)).thenReturn(Optional.of(application));
        when(companyServiceClient.getMyCompany()).thenReturn(company(99L));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.updateStatus(1L, statusRequest(ApplicationStatus.ACCEPTED))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(repository, never()).save(application);
    }

    @Test
    void acceptingWhenMissionAlreadyHasAcceptedApplicationIsRejected() {
        Application application = application(ApplicationStatus.PENDING);
        when(repository.findById(1L)).thenReturn(Optional.of(application));
        when(companyServiceClient.getMyCompany()).thenReturn(company(10L));
        when(repository.existsByMissionIdAndStatus(1L, ApplicationStatus.ACCEPTED)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.updateStatus(1L, statusRequest(ApplicationStatus.ACCEPTED))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(repository, never()).save(application);
        verify(missionServiceClient, never()).assignFreelancer(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(AssignFreelancerRequest.class));
    }

    @Test
    void acceptingPendingApplicationSavesAndAssignsFreelancer() {
        Application application = application(ApplicationStatus.PENDING);
        when(repository.findById(1L)).thenReturn(Optional.of(application));
        when(companyServiceClient.getMyCompany()).thenReturn(company(10L));
        when(repository.existsByMissionIdAndStatus(1L, ApplicationStatus.ACCEPTED)).thenReturn(false);
        when(repository.save(application)).thenReturn(application);

        applicationService.updateStatus(1L, statusRequest(ApplicationStatus.ACCEPTED));

        assertEquals(ApplicationStatus.ACCEPTED, application.getStatus());
        verify(repository).save(application);
        verify(missionServiceClient).assignFreelancer(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(AssignFreelancerRequest.class)
        );
    }

    private ApplicationRequest applicationRequest() {
        ApplicationRequest request = new ApplicationRequest();
        request.setMissionId(1L);
        request.setCoverLetter("I can do this mission");
        return request;
    }

    private ApplicationStatusRequest statusRequest(ApplicationStatus status) {
        ApplicationStatusRequest request = new ApplicationStatusRequest();
        request.setStatus(status);
        return request;
    }

    private Application application(ApplicationStatus status) {
        return Application.builder()
                .id(1L)
                .missionId(1L)
                .missionCompanyId(10L)
                .freelancerKeycloakId("freelancer-1")
                .freelancerFullname("Freelancer One")
                .coverLetter("I can do this mission")
                .compatibilityScore(80)
                .status(status)
                .build();
    }

    private MissionResponse mission() {
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

    private CompanyResponse company(Long id) {
        CompanyResponse company = new CompanyResponse();
        company.setId(id);
        company.setStatus("Validated");
        return company;
    }

    private Jwt jwt(String subject, String role) {
        return Jwt.withTokenValue(subject + "-token")
                .header("alg", "none")
                .subject(subject)
                .issuer("http://localhost:8080/realms/b2b-platform")
                .issuedAt(Instant.now())
                .claim("realm_access", Map.of("roles", List.of(role)))
                .build();
    }
}
