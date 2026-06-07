package org.sid.application_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MissionResponse {
    private Long id;
    private Long companyId;
    private String companyKeycloakId;
    private String title;
    private String description;
    private List<String> requiredSkills;
    private Integer durationDays;
    private BigDecimal budget;
    private String workMode;
    private String status;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}
