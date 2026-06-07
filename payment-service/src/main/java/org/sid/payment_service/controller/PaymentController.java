package org.sid.payment_service.controller;

import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import org.sid.payment_service.dto.CreatePaymentRequest;
import org.sid.payment_service.dto.PaymentResponse;
import org.sid.payment_service.service.StripePaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final StripePaymentService stripePaymentService;

    public PaymentController(StripePaymentService stripePaymentService) {
        this.stripePaymentService = stripePaymentService;
    }

    @PostMapping("/mission/{missionId}")
    public ResponseEntity<PaymentResponse> createPayment(@PathVariable String missionId,
                                                         @Valid @RequestBody CreatePaymentRequest request,
                                                         Authentication authentication)
            throws StripeException {
        return ResponseEntity.ok(stripePaymentService.createPaymentForMissionComplete(missionId, request, authentication));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId,
                                                      Authentication authentication) {
        return ResponseEntity.ok(stripePaymentService.getPayment(paymentId, authentication));
    }
}

