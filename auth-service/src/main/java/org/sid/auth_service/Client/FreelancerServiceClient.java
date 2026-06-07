package org.sid.auth_service.Client;

import org.sid.auth_service.DTO.FreelancerRequest;
import org.sid.auth_service.DTO.FreelancerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "freelancer-service", url = "${services.freelancer.url:http://freelancer-service:8082}")
public interface FreelancerServiceClient {

    @PostMapping("/api/freelances")
    ResponseEntity<FreelancerResponse> createFreelancer(@RequestBody FreelancerRequest freelancerData);

    @GetMapping("/api/freelances/internal/email-exists")
    ResponseEntity<Void> freelancerEmailExists(@RequestParam("email") String email);
}
