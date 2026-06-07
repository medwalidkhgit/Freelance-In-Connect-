package org.sid.mission_service.services;

import org.sid.mission_service.dto.AssignFreelancerRequest;
import org.sid.mission_service.dto.CreatePaymentRequest;
import org.sid.mission_service.dto.MissionRequest;
import org.sid.mission_service.dto.MissionResponse;
import org.sid.mission_service.entities.Mission;
import org.sid.mission_service.entities.MissionStatus;
import org.sid.mission_service.entities.WorkMode;
import org.sid.mission_service.repositories.MissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Service
@Transactional
public class MissionService {

    private final MissionRepository missionRepository;
    private final RestTemplate restTemplate;
    private final String paymentServiceBaseUrl;

    public MissionService(MissionRepository missionRepository,
                          RestTemplate restTemplate,
                          @Value("${payment-service.base-url}") String paymentServiceBaseUrl) {
        this.missionRepository = missionRepository;
        this.restTemplate = restTemplate;
        this.paymentServiceBaseUrl = paymentServiceBaseUrl;
    }

    // ── CRUD de base ──────────────────────────────────────────────────────────

    public List<Mission> getAllMissions() {
        return missionRepository.findByStatus(MissionStatus.PUBLIEE);
    }

    public List<Mission> getEveryMission() {
        return missionRepository.findAll();
    }

    public Optional<Mission> getMissionById(Long id) {
        return missionRepository.findByIdAndStatus(id, MissionStatus.PUBLIEE);
    }

    public Mission createMission(Mission mission, String companyKeycloakId) {
        validateMissionPayload(mission);
        mission.setStatus(MissionStatus.BROUILLON);
        if (companyKeycloakId != null && !companyKeycloakId.isBlank()) {
            mission.setCompanyKeycloakId(companyKeycloakId);
        }
        return missionRepository.save(mission);
    }

    public Optional<Mission> updateMission(Long id, Mission updated) {
        validateMissionPayload(updated);
        return missionRepository.findById(id).map(existing -> {
            assertMissionCanBeUpdated(existing);
            existing.setTitle(updated.getTitle());
            existing.setDescription(updated.getDescription());
            existing.setRequiredSkills(updated.getRequiredSkills());
            existing.setDurationDays(updated.getDurationDays());
            existing.setBudget(updated.getBudget());
            existing.setWorkMode(updated.getWorkMode());
            return missionRepository.save(existing);
        });
    }

    public Optional<Mission> updateMissionForCompany(Long id, Long companyId, Mission updated, String companyKeycloakId) {
        assertMissionOwner(id, companyId);
        Optional<Mission> result = updateMission(id, updated);
        if (companyKeycloakId != null && !companyKeycloakId.isBlank()) {
            result.ifPresent(mission -> {
                if (mission.getCompanyKeycloakId() == null || mission.getCompanyKeycloakId().isBlank()) {
                    mission.setCompanyKeycloakId(companyKeycloakId);
                    missionRepository.save(mission);
                }
            });
        }
        return result;
    }

    public boolean deleteMission(Long id) {
        Mission mission = missionRepository.findById(id).orElse(null);
        if (mission == null) return false;
        missionRepository.delete(mission);
        return true;
    }

    public boolean deleteMissionForCompany(Long id, Long companyId) {
        assertMissionOwner(id, companyId);
        return deleteMission(id);
    }

    // ── Gestion des statuts ───────────────────────────────────────────────────

    public Optional<Mission> publierMission(Long id) {
        return changerStatut(id, MissionStatus.PUBLIEE);
    }

    public Optional<Mission> publierMissionForCompany(Long id, Long companyId) {
        assertMissionOwner(id, companyId);
        return publierMission(id);
    }

    public Optional<Mission> demarrerMission(Long id) {
        return changerStatut(id, MissionStatus.EN_COURS);
    }

    public Optional<Mission> demarrerMissionForCompany(Long id, Long companyId) {
        assertMissionOwner(id, companyId);
        return demarrerMission(id);
    }

    public Optional<Mission> cloturerMission(Long id) {
        return changerStatut(id, MissionStatus.CLOTUREE);
    }

    public Optional<Mission> cloturerMissionForCompany(Long id, Long companyId) {
        assertMissionOwner(id, companyId);
        return cloturerMission(id);
    }

