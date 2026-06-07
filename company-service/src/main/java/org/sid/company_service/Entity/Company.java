package org.sid.company_service.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String keycloakId;

    @Column(unique = true, nullable = false)
    private String companyEmail;

    @Column(nullable = false)
    private String companyName;

    // ✅ Ajouté — envoyé par auth-service à l'inscription
    @Column(unique = true, nullable = false)
    private String siret;

    // ✅ Ajouté — champs séparés (fix 4 auth-service)
    private String contactFirstName;
    private String contactLastName;

    private String companyAddress;
    private String companyPhone;
    private String companyFax;
    private String domaine;
    private String pfpUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CompanyStatus status = CompanyStatus.Pending;

    private String rejectionReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
