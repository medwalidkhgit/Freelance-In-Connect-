package org.sid.payment_service.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateStripeAccountRequest {
    @NotBlank
    private String ownerType;
    @NotBlank
    private String ownerId;
    private String email;

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

