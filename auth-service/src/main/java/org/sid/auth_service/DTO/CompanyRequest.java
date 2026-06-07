package org.sid.auth_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
