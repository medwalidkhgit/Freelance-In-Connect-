package org.sid.application_service.web;

import org.sid.application_service.dto.ApplicationRequest;
import org.sid.application_service.dto.ApplicationResponse;
import org.sid.application_service.dto.ApplicationStatusRequest;
import org.sid.application_service.service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ApplicationRequest request) {
        return applicationService.apply(jwt, request);
    }

    @GetMapping("/{id:\\d+}")
    public ApplicationResponse getApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return applicationService.getApplication(id, jwt);
    }

    @GetMapping("/me")
    public List<ApplicationResponse> getMyApplications(@AuthenticationPrincipal Jwt jwt) {
        return applicationService.getMyApplications(jwt);
    }

    @GetMapping("/mission/{missionId}")
    public List<ApplicationResponse> getApplicationsForMission(@PathVariable Long missionId) {
        return applicationService.getApplicationsForMission(missionId);
    }

    @GetMapping("/mission/{missionId}/accepted")
    public List<ApplicationResponse> getAcceptedApplicationsForMission(@PathVariable Long missionId) {
        return applicationService.getAcceptedApplicationsForMission(missionId);
    }

    @GetMapping("/company/me")
    public List<ApplicationResponse> getApplicationsForCurrentCompany() {
        return applicationService.getApplicationsForCurrentCompany();
    }

    @GetMapping("/company/freelancers/{freelancerId}/access")
    public boolean companyHasApplicationForFreelancer(@PathVariable Long freelancerId) {
        return applicationService.companyHasApplicationForFreelancer(freelancerId);
    }

    @PutMapping("/{id:\\d+}/status")
    public ApplicationResponse updateStatus(
            @PathVariable Long id,
            @RequestBody ApplicationStatusRequest request) {
        return applicationService.updateStatus(id, request);
    }

    @GetMapping("/admin/all")
    public List<ApplicationResponse> getAllApplicationsForAdmin() {
        return applicationService.getAllApplicationsForAdmin();
    }
}
