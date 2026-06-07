package org.sid.admin_service.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String adminUsername;
    private String action;
    private String targetAccountType; // "Freelance" ou "Entreprise"
    private Long targetAccountId;
    private String reason;
    private LocalDateTime timestamp;
}

