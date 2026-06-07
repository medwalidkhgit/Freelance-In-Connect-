package org.sid.mission_service.web;

import org.sid.mission_service.dto.AssignFreelancerRequest;
import org.sid.mission_service.entities.Mission;
import org.sid.mission_service.entities.WorkMode;
import org.sid.mission_service.services.MissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @GetMapping
    public List<Mission> getAllMissions() {
        return missionService.getAllMissions();
    }

    @GetMapping("/admin/all")
    public List<Mission> getEveryMission() {
        return missionService.getEveryMission();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mission> getMissionById(@PathVariable Long id) {
        return missionService.getMissionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mission createMission(@RequestBody Mission mission, @AuthenticationPrincipal Jwt jwt) {
        String companyKeycloakId = jwt == null ? null : jwt.getSubject();
        return missionService.createMission(mission, companyKeycloakId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Mission> updateMission(@PathVariable Long id, @RequestBody Mission mission) {
        return missionService.updateMission(id, mission)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/company/{companyId}")
    public ResponseEntity<Mission> updateMissionForCompany(
            @PathVariable Long id,
            @PathVariable Long companyId,
            @RequestBody Mission mission,
            @AuthenticationPrincipal Jwt jwt) {
        String companyKeycloakId = jwt == null ? null : jwt.getSubject();
        return missionService.updateMissionForCompany(id, companyId, mission, companyKeycloakId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMission(@PathVariable Long id) {
        if (!missionService.deleteMission(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/company/{companyId}")
    public ResponseEntity<Void> deleteMissionForCompany(
            @PathVariable Long id,
            @PathVariable Long companyId) {
        if (!missionService.deleteMissionForCompany(id, companyId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    // ── Gestion des statuts ───────────────────────────────────────────────────

    @PostMapping("/{id}/publier")
    public ResponseEntity<Mission> publierMission(@PathVariable Long id) {
        return missionService.publierMission(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/company/{companyId}/publier")
    public ResponseEntity<Mission> publierMissionForCompany(
            @PathVariable Long id,
            @PathVariable Long companyId) {
        return missionService.publierMissionForCompany(id, companyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/demarrer")
    public ResponseEntity<Mission> demarrerMission(@PathVariable Long id) {
        return missionService.demarrerMission(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/company/{companyId}/demarrer")
    public ResponseEntity<Mission> demarrerMissionForCompany(
            @PathVariable Long id,
            @PathVariable Long companyId) {
        return missionService.demarrerMissionForCompany(id, companyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cloturer")
    public ResponseEntity<Mission> cloturerMission(@PathVariable Long id) {
        return missionService.cloturerMission(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/company/{companyId}/cloturer")
    public ResponseEntity<Mission> cloturerMissionForCompany(
            @PathVariable Long id,
            @PathVariable Long companyId) {
        return missionService.cloturerMissionForCompany(id, companyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Recherche & filtres ───────────────────────────────────────────────────

    // Missions d'une entreprise : GET /api/missions/company/42
    @GetMapping("/company/{companyId}")
    public List<Mission> getMissionsByCompany(@PathVariable Long companyId) {
        return missionService.getMissionsByCompany(companyId);
    }

    @GetMapping("/company/{companyId}/all")
    public List<Mission> getAllMissionsByCompany(@PathVariable Long companyId) {
        return missionService.getAllMissionsByCompany(companyId);
    }

    // Toutes les missions publiées : GET /api/missions/publiees
    @GetMapping("/publiees")
    public List<Mission> getMissionsPublished() {
        return missionService.getMissionsPublished();
    }

    // Filtrer par compétence : GET /api/missions/search?skill=Java
    @GetMapping("/search")
    public List<Mission> rechercherMissions(
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget) {

        if (hasText(keyword) && !hasText(skill) && workMode == null && minBudget == null && maxBudget == null) {
            return missionService.searchMissions(keyword);
        }
        return missionService.filterPublishedMissions(skill, workMode, minBudget, maxBudget);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @PutMapping("/{id}/assign-freelancer")
    public ResponseEntity<Mission> assignFreelancer(
            @PathVariable Long id,
            @RequestBody AssignFreelancerRequest request) {
        return missionService.assignFreelancer(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
