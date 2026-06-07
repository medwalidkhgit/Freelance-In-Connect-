package org.sid.payment_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.sid.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
}

