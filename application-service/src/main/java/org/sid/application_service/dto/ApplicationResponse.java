package org.sid.application_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.sid.application_service.entity.ApplicationStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ApplicationResponse {
    private Long id;
    private Long missionId;
    private Long missionCompanyId;
    private Long freelancerId;
    private String freelancerKeycloakId;
    private String freelancerFullname;
    private String coverLetter;
    private int compatibilityScore;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
