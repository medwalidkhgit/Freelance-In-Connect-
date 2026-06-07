package org.sid.application_service.dto;

import lombok.Data;

@Data
public class CompanyResponse {
    private Long id;
    private String keycloakId;
    private String companyEmail;
    private String companyName;
    private String status;
}
