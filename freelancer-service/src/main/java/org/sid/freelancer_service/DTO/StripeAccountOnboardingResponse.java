package org.sid.freelancer_service.DTO;

public class StripeAccountOnboardingResponse {
    private String accountId;
    private String onboardingUrl;

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

