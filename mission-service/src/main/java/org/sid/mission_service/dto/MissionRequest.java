package org.sid.mission_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MissionRequest {

    private Long companyId;
    private String title;
    private String description;
    private List<String> requiredSkills;
    private Integer durationDays;
    private BigDecimal budget;
    private WorkMode workMode;

    public MissionRequest() {

    }
}
