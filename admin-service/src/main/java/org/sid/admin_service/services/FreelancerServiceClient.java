package org.sid.admin_service.services;

import org.sid.admin_service.dto.FreelancerDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@FeignClient(name = "freelancer-service", url = "${services.freelancer.url:http://freelancer-service:8082}")
public interface FreelancerServiceClient {

    @GetMapping("/api/freelances/admin")
    List<FreelancerDto> getAllFreelancers();

    @PostMapping("/api/freelances/{id}/suspend")
    void suspendFreelancer(@PathVariable("id") Long freelancerId);

    @DeleteMapping("/api/freelances/{id}")
    void deleteFreelancer(@PathVariable("id") Long freelancerId);
}
