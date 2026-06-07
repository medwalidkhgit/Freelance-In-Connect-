package org.sid.payment_service.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import org.sid.payment_service.config.StripeProperties;
import org.sid.payment_service.dto.CreateStripeAccountRequest;
import org.sid.payment_service.dto.CreateStripeAccountResponse;
import org.sid.payment_service.entity.StripeAccount;
import org.sid.payment_service.entity.StripeAccountOwnerType;
import org.sid.payment_service.repository.StripeAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class StripeAccountService {
    private final StripeAccountRepository stripeAccountRepository;
    private final StripeProperties stripeProperties;

    public StripeAccountService(StripeAccountRepository stripeAccountRepository, StripeProperties stripeProperties) {
        this.stripeAccountRepository = stripeAccountRepository;
        this.stripeProperties = stripeProperties;
    }

    @Transactional
    public CreateStripeAccountResponse createAccount(CreateStripeAccountRequest request) throws StripeException {
        StripeAccountOwnerType ownerType = StripeAccountOwnerType.valueOf(request.getOwnerType().toUpperCase());
        return createAccountForOwner(ownerType, request.getOwnerId(), request.getEmail());
    }

    @Transactional
    public CreateStripeAccountResponse createFreelancerAccount(String freelancerId, String email) throws StripeException {
        return createAccountForOwner(StripeAccountOwnerType.FREELANCER, freelancerId, email);
    }

    private CreateStripeAccountResponse createAccountForOwner(StripeAccountOwnerType ownerType,
                                                             String ownerId,
                                                             String email) throws StripeException {
        assertStripeConfigured();

        Optional<StripeAccount> existing = stripeAccountRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId);
        if (existing.isPresent()) {
            return createOnboardingResponse(existing.get().getStripeAccountId());
        }
        return createNewAccount(ownerType, ownerId, email);
    }

    private CreateStripeAccountResponse createNewAccount(StripeAccountOwnerType ownerType,
                                                         String ownerId,
                                                         String email) throws StripeException {
        AccountCreateParams.Builder accountBuilder = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setCapabilities(
                        AccountCreateParams.Capabilities.builder()
                                .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder().setRequested(true).build())
                                .setTransfers(AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build())
                                .build()
                );
        if (email != null && !email.isBlank()) {
            accountBuilder.setEmail(email);
        }
        Account account = Account.create(accountBuilder.build());

        StripeAccount stripeAccount = new StripeAccount();
        stripeAccount.setOwnerType(ownerType);
        stripeAccount.setOwnerId(ownerId);
        stripeAccount.setStripeAccountId(account.getId());
        stripeAccountRepository.save(stripeAccount);

        return createOnboardingResponse(account.getId());
    }

    private CreateStripeAccountResponse createOnboardingResponse(String stripeAccountId) throws StripeException {
        AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                .setAccount(stripeAccountId)
                .setRefreshUrl(stripeProperties.getAccountLinkRefreshUrl())
                .setReturnUrl(stripeProperties.getAccountLinkReturnUrl())
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();
        AccountLink link = AccountLink.create(linkParams);
        return new CreateStripeAccountResponse(stripeAccountId, link.getUrl());
    }

    private void assertStripeConfigured() {
        String secretKey = stripeProperties.getSecretKey();
        if (secretKey == null || secretKey.isBlank() || "sk_test_xxx".equals(secretKey)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Stripe n'est pas configure: renseignez STRIPE_SECRET_KEY avec une vraie cle de test Stripe."
            );
        }
    }
}
