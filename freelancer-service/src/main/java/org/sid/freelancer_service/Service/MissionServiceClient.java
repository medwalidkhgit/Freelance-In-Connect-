package org.sid.freelancer_service.Service;

import org.sid.freelancer_service.DTO.MissionResponse;
import org.sid.freelancer_service.DTO.WorkMode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "mission-service", url = "${services.mission.url}")
public interface MissionServiceClient {

    @GetMapping("/api/missions/company/{companyId}")
    List<MissionResponse> getMissionsByCompany(@PathVariable("companyId") Long companyId);

    @GetMapping("/api/missions")
    List<MissionResponse> getAllMissions();

    @GetMapping("/api/missions/{id}")
    MissionResponse getMissionById(@PathVariable("id") Long id);

    @GetMapping("/api/missions/publiees")
    List<MissionResponse> getMissionsPublished();

    @GetMapping("/api/missions/search")
    List<MissionResponse> searchMissions(
            @RequestParam(value = "skill", required = false) String skill,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "workMode", required = false) WorkMode workMode);

//    @PostMapping("/api/missions")
//    MissionResponse createMission(@RequestBody MissionRequest mission);
}
