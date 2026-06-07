package org.sid.payment_service.controller;

import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import org.sid.payment_service.dto.CreateFreelancerStripeAccountRequest;
import org.sid.payment_service.dto.CreateStripeAccountRequest;
import org.sid.payment_service.dto.CreateStripeAccountResponse;
import org.sid.payment_service.service.StripeAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/stripe/accounts")
public class StripeAccountController {
    private final StripeAccountService stripeAccountService;

    public StripeAccountController(StripeAccountService stripeAccountService) {
        this.stripeAccountService = stripeAccountService;
    }

    @PostMapping
    public ResponseEntity<CreateStripeAccountResponse> createAccount(@Valid @RequestBody CreateStripeAccountRequest request)
            throws StripeException {
        return ResponseEntity.ok(stripeAccountService.createAccount(request));
    }

    @PostMapping("/freelancers/{freelancerId}")
    public ResponseEntity<CreateStripeAccountResponse> createFreelancerAccount(
            @PathVariable String freelancerId,
            @RequestBody CreateFreelancerStripeAccountRequest request) throws StripeException {
        return ResponseEntity.ok(stripeAccountService.createFreelancerAccount(freelancerId, request.getEmail()));
    }
}
