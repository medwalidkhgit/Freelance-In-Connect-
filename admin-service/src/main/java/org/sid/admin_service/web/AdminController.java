package org.sid.admin_service.web;

import org.sid.admin_service.entities.AuditLog;
import org.sid.admin_service.services.AdminService;
import org.sid.admin_service.dto.AdminProfileDto;
import org.sid.admin_service.dto.CompanyDto;
import org.sid.admin_service.dto.FreelancerDto;
import org.sid.admin_service.dto.MissionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public AdminProfileDto getAdmin() {
        return adminService.getAdminProfile();
    }

    @PutMapping
    public AdminProfileDto updateAdmin(@RequestBody AdminProfileDto admin) {
        return adminService.updateAdmin(admin);
    }

    @PostMapping("/companies/{id}/approve")
    public void approveCompany(@PathVariable Long id) {
        adminService.approveCompany(id);
    }

    @GetMapping("/companies/pending")
    public List<CompanyDto> getPendingCompanies(@RequestParam(defaultValue = "false") boolean all) {
        return all ? adminService.getAllCompanies() : adminService.getPendingCompanies();
    }

    @GetMapping("/companies")
    public List<CompanyDto> getAllCompanies() {
        return adminService.getAllCompanies();
    }

    @GetMapping("/freelancers")
    public List<FreelancerDto> getAllFreelancers() {
        return adminService.getAllFreelancers();
    }

    @GetMapping("/missions")
    public List<MissionDto> getAllMissions() {
        return adminService.getAllMissions();
    }

    @GetMapping("/companies/{id}/missions")
    public List<MissionDto> getCompanyMissions(@PathVariable Long id) {
        return adminService.getCompanyMissions(id);
    }

    @PostMapping("/companies/{id}/reject")
    public CompanyDto rejectCompany(@PathVariable Long id, @RequestParam String reason) {
        return adminService.rejectCompany(id, reason);
    }

    @PostMapping("/companies/{id}/suspend")
    public CompanyDto suspendCompany(@PathVariable Long id, @RequestParam String reason) {
        return adminService.suspendCompany(id, reason);
    }

    @PostMapping("/freelancers/{id}/suspend")
    public void suspendFreelancer(@PathVariable Long id, @RequestParam String reason) {
        adminService.suspendFreelancer(id, reason);
    }

    @DeleteMapping("/companies/{id}")
    public void deleteCompany(@PathVariable Long id, @RequestParam String reason) {
        adminService.deleteCompany(id, reason);
    }

    @DeleteMapping("/freelancers/{id}")
    public void deleteFreelancer(@PathVariable Long id, @RequestParam String reason) {
        adminService.deleteFreelancer(id, reason);
    }

    @GetMapping("/audit")
    public List<AuditLog> getAuditLogs() {
        return adminService.getAuditLogs();
    }
}
