package org.sid.application_service.client;

import org.sid.application_service.dto.FreelancerProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "freelancer-service", url = "${services.freelancer.url}")
public interface FreelancerServiceClient {
    @GetMapping("/api/freelances/me")
    FreelancerProfileDTO getMyProfile();
}
