package org.sid.payment_service.dto;

public class PaymentResponse {
    private String id;
    private String status;
    private String clientSecret;
    private Long amountCents;
    private Long platformFeeCents;
    private Long netAmountCents;
    private String currency;
    private String stripePaymentIntentId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }

    public Long getPlatformFeeCents() {
        return platformFeeCents;
    }

    public void setPlatformFeeCents(Long platformFeeCents) {
        this.platformFeeCents = platformFeeCents;
    }

    public Long getNetAmountCents() {
        return netAmountCents;
    }

    public void setNetAmountCents(Long netAmountCents) {
        this.netAmountCents = netAmountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }
}

