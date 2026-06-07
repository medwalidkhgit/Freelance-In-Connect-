package org.sid.freelancer_service.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MissionResponse {

    private Long id;
    private Long companyId;
    private String title;
    private String description;
    private List<String> requiredSkills;
    private Integer durationDays;
    private BigDecimal budget;
    private WorkMode workMode;
    private MissionStatus status;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    public MissionResponse() {

    }}

