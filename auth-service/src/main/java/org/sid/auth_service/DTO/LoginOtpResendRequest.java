package org.sid.auth_service.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginOtpResendRequest {
    @NotBlank
    private String challengeId;
}
