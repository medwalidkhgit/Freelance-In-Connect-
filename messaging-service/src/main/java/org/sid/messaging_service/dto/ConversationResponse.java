package org.sid.messaging_service.dto;

import java.time.Instant;

public class ConversationResponse {

    private String id;
    private Long missionId;
    private Long companyId;
    private Long freelancerId;
    private String companyKeycloakId;
    private String freelancerKeycloakId;
    private Instant createdAt;

    public ConversationResponse() {
    }

    public ConversationResponse(String id, Long missionId, Long companyId, Long freelancerId, Instant createdAt) {
        this.id = id;
        this.missionId = missionId;
        this.companyId = companyId;
        this.freelancerId = freelancerId;
        this.createdAt = createdAt;
    }

    public ConversationResponse(String id, Long missionId, Long companyId, Long freelancerId,
                                String companyKeycloakId, String freelancerKeycloakId, Instant createdAt) {
        this.id = id;
        this.missionId = missionId;
        this.companyId = companyId;
        this.freelancerId = freelancerId;
        this.companyKeycloakId = companyKeycloakId;
        this.freelancerKeycloakId = freelancerKeycloakId;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getMissionId() {
        return missionId;
    }

    public void setMissionId(Long missionId) {
        this.missionId = missionId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    public String getCompanyKeycloakId() {
        return companyKeycloakId;
    }

    public void setCompanyKeycloakId(String companyKeycloakId) {
        this.companyKeycloakId = companyKeycloakId;
    }

    public String getFreelancerKeycloakId() {
        return freelancerKeycloakId;
    }

    public void setFreelancerKeycloakId(String freelancerKeycloakId) {
        this.freelancerKeycloakId = freelancerKeycloakId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
