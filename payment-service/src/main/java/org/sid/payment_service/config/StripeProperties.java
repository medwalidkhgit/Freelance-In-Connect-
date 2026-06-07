package org.sid.payment_service.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {
    private String secretKey;
    private String webhookSecret;
    private BigDecimal platformFeePercent = BigDecimal.TEN;
    private String defaultCurrency = "usd";
    private String accountLinkRefreshUrl = "http://localhost:8088/api/stripe/accounts/refresh";
    private String accountLinkReturnUrl = "http://localhost:8088/api/stripe/accounts/return";

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public BigDecimal getPlatformFeePercent() {
        return platformFeePercent;
    }

    public void setPlatformFeePercent(BigDecimal platformFeePercent) {
        this.platformFeePercent = platformFeePercent;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public String getAccountLinkRefreshUrl() {
        return accountLinkRefreshUrl;
    }

    public void setAccountLinkRefreshUrl(String accountLinkRefreshUrl) {
        this.accountLinkRefreshUrl = accountLinkRefreshUrl;
    }

    public String getAccountLinkReturnUrl() {
        return accountLinkReturnUrl;
    }

    public void setAccountLinkReturnUrl(String accountLinkReturnUrl) {
        this.accountLinkReturnUrl = accountLinkReturnUrl;
    }
}

