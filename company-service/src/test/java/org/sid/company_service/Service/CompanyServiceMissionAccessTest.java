package org.sid.company_service.Service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sid.company_service.DTO.MissionRequest;
import org.sid.company_service.Entity.Company;
import org.sid.company_service.Entity.CompanyStatus;
import org.sid.company_service.Repository.CompanyServiceRepository;
import org.springframework.web.server.ResponseStatusException;

class CompanyServiceMissionAccessTest {

    private CompanyServiceRepository companyRepository;
    private MissionServiceClient missionServiceClient;
    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        companyRepository = mock(CompanyServiceRepository.class);
        missionServiceClient = mock(MissionServiceClient.class);
        companyService = new CompanyService(companyRepository, missionServiceClient);
    }

    @Test
    void pendingCompanyCannotUpdateMission() {
        mockCompanyStatus(CompanyStatus.Pending);

        assertThatThrownBy(() -> companyService.updateMission("kc-company", 10L, new MissionRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verifyNoInteractions(missionServiceClient);
    }

    @Test
    void suspendedCompanyCannotDeleteMission() {
        mockCompanyStatus(CompanyStatus.Suspended);

        assertThatThrownBy(() -> companyService.deleteMission("kc-company", 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verifyNoInteractions(missionServiceClient);
    }

    @Test
    void rejectedCompanyCannotPublishMission() {
        mockCompanyStatus(CompanyStatus.Rejected);

        assertThatThrownBy(() -> companyService.publierMission("kc-company", 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verifyNoInteractions(missionServiceClient);
    }

    @Test
    void pendingCompanyCannotStartMission() {
        mockCompanyStatus(CompanyStatus.Pending);

        assertThatThrownBy(() -> companyService.demarrerMission("kc-company", 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verifyNoInteractions(missionServiceClient);
    }

    @Test
    void suspendedCompanyCannotCloseMission() {
        mockCompanyStatus(CompanyStatus.Suspended);

        assertThatThrownBy(() -> companyService.cloturerMission("kc-company", 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verifyNoInteractions(missionServiceClient);
    }

    @Test
    void validatedCompanyCanPublishMission() {
        mockCompanyStatus(CompanyStatus.Validated);

        companyService.publierMission("kc-company", 10L);

        verify(missionServiceClient).publierMissionForCompany(10L, 1L);
    }

    private void mockCompanyStatus(CompanyStatus status) {
        Company company = Company.builder()
                .id(1L)
                .keycloakId("kc-company")
                .companyEmail("company@example.com")
                .companyName("Company")
                .siret("12345678901234")
                .status(status)
                .build();
        when(companyRepository.findByKeycloakId("kc-company")).thenReturn(Optional.of(company));
    }
}
