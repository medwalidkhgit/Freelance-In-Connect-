package org.sid.auth_service.DTO;

import lombok.Data;

@Data
public class CompanyResponse {
    private Long id;
    private String keycloakUserId;
    private String email;
    private String companyName;
    private String status;
    // autres champs selon réponse de company-service
}