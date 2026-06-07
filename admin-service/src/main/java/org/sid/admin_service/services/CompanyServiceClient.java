package org.sid.admin_service.services;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.sid.admin_service.dto.CompanyDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "company-service", url = "${services.company.url:http://company-service:8083}")
public interface CompanyServiceClient {

    @GetMapping("/api/companies/admin/pending")
    List<CompanyDto> getPendingCompanies();

    @GetMapping("/api/companies/admin")
    List<CompanyDto> getAllCompanies();

    @PutMapping("/api/companies/admin/{id}/validate")
    CompanyDto validateCompany(@PathVariable("id") Long companyId);

    @PutMapping("/api/companies/admin/{id}/reject")
    CompanyDto rejectCompany(@PathVariable("id") Long companyId, @RequestBody Map<String, String> body);

    @PutMapping("/api/companies/admin/{id}/suspend")
    CompanyDto suspendCompany(@PathVariable("id") Long companyId, @RequestBody Map<String, String> body);

    @DeleteMapping("/api/companies/admin/{id}")
    void deleteCompany(@PathVariable("id") Long companyId);

}
