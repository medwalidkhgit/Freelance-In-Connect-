package org.sid.mission_service.repositories;

import org.sid.mission_service.entities.Mission;
import org.sid.mission_service.entities.MissionStatus;
import org.sid.mission_service.entities.WorkMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    // Toutes les missions d'une entreprise
    List<Mission> findByCompanyId(Long companyId);

    // Missions par statut
    List<Mission> findByStatus(MissionStatus status);

    Optional<Mission> findByIdAndStatus(Long id, MissionStatus status);

    List<Mission> findByCompanyIdAndStatus(Long companyId, MissionStatus status);

    // Missions publiées filtrables par compétence et mode de travail
    @Query("SELECT m FROM Mission m JOIN m.requiredSkills s " +
           "WHERE m.status = 'PUBLIEE' AND s = :skill")
    List<Mission> findPublishedBySkill(@Param("skill") String skill);

    List<Mission> findByStatusAndWorkMode(MissionStatus status, WorkMode workMode);

    // Recherche fulltext simple sur titre et description
    @Query("SELECT m FROM Mission m WHERE m.status = 'PUBLIEE' AND " +
           "(LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Mission> searchPublished(@Param("keyword") String keyword);

    @Query("""
           SELECT DISTINCT m FROM Mission m
           LEFT JOIN m.requiredSkills s
           WHERE m.status = 'PUBLIEE'
             AND (:skill IS NULL OR :skill = '' OR LOWER(s) = LOWER(:skill))
             AND (:workMode IS NULL OR m.workMode = :workMode)
             AND (:minBudget IS NULL OR m.budget >= :minBudget)
             AND (:maxBudget IS NULL OR m.budget <= :maxBudget)
           """)
    List<Mission> filterPublished(
            @Param("skill") String skill,
            @Param("workMode") WorkMode workMode,
            @Param("minBudget") BigDecimal minBudget,
            @Param("maxBudget") BigDecimal maxBudget
    );
}
