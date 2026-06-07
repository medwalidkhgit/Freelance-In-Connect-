package org.sid.company_service.Repository;

import org.sid.company_service.Entity.Company;
import org.sid.company_service.Entity.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyServiceRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByKeycloakId(String keycloakId);

    Optional<Company> findByCompanyEmail(String email);

    List<Company> findByStatus(CompanyStatus status);
}
