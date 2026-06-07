package org.sid.payment_service.dto;

public class CreateStripeAccountResponse {
    private String accountId;
    private String onboardingUrl;

    public CreateStripeAccountResponse() {
    }

    public CreateStripeAccountResponse(String accountId, String onboardingUrl) {
        this.accountId = accountId;
        this.onboardingUrl = onboardingUrl;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getOnboardingUrl() {
        return onboardingUrl;
    }

    public void setOnboardingUrl(String onboardingUrl) {
        this.onboardingUrl = onboardingUrl;
    }
}

