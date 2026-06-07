package org.sid.freelancer_service.Controller;

import org.sid.freelancer_service.Entity.Freelancer;
import org.sid.freelancer_service.DTO.FreelancerAdminDTO;
import org.sid.freelancer_service.DTO.FreelancerProfileDTO;
import org.sid.freelancer_service.DTO.FreelancerRequest;
import org.sid.freelancer_service.DTO.FreelancerResponse;
import org.sid.freelancer_service.DTO.FreelancerUpdateRequest;
import org.sid.freelancer_service.DTO.MissionResponse;
import org.sid.freelancer_service.DTO.WorkMode;
import org.sid.freelancer_service.Service.FreelancerService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/freelances")
public class FreelancerController {
    private final FreelancerService service;

    public FreelancerController(FreelancerService service) {
        this.service = service;
    }

    @GetMapping
    public Page<FreelancerProfileDTO> getAllProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getAllProfiles(page, size).map(service::toProfileDto);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<FreelancerProfileDTO> getProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.getProfileForViewer(id, jwt));
    }

    @GetMapping("/admin")
    public List<FreelancerAdminDTO> getProfilesForAdmin() {
        return service.getAllProfilesForAdmin();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FreelancerResponse createProfile(@RequestBody FreelancerRequest request) {
        return service.createFreelancer(request);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Freelancer> updateProfile(@PathVariable Long id, @RequestBody FreelancerUpdateRequest request) {
        if (!service.profileExists(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.updateProfileById(id, request));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        if (!service.profileExists(id)) {
            return ResponseEntity.notFound().build();
        }
        service.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id:\\d+}/suspend")
    public ResponseEntity<Void> suspendProfile(@PathVariable Long id) {
        if (!service.profileExists(id)) {
            return ResponseEntity.notFound().build();
        }
        service.suspendProfile(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/missions")
    public ResponseEntity<List<MissionResponse>> getAllMissions() {
        return ResponseEntity.ok(service.getAllMissions());
    }

    @GetMapping("/missions/{id}")
    public ResponseEntity<MissionResponse> getMissionById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getMissionById(id));
    }

    @GetMapping("/missions/publiees")
    public ResponseEntity<List<MissionResponse>> getMissionsPublished() {
        return ResponseEntity.ok(service.getMissionsPublished());
    }

    @GetMapping("/missions/search")
    public ResponseEntity<List<MissionResponse>> searchMissions(
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) WorkMode workMode) {
        return ResponseEntity.ok(service.searchMissions(skill, keyword, workMode));
    }

    @GetMapping("/missions/company/{companyId}")
    public ResponseEntity<List<MissionResponse>> getMissionsByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(service.getMissionsByCompany(companyId));
    }

    @GetMapping("/me")
    public ResponseEntity<Freelancer> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return service.getProfileByKeycloakId(jwt.getSubject())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/internal/email-exists")
    public ResponseEntity<Void> emailExists(@RequestParam String email) {
        return service.emailExists(email)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/me")
    public ResponseEntity<Freelancer> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody FreelancerUpdateRequest request) {
        return ResponseEntity.ok(service.updateMyProfile(jwt.getSubject(), request));
    }
}
