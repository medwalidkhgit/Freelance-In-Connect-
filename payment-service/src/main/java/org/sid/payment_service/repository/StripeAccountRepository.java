package org.sid.payment_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.sid.payment_service.entity.StripeAccount;
import org.sid.payment_service.entity.StripeAccountOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeAccountRepository extends JpaRepository<StripeAccount, UUID> {
    Optional<StripeAccount> findByOwnerTypeAndOwnerId(StripeAccountOwnerType ownerType, String ownerId);
}

