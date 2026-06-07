package org.sid.company_service.DTO;

import lombok.Data;

@Data
public class CompanyRequest {
    private String keycloakUserId;
    private String email;
    private String companyName;
    private String siret;
    private String contactFirstName;
    private String contactLastName;
    private String companyAddress;
    private String companyPhone;
    private String domaine;
    private String status;
    private String pfpUrl;
}
