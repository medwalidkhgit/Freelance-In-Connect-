package org.sid.application_service.client;

import org.sid.application_service.dto.CompanyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "company-service", url = "${services.company.url}")
public interface CompanyServiceClient {
    @GetMapping("/api/companies/me")
    CompanyResponse getMyCompany();
}
