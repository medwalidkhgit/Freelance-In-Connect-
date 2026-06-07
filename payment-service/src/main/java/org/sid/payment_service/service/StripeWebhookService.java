package org.sid.payment_service.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.sid.payment_service.config.StripeProperties;
import org.sid.payment_service.entity.WebhookEvent;
import org.sid.payment_service.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class StripeWebhookService {
    private final StripeProperties stripeProperties;
    private final WebhookEventRepository webhookEventRepository;
    private final StripePaymentService stripePaymentService;

    public StripeWebhookService(StripeProperties stripeProperties,
                                WebhookEventRepository webhookEventRepository,
                                StripePaymentService stripePaymentService) {
        this.stripeProperties = stripeProperties;
        this.webhookEventRepository = webhookEventRepository;
        this.stripePaymentService = stripePaymentService;
    }

    @Transactional
    public void handleEvent(String payload, String signatureHeader) throws StripeException {
        Event event = constructEvent(payload, signatureHeader);
        Optional<WebhookEvent> existing = webhookEventRepository.findByStripeEventId(event.getId());
        if (existing.isPresent()) {
            return;
        }

        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setStripeEventId(event.getId());
        webhookEvent.setEventType(event.getType());
        webhookEvent.setPayload(payload);
        webhookEventRepository.save(webhookEvent);

        if ("payment_intent.succeeded".equals(event.getType())) {
            getPaymentIntent(event).ifPresent(intent -> stripePaymentService.markSucceeded(intent.getId()));
        } else if ("payment_intent.payment_failed".equals(event.getType()) || "payment_intent.canceled".equals(event.getType())) {
            getPaymentIntent(event).ifPresent(intent -> stripePaymentService.markFailed(intent.getId()));
        }
    }

    private Event constructEvent(String payload, String signatureHeader) throws StripeException {
        String secret = stripeProperties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Stripe webhook secret is not configured");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new IllegalArgumentException("Stripe signature header is required");
        }
        return Webhook.constructEvent(payload, signatureHeader, secret);
    }

    private Optional<PaymentIntent> getPaymentIntent(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            return Optional.of((PaymentIntent) deserializer.getObject().get());
        }
        return Optional.empty();
    }
}

