package org.sid.auth_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginOtpResponse {
    private boolean otpRequired;
    private String challengeId;
    private String email;
    private String message;
    private long expiresInSeconds;
}
