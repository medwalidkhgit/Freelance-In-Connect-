package org.sid.admin_service.services;

import org.sid.admin_service.dto.AdminProfileDto;
import org.sid.admin_service.dto.CompanyDto;
import org.sid.admin_service.dto.FreelancerDto;
import org.sid.admin_service.dto.MissionDto;
import org.sid.admin_service.entities.Admin;
import org.sid.admin_service.entities.AuditLog;
import org.sid.admin_service.repositories.AdminRepository;
import org.sid.admin_service.repositories.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;
    private final AuditLogRepository auditLogRepository;
    private final FreelancerServiceClient freelancerServiceClient;
    private final CompanyServiceClient companyServiceClient;
    private final AuthServiceClient authServiceClient;
    private final MissionServiceClient missionServiceClient;

    public AdminService(AdminRepository adminRepository,
                        AuditLogRepository auditLogRepository,
                        FreelancerServiceClient freelancerServiceClient,
                        CompanyServiceClient companyServiceClient,
                        AuthServiceClient authServiceClient,
                        MissionServiceClient missionServiceClient) {
        this.adminRepository = adminRepository;
        this.auditLogRepository = auditLogRepository;
        this.freelancerServiceClient = freelancerServiceClient;
        this.companyServiceClient = companyServiceClient;
        this.authServiceClient = authServiceClient;
        this.missionServiceClient = missionServiceClient;
    }

    public Admin getAdmin() {
        Admin admin = adminRepository.findAll().stream().findFirst().orElse(null);
        if (admin == null) {
            admin = Admin.builder()
                    .username(currentAdminUsername())
                    .email(currentAdminEmail())
                    .build();
            admin = adminRepository.save(admin);
        }
        return admin;
    }

    public AdminProfileDto getAdminProfile() {
        return toProfileDto(getAdmin());
    }

    public AdminProfileDto updateAdmin(AdminProfileDto admin) {
        Admin existing = getAdmin();
        existing.setUsername(admin.getUsername());
        existing.setEmail(admin.getEmail());
        return toProfileDto(adminRepository.save(existing));
    }

    public void approveCompany(Long companyId) {
        try {
            companyServiceClient.validateCompany(companyId);
            logAction("Approbation d'entreprise", "Company", companyId, null);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors d'approbation d'entreprise: " + e.getMessage());
        }
    }

    public CompanyDto rejectCompany(Long companyId, String reason) {
        requireReason(reason);
        try {
            Map<String, String> body = new HashMap<>();
            body.put("reason", reason);
            CompanyDto result = companyServiceClient.rejectCompany(companyId, body);
            logAction("Rejet d'entreprise", "Company", companyId, reason);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du reject d'entreprise: " + e.getMessage());
        }
    }

    public List<CompanyDto> getPendingCompanies() {
        try {
            return companyServiceClient.getPendingCompanies();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recuperation des entreprises en attente: " + e.getMessage());
        }
    }

    public List<CompanyDto> getAllCompanies() {
        try {
            return companyServiceClient.getAllCompanies();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recuperation des entreprises: " + e.getMessage());
        }
    }

    public List<FreelancerDto> getAllFreelancers() {
        try {
            return freelancerServiceClient.getAllFreelancers();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recuperation des freelances: " + e.getMessage());
        }
    }

    public List<MissionDto> getAllMissions() {
        try {
            return missionServiceClient.getAllMissions();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recuperation des missions: " + e.getMessage());
        }
    }

    public List<MissionDto> getCompanyMissions(Long companyId) {
        try {
            return missionServiceClient.getAllMissionsByCompany(companyId);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recuperation des missions de l'entreprise: " + e.getMessage());
        }
    }

    public void suspendFreelancer(Long freelancerId, String reason) {
        requireReason(reason);
        try {
            freelancerServiceClient.suspendFreelancer(freelancerId);
            logAction("Suspension de freelance", "Freelancer", freelancerId, reason);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suspension du freelancer: " + e.getMessage());
        }
    }

    public void deleteCompany(Long companyId, String reason) {
        requireReason(reason);
        try {
            CompanyDto company = findCompanyForDeletion(companyId);
            deleteKeycloakUser(company.getKeycloakId());
            companyServiceClient.deleteCompany(companyId);
            logAction("Suppression d'entreprise", "Company", companyId, reason);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression d'entreprise: " + e.getMessage());
        }
    }

    public CompanyDto suspendCompany(Long companyId, String reason) {
        requireReason(reason);
        try {
            Map<String, String> body = new HashMap<>();
            body.put("reason", reason);
            CompanyDto result = companyServiceClient.suspendCompany(companyId, body);
            logAction("Suspension d'entreprise", "Company", companyId, reason);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suspension d'entreprise: " + e.getMessage());
        }
    }

    public void deleteFreelancer(Long freelancerId, String reason) {
        requireReason(reason);
        try {
            FreelancerDto freelancer = findFreelancerForDeletion(freelancerId);
            deleteKeycloakUser(freelancer.getKeycloakId());
            freelancerServiceClient.deleteFreelancer(freelancerId);
            logAction("Suppression de freelance", "Freelancer", freelancerId, reason);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression du freelancer: " + e.getMessage());
        }
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAll();
    }

    private void logAction(String action, String targetType, Long targetId, String reason) {
        AuditLog auditLog = AuditLog.builder()
                .adminUsername(currentAdminUsername())
                .action(action)
                .targetAccountType(targetType)
                .targetAccountId(targetId)
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("La raison est obligatoire pour cette action admin.");
        }
    }

    private CompanyDto findCompanyForDeletion(Long companyId) {
        return getAllCompanies().stream()
                .filter(company -> company.getId().equals(companyId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entreprise introuvable: " + companyId));
    }

    private FreelancerDto findFreelancerForDeletion(Long freelancerId) {
        return getAllFreelancers().stream()
                .filter(freelancer -> freelancer.getId().equals(freelancerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Freelancer introuvable: " + freelancerId));
    }

    private void deleteKeycloakUser(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new IllegalArgumentException("Keycloak ID manquant pour la suppression.");
        }
        authServiceClient.deleteKeycloakUser(keycloakId);
    }

    private AdminProfileDto toProfileDto(Admin admin) {
        return new AdminProfileDto(admin.getId(), admin.getUsername(), admin.getEmail());
    }

    private String currentAdminUsername() {
        Jwt jwt = currentJwt();
        if (jwt == null) {
            return "unknown-admin";
        }
        String username = jwt.getClaimAsString("preferred_username");
        if (username != null && !username.isBlank()) {
            return username;
        }
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        return jwt.getSubject();
    }

    private String currentAdminEmail() {
        Jwt jwt = currentJwt();
        if (jwt == null) {
            return null;
        }
        return jwt.getClaimAsString("email");
    }

    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }
}
