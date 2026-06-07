package org.sid.company_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sid.company_service.Entity.CompanyStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyMeDTO {
    private Long id;
    private String companyName;
    private String companyEmail;
    private String companyAddress;
    private String companyPhone;
    private String companyFax;
    private String domaine;
    private String pfpUrl;
    private String siret;
    private CompanyStatus status;
    // rejectionReason visible uniquement si statut = Rejected
    private String rejectionReason;
}
