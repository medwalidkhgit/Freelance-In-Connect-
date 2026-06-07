package org.sid.admin_service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sid.admin_service.dto.CompanyDto;
import org.sid.admin_service.entities.AuditLog;
import org.sid.admin_service.repositories.AdminRepository;
import org.sid.admin_service.repositories.AuditLogRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceBusinessRulesTest {

    private AdminRepository adminRepository;
    private AuditLogRepository auditLogRepository;
    private FreelancerServiceClient freelancerServiceClient;
    private CompanyServiceClient companyServiceClient;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminRepository = mock(AdminRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        freelancerServiceClient = mock(FreelancerServiceClient.class);
        companyServiceClient = mock(CompanyServiceClient.class);
        adminService = new AdminService(
                adminRepository,
                auditLogRepository,
                freelancerServiceClient,
                companyServiceClient
        );
    }

    @Test
    void rejectCompanyRequiresReasonBeforeCallingCompanyService() {
        assertThatThrownBy(() -> adminService.rejectCompany(1L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La raison est obligatoire pour cette action admin.");

        verify(companyServiceClient, never()).rejectCompany(anyLong(), anyMap());
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void suspendCompanyRequiresReasonBeforeCallingCompanyService() {
        assertThatThrownBy(() -> adminService.suspendCompany(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La raison est obligatoire pour cette action admin.");

        verify(companyServiceClient, never()).suspendCompany(anyLong(), anyMap());
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void deleteCompanyRequiresReasonBeforeCallingCompanyService() {
        assertThatThrownBy(() -> adminService.deleteCompany(1L, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La raison est obligatoire pour cette action admin.");

        verify(companyServiceClient, never()).deleteCompany(anyLong());
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void suspendFreelancerRequiresReasonBeforeCallingFreelancerService() {
        assertThatThrownBy(() -> adminService.suspendFreelancer(1L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La raison est obligatoire pour cette action admin.");

        verify(freelancerServiceClient, never()).suspendFreelancer(anyLong());
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void deleteFreelancerRequiresReasonBeforeCallingFreelancerService() {
        assertThatThrownBy(() -> adminService.deleteFreelancer(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La raison est obligatoire pour cette action admin.");

        verify(freelancerServiceClient, never()).deleteFreelancer(anyLong());
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void rejectCompanyWithReasonCallsCompanyServiceAndWritesAuditLog() {
        when(companyServiceClient.rejectCompany(anyLong(), anyMap())).thenReturn(new CompanyDto());

        adminService.rejectCompany(1L, "Documents invalides");

        verify(companyServiceClient).rejectCompany(anyLong(), anyMap());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void auditLogsAreReturnedFromRepository() {
        when(auditLogRepository.findAll()).thenReturn(List.of(AuditLog.builder().id(1L).build()));

        adminService.getAuditLogs();

        verify(auditLogRepository).findAll();
    }
}
