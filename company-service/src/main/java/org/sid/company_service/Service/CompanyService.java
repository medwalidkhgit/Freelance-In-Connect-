package org.sid.company_service.Service;

import org.sid.company_service.DTO.*;
import org.sid.company_service.Entity.Company;
import org.sid.company_service.Entity.CompanyStatus;
import org.sid.company_service.Repository.CompanyServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CompanyService {

    private final CompanyServiceRepository companyRep;
    private final MissionServiceClient missionServiceClient;
    private final AuthServiceClient authServiceClient;

    public CompanyService(CompanyServiceRepository companyRep,
                          MissionServiceClient missionServiceClient,
                          AuthServiceClient authServiceClient) {
        this.companyRep = companyRep;
        this.missionServiceClient = missionServiceClient;
        this.authServiceClient = authServiceClient;
    }

    public CompanyResponse saveCompanyFromAuth(CompanyRequest request) {
        if (companyRep.findByCompanyEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email deja utilise");
        }

        Company company = Company.builder()
                .keycloakId(request.getKeycloakUserId())
                .companyEmail(request.getEmail())
                .companyName(request.getCompanyName())
                .siret(request.getSiret())
                .contactFirstName(request.getContactFirstName())
                .contactLastName(request.getContactLastName())
                .companyAddress(request.getCompanyAddress())
                .companyPhone(request.getCompanyPhone())
                .domaine(request.getDomaine())
                .pfpUrl(request.getPfpUrl())
                .status(CompanyStatus.Pending) // toujours forcé à Pending
                .build();

        Company saved = companyRep.save(company);
        return new CompanyResponse(saved.getId(), saved.getCompanyName(), saved.getStatus().name());
    }

    public List<Company> getAllCompanies() {
        return companyRep.findAll();
    }

    public boolean emailExists(String email) {
        return email != null && companyRep.findByCompanyEmail(email).isPresent();
    }

    public Company getCompanyById(Long id) {
        return companyRep.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Entreprise non trouvée"));
    }

    public Company getCompanyByKeycloakId(String keycloakId) {
        return companyRep.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Aucune entreprise pour cet utilisateur"));
    }

    public Company updateCompany(Long id, CompanyUpdateRequest updated) {
        Company existing = getCompanyById(id);
        existing.setCompanyName(updated.getCompanyName());
        existing.setContactFirstName(updated.getContactFirstName());
        existing.setContactLastName(updated.getContactLastName());
        existing.setCompanyAddress(updated.getCompanyAddress());
        existing.setCompanyPhone(updated.getCompanyPhone());
        existing.setCompanyFax(updated.getCompanyFax());
        existing.setDomaine(updated.getDomaine());
        existing.setPfpUrl(updated.getPfpUrl());
        Company saved = companyRep.save(existing);
        syncKeycloakProfile(saved);
        return saved;
    }

    public void deleteCompany(Long id) {
        companyRep.deleteById(id);
    }

    public List<Company> getPendingCompanies() {
        return companyRep.findByStatus(CompanyStatus.Pending);
    }

    public Company validateCompany(Long id) {
        Company company = getCompanyById(id);
        company.setStatus(CompanyStatus.Validated);
        company.setRejectionReason(null);
        return companyRep.save(company);
    }

    public Company rejectCompany(Long id, String reason) {
        Company company = getCompanyById(id);
        company.setStatus(CompanyStatus.Rejected);
        company.setRejectionReason(reason);
        return companyRep.save(company);
    }

    public Company suspendCompany(Long id, String reason) {
        Company company = getCompanyById(id);
        company.setStatus(CompanyStatus.Suspended);
        company.setRejectionReason(reason);
        return companyRep.save(company);
    }

    public MissionResponse createMission(String keycloakId, MissionRequest mission) {
        Company company = getValidatedCompanyByKeycloakId(keycloakId);
        mission.setCompanyId(company.getId());
        mission.setCompanyKeycloakId(keycloakId);
        return missionServiceClient.createMission(mission);
    }

    public List<MissionResponse> getMyMissions(String keycloakId) {
        Company company = getValidatedCompanyByKeycloakId(keycloakId);
        return missionServiceClient.getAllMissionsForCompany(company.getId());
    }

    public ResponseEntity<MissionResponse> updateMission(String keycloakId, Long id, MissionRequest mission) {
        Company company = getValidatedCompanyByKeycloakId(keycloakId);
        mission.setCompanyId(company.getId());
        mission.setCompanyKeycloakId(keycloakId);
        return missionServiceClient.updateMissionForCompany(id, company.getId(), mission);
    }

    public ResponseEntity<Void> deleteMission(String keycloakId, Long id) {
        Company company = getValidatedCompanyByKeycloakId(keycloakId);
        return missionServiceClient.deleteMissionForCompany(id, company.getId());
    }

    public ResponseEntity<MissionResponse> publierMission(String keycloakId, Long id) {
        Company company = getValidatedCompanyByKeycloakId(keycloakId);
        return missionServiceClient.publierMissionForCompany(id, company.getId());
    }

    public ResponseEntity<MissionResponse> demarrerMission(String keycloakId, Long id) {
        Company company = getValidatedCompanyByKeycloakId(keycloakId);
        return missionServiceClient.demarrerMissionForCompany(id, company.getId());
    }

    public ResponseEntity<MissionResponse> cloturerMission(String keycloakId, Long id) {
        Company company = getValidatedCompanyByKeycloakId(keycloakId);
        return missionServiceClient.cloturerMissionForCompany(id, company.getId());
    }

    public List<Company> getValidatedCompanies() {
        return companyRep.findByStatus(CompanyStatus.Validated);
    }

    private Company getValidatedCompanyByKeycloakId(String keycloakId) {
        Company company = getCompanyByKeycloakId(keycloakId);
        if (company.getStatus() != CompanyStatus.Validated) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company not validated");
        }
        return company;
    }

    private void syncKeycloakProfile(Company company) {
        try {
            authServiceClient.updateKeycloakProfile(
                    company.getKeycloakId(),
                    new KeycloakProfileUpdateRequest(
                            company.getContactFirstName(),
                            company.getContactLastName(),
                            company.getCompanyPhone(),
                            company.getCompanyName()
                    )
            );
        } catch (Exception ignored) {
            // Company profile remains the business source of truth; Keycloak sync is retried on the next update.
        }
    }
}
