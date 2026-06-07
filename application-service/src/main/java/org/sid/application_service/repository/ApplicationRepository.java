package org.sid.application_service.repository;

import org.sid.application_service.entity.Application;
import org.sid.application_service.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByMissionIdAndFreelancerKeycloakId(Long missionId, String freelancerKeycloakId);

    boolean existsByMissionIdAndStatus(Long missionId, ApplicationStatus status);

    boolean existsByMissionCompanyIdAndFreelancerId(Long missionCompanyId, Long freelancerId);

    Optional<Application> findByIdAndFreelancerKeycloakId(Long id, String freelancerKeycloakId);

    List<Application> findByFreelancerKeycloakIdOrderByCreatedAtDesc(String freelancerKeycloakId);

    List<Application> findByMissionIdOrderByCompatibilityScoreDescCreatedAtAsc(Long missionId);

    List<Application> findByMissionCompanyIdOrderByCreatedAtDesc(Long missionCompanyId);

    List<Application> findByMissionIdAndStatusOrderByCompatibilityScoreDescCreatedAtAsc(
            Long missionId,
            ApplicationStatus status
    );
}
