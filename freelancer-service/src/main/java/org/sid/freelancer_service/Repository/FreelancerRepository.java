package org.sid.freelancer_service.Repository;

import org.sid.freelancer_service.Entity.Freelancer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FreelancerRepository extends JpaRepository<Freelancer, Long> {
        boolean existsByEmail(String email);
        boolean existsByKeycloakUserId(String keycloakUserId);
        Optional<Freelancer> findByEmail(String email);
        Freelancer findByKeycloakUserId(String keycloakUserId);
        Optional<Freelancer> findByKeycloakUserIdAndSuspendedFalse(String keycloakUserId);
        Page<Freelancer> findBySuspendedFalse(Pageable pageable);
        Optional<Freelancer> findByIdAndSuspendedFalse(Long id);
}

