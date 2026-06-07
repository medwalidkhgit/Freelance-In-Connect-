package org.sid.auth_service.Client;

import org.sid.auth_service.DTO.CompanyRequest;
import org.sid.auth_service.DTO.CompanyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "company-service", url = "${services.company.url:http://company-service:8083}")
public interface CompanyServiceClient {

    @PostMapping("/api/companies")
    ResponseEntity<CompanyResponse> createCompany(@RequestBody CompanyRequest companyData);

    @GetMapping("/api/companies/me")
    ResponseEntity<CompanyResponse> getMyCompany(@RequestHeader("Authorization") String authorization);

    @GetMapping("/api/companies/internal/email-exists")
    ResponseEntity<Void> companyEmailExists(@RequestParam("email") String email);
}
