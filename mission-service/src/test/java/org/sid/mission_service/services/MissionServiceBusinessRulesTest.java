package org.sid.mission_service.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sid.mission_service.dto.AssignFreelancerRequest;
import org.sid.mission_service.entities.Mission;
import org.sid.mission_service.entities.MissionStatus;
import org.sid.mission_service.entities.WorkMode;
import org.sid.mission_service.repositories.MissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MissionServiceBusinessRulesTest {

    @Mock
    private MissionRepository missionRepository;

    private MissionService missionService;

    @BeforeEach
    void setUp() {
        missionService = new MissionService(missionRepository, new RestTemplate(), "http://payment-service:8088");
    }

    @Test
    void createMissionWithoutCompanyIdIsRejected() {
        Mission mission = validMission();
        mission.setCompanyId(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> missionService.createMission(mission, "company-keycloak-id")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(missionRepository, never()).save(mission);
    }

    @Test
    void updatePublishedMissionIsRejected() {
        Mission existing = validMission();
        existing.setStatus(MissionStatus.PUBLIEE);
        Mission updated = validMission();
        when(missionRepository.findById(1L)).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> missionService.updateMission(1L, updated)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(missionRepository, never()).save(existing);
    }

    @Test
    void deleteStartedMissionIsRejected() {
        Mission existing = validMission();
        existing.setStatus(MissionStatus.EN_COURS);
        when(missionRepository.findById(1L)).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> missionService.deleteMission(1L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(missionRepository, never()).delete(existing);
    }

    @Test
    void startMissionWithoutAssignedFreelancerIsRejected() {
        Mission existing = validMission();
        existing.setStatus(MissionStatus.PUBLIEE);
        when(missionRepository.findById(1L)).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> missionService.demarrerMission(1L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(missionRepository, never()).save(existing);
    }

    @Test
    void assignFreelancerToDraftMissionIsRejected() {
        Mission existing = validMission();
        existing.setStatus(MissionStatus.BROUILLON);
        AssignFreelancerRequest request = assignFreelancerRequest();
        when(missionRepository.findById(1L)).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> missionService.assignFreelancer(1L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(missionRepository, never()).save(existing);
    }

    @Test
    void assignFreelancerToAlreadyAssignedMissionIsRejected() {
        Mission existing = validMission();
        existing.setStatus(MissionStatus.PUBLIEE);
        existing.setFreelancerKeycloakId("freelancer-1");
        AssignFreelancerRequest request = assignFreelancerRequest();
        when(missionRepository.findById(1L)).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> missionService.assignFreelancer(1L, request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(missionRepository, never()).save(existing);
    }

    @Test
    void assignFreelancerToPublishedMissionIsAllowed() {
        Mission existing = validMission();
        existing.setStatus(MissionStatus.PUBLIEE);
        AssignFreelancerRequest request = assignFreelancerRequest();
        when(missionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(missionRepository.save(existing)).thenReturn(existing);

        Mission saved = missionService.assignFreelancer(1L, request).orElseThrow();

        assertEquals("freelancer-1", saved.getFreelancerKeycloakId());
        verify(missionRepository).save(existing);
    }

    private Mission validMission() {
        return Mission.builder()
                .id(1L)
                .companyId(10L)
                .companyKeycloakId("company-1")
                .title("Build API")
                .description("Build a secure backend API")
                .requiredSkills(List.of("Java", "Spring"))
                .durationDays(20)
                .budget(BigDecimal.valueOf(1000))
                .workMode(WorkMode.REMOTE)
                .status(MissionStatus.BROUILLON)
                .build();
    }

    private AssignFreelancerRequest assignFreelancerRequest() {
        AssignFreelancerRequest request = new AssignFreelancerRequest();
        request.setFreelancerKeycloakId("freelancer-1");
        return request;
    }
}
