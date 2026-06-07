package org.sid.application_service.client;

import org.sid.application_service.dto.MissionResponse;
import org.sid.application_service.dto.AssignFreelancerRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mission-service", url = "${services.mission.url}")
public interface MissionServiceClient {
    @GetMapping("/api/missions/{id}")
    MissionResponse getMissionById(@PathVariable("id") Long id);

    @PutMapping("/api/missions/{id}/assign-freelancer")
    void assignFreelancer(@PathVariable("id") Long missionId,
                          @RequestBody AssignFreelancerRequest request);
}
