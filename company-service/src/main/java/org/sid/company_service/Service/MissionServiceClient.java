package org.sid.company_service.Service;

import org.sid.company_service.DTO.MissionRequest;
import org.sid.company_service.DTO.MissionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "mission-service", url = "${services.mission.url}")
public interface MissionServiceClient {


    @PostMapping("/api/missions")
    MissionResponse createMission(@RequestBody MissionRequest mission);

    @GetMapping("/api/missions/company/{companyId}/all")
    java.util.List<MissionResponse> getAllMissionsForCompany(@PathVariable("companyId") Long companyId);

    @PutMapping("/api/missions/{id}")
    ResponseEntity<MissionResponse> updateMission(
            @PathVariable("id") Long id,
            @RequestBody MissionRequest mission);

    @PutMapping("/api/missions/{id}/company/{companyId}")
    ResponseEntity<MissionResponse> updateMissionForCompany(
            @PathVariable("id") Long id,
            @PathVariable("companyId") Long companyId,
            @RequestBody MissionRequest mission);

    @DeleteMapping("/api/missions/{id}")
    ResponseEntity<Void> deleteMission(@PathVariable("id") Long id);

    @DeleteMapping("/api/missions/{id}/company/{companyId}")
    ResponseEntity<Void> deleteMissionForCompany(
            @PathVariable("id") Long id,
            @PathVariable("companyId") Long companyId);

    @PostMapping("/api/missions/{id}/publier")
    ResponseEntity<MissionResponse> publierMission(@PathVariable("id") Long id);

    @PostMapping("/api/missions/{id}/company/{companyId}/publier")
    ResponseEntity<MissionResponse> publierMissionForCompany(
            @PathVariable("id") Long id,
            @PathVariable("companyId") Long companyId);

    @PostMapping("/api/missions/{id}/demarrer")
    ResponseEntity<MissionResponse> demarrerMission(@PathVariable("id") Long id);

    @PostMapping("/api/missions/{id}/company/{companyId}/demarrer")
    ResponseEntity<MissionResponse> demarrerMissionForCompany(
            @PathVariable("id") Long id,
            @PathVariable("companyId") Long companyId);

    @PostMapping("/api/missions/{id}/cloturer")
    ResponseEntity<MissionResponse> cloturerMission(@PathVariable("id") Long id);

    @PostMapping("/api/missions/{id}/company/{companyId}/cloturer")
    ResponseEntity<MissionResponse> cloturerMissionForCompany(
            @PathVariable("id") Long id,
            @PathVariable("companyId") Long companyId);

}
