package org.sid.application_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "applications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_application_mission_freelancer",
                columnNames = {"mission_id", "freelancer_keycloak_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(name = "mission_company_id", nullable = false)
    private Long missionCompanyId;

    @Column(name = "freelancer_keycloak_id", nullable = false)
    private String freelancerKeycloakId;

    @Column(name = "freelancer_id")
    private Long freelancerId;

    @Column(nullable = false)
    private String freelancerFullname;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @Column(nullable = false)
    private int compatibilityScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "varchar(30)")
    private ApplicationStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = ApplicationStatus.PENDING;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