    public Optional<Mission> assignFreelancer(Long missionId, AssignFreelancerRequest request) {
        if (request == null || request.getFreelancerKeycloakId() == null || request.getFreelancerKeycloakId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "freelancerKeycloakId is required");
        }
        return missionRepository.findById(missionId).map(mission -> {
            if (mission.getStatus() != MissionStatus.PUBLIEE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Freelancer can only be assigned to a published mission");
            }
            if (mission.getFreelancerKeycloakId() != null && !mission.getFreelancerKeycloakId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mission already has an assigned freelancer");
            }
            mission.setFreelancerKeycloakId(request.getFreelancerKeycloakId());
            return missionRepository.save(mission);
        });
    }

    private void assertMissionOwner(Long missionId, Long companyId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mission not found"));
        if (!companyId.equals(mission.getCompanyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mission does not belong to this company");
        }
    }

    private Optional<Mission> changerStatut(Long id, MissionStatus nouveauStatut) {
        return missionRepository.findById(id).map(mission -> {
            validateStatusTransition(mission.getStatus(), nouveauStatut);
            if (nouveauStatut == MissionStatus.PUBLIEE) {
                validateMissionPayload(mission);
            }
            if (nouveauStatut == MissionStatus.EN_COURS
                    && (mission.getFreelancerKeycloakId() == null || mission.getFreelancerKeycloakId().isBlank())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "freelancerKeycloakId is required to start mission");
            }
            mission.setStatus(nouveauStatut);
            return missionRepository.save(mission);
        });
    }

    private void validateStatusTransition(MissionStatus current, MissionStatus next) {
        boolean valid = switch (current) {
            case BROUILLON -> next == MissionStatus.PUBLIEE;
            case PUBLIEE -> next == MissionStatus.EN_COURS;
            case EN_COURS -> next == MissionStatus.CLOTUREE;
            case CLOTUREE -> false;
        };
        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid mission status transition: " + current + " -> " + next
            );
        }
    }

    private void validateMissionPayload(Mission mission) {
        if (mission == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mission payload is required");
        }
        if (mission.getCompanyId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId is required");
        }
        if (mission.getTitle() == null || mission.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        if (mission.getDescription() == null || mission.getDescription().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required");
        }
        if (mission.getDurationDays() == null || mission.getDurationDays() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "durationDays must be positive");
        }
        if (mission.getBudget() == null || mission.getBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "budget must be positive");
        }
        if (mission.getWorkMode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workMode is required");
        }
    }

    private void assertMissionCanBeUpdated(Mission mission) {
        if (mission.getStatus() == MissionStatus.CLOTUREE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Closed missions cannot be updated");
        }
    }

    private void triggerPayment(Mission mission) {
        if (mission.getCompanyKeycloakId() == null || mission.getCompanyKeycloakId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyKeycloakId is required to pay");
        }
        if (mission.getFreelancerKeycloakId() == null || mission.getFreelancerKeycloakId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "freelancerKeycloakId is required to pay");
        }
        if (mission.getBudget() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "budget is required to pay");
        }
        CreatePaymentRequest payload = new CreatePaymentRequest();
        payload.setCompanyId(resolveCompanyId(mission));
        payload.setFreelancerId(mission.getFreelancerKeycloakId());
        payload.setAmountCents(toCents(mission.getBudget()));

        String url = paymentServiceBaseUrl + "/api/payments/mission/" + mission.getId();
        restTemplate.postForObject(url, payload, Object.class);
    }

    private String resolveCompanyId(Mission mission) {
        if (mission.getCompanyKeycloakId() != null && !mission.getCompanyKeycloakId().isBlank()) {
            return mission.getCompanyKeycloakId();
        }
        return mission.getCompanyId() == null ? null : mission.getCompanyId().toString();
    }

    private long toCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
    }

    // ── Requêtes métier ───────────────────────────────────────────────────────

    public List<Mission> getMissionsByCompany(Long companyId) {
        return missionRepository.findByCompanyIdAndStatus(companyId, MissionStatus.PUBLIEE);
    }

    public List<Mission> getAllMissionsByCompany(Long companyId) {
        return missionRepository.findByCompanyId(companyId);
    }

    public List<Mission> getMissionsPublished() {
        return missionRepository.findByStatus(MissionStatus.PUBLIEE);
    }

    public List<Mission> getMissionsBySkill(String skill) {
        return missionRepository.findPublishedBySkill(skill);
    }

    public List<Mission> getMissionsBYWorkMode(WorkMode workMode) {
        return missionRepository.findByStatusAndWorkMode(MissionStatus.PUBLIEE, workMode);
    }

    public List<Mission> searchMissions(String keyword) {
        return missionRepository.searchPublished(keyword);
    }

    public List<Mission> filterPublishedMissions(String skill,
                                                 WorkMode workMode,
                                                 BigDecimal minBudget,
                                                 BigDecimal maxBudget) {
        return missionRepository.filterPublished(
                normalizeFilter(skill),
                workMode,
                minBudget,
                maxBudget
        );
    }

    private String normalizeFilter(String value) {
        return value == null ? null : value.trim();
    }
}
