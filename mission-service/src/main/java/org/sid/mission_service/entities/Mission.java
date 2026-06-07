package org.sid.mission_service.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifiant de l'entreprise propriétaire (référence externe)
    private Long companyId;
    private String companyKeycloakId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    private List<String> requiredSkills;

    // Durée en jours
    private Integer durationDays;

    private BigDecimal budget;

    @Enumerated(EnumType.STRING)
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    private MissionStatus status = MissionStatus.BROUILLON;

    private String freelancerKeycloakId;

    private LocalDate createdAt;
    private LocalDate updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDate.now();
        updatedAt = LocalDate.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDate.now();
    }
}
