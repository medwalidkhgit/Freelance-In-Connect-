package org.sid.messaging_service.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;

    private Long missionId;
    private Long companyId;
    private Long freelancerId;
    private String companyKeycloakId;
    private String freelancerKeycloakId;
    private Instant createdAt = Instant.now();

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
