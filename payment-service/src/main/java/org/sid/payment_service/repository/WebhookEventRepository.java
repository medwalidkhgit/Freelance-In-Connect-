package org.sid.payment_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.sid.payment_service.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    Optional<WebhookEvent> findByStripeEventId(String stripeEventId);
}

