package org.sid.freelancer_service.Service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "application-service", url = "${services.application.url:http://application-service:8085}")
public interface ApplicationServiceClient {

    @GetMapping("/api/applications/company/freelancers/{freelancerId}/access")
    Boolean companyHasApplicationForFreelancer(@PathVariable("freelancerId") Long freelancerId);
}
