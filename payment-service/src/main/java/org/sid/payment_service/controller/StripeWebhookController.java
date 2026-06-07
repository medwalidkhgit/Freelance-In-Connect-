package org.sid.payment_service.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import org.sid.payment_service.service.StripeWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stripe/webhook")
public class StripeWebhookController {
    private final StripeWebhookService webhookService;

    public StripeWebhookController(StripeWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<Void> handle(@RequestBody String payload,
                                       @RequestHeader(value = "Stripe-Signature", required = false) String signature)
            throws StripeException {
        webhookService.handleEvent(payload, signature);
        return ResponseEntity.ok().build();
    }
}

