package org.sid.application_service.dto;

import lombok.Data;

@Data
public class CreateConversationRequest {
    private Long missionId;
    private Long companyId;
    private Long freelancerId;
    private String companyKeycloakId;
    private String freelancerKeycloakId;
}
