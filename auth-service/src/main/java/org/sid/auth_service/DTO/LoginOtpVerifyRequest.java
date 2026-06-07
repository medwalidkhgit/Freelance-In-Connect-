package org.sid.auth_service.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginOtpVerifyRequest {
    @NotBlank
    private String challengeId;

    @NotBlank
    private String otp;
}
