package org.sid.company_service.DTO;

import lombok.Data;

@Data
public class CompanyUpdateRequest {
    private String companyName;
    private String contactFirstName;
    private String contactLastName;
    private String companyAddress;
    private String companyPhone;
    private String companyFax;
    private String domaine;
    private String pfpUrl;
}
