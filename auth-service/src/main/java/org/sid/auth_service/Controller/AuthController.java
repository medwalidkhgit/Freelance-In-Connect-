package org.sid.auth_service.Controller;

import org.sid.auth_service.DTO.AuthRequest;
import org.sid.auth_service.DTO.FreelancerRegisterRequest;
import org.sid.auth_service.DTO.CompanyRegisterRequest;
import org.sid.auth_service.DTO.ForgotPasswordRequest;
import org.sid.auth_service.DTO.LoginOtpResendRequest;
import org.sid.auth_service.DTO.LoginOtpVerifyRequest;
import org.sid.auth_service.DTO.KeycloakProfileUpdateRequest;
import org.sid.auth_service.DTO.RefreshTokenRequest;
import org.sid.auth_service.DTO.ResetPasswordRequest;
import org.sid.auth_service.DTO.VerifyOtpRequest;
import org.sid.auth_service.Service.AuthService;
import org.sid.auth_service.Service.KeycloakService;
import org.sid.auth_service.Service.LoginOtpService;
import org.sid.auth_service.Service.PasswordResetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final KeycloakService keycloakService;
    private final PasswordResetService passwordResetService;
    private final LoginOtpService loginOtpService;

    @Value("${internal.service-token:}")
    private String internalServiceToken;

    @Value("${app.dev-otp.enabled:false}")
    private boolean devOtpEnabled;

    public AuthController(AuthService authService,
                          KeycloakService keycloakService,
                          PasswordResetService passwordResetService,
                          LoginOtpService loginOtpService) {
        this.authService = authService;
        this.keycloakService = keycloakService;
        this.passwordResetService = passwordResetService;
        this.loginOtpService = loginOtpService;
    }

    @PostMapping("/freelancer/register")
    public ResponseEntity<String> registerFreelancer(@Valid @RequestBody FreelancerRegisterRequest request) {
        return authService.registerFreelancer(request);
    }

    @PostMapping("/company/register")
    public ResponseEntity<String> registerCompany(@Valid @RequestBody CompanyRegisterRequest request) {
        return authService.registerCompany(request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/login/verify-otp")
    public ResponseEntity<?> verifyLoginOtp(@Valid @RequestBody LoginOtpVerifyRequest request) {
        return loginOtpService.verify(request);
    }

    @PostMapping("/login/resend-otp")
    public ResponseEntity<?> resendLoginOtp(@Valid @RequestBody LoginOtpResendRequest request) {
        return loginOtpService.resend(request);
    }

    @GetMapping("/dev/login-otp/{challengeId}")
    public ResponseEntity<?> getDevLoginOtp(@PathVariable String challengeId) {
        if (!devOtpEnabled) {
            return ResponseEntity.notFound().build();
        }
        return loginOtpService.getDevOtp(challengeId);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return keycloakService.refreshToken(request.getRefreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body == null ? null : body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("refreshToken is required");
        }
        return authService.logout(refreshToken);
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwordResetService.requestOtp(request);
    }

    @PostMapping("/password/verify-otp")
    public ResponseEntity<String> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return passwordResetService.verifyOtp(request);
    }

    @PostMapping("/password/reset")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return passwordResetService.resetPassword(request);
    }

    @DeleteMapping("/internal/users/{userId}")
    public ResponseEntity<?> deleteKeycloakUser(
            @PathVariable String userId,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (internalServiceToken == null || internalServiceToken.isBlank()
                || token == null || !internalServiceToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid internal token");
        }
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("userId is required");
        }
        boolean deleted = keycloakService.deleteUser(userId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Keycloak user deletion failed");
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/internal/users/{userId}/profile")
    public ResponseEntity<?> updateKeycloakProfile(
            @PathVariable String userId,
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody KeycloakProfileUpdateRequest request) {
        if (internalServiceToken == null || internalServiceToken.isBlank()
                || token == null || !internalServiceToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid internal token");
        }
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("userId is required");
        }
        boolean updated = keycloakService.updateUserProfile(
                userId,
                request == null ? null : request.getFirstName(),
                request == null ? null : request.getLastName(),
                request == null ? null : request.getPhone(),
                request == null ? null : request.getCompanyName()
        );
        if (!updated) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Keycloak profile update failed");
        }
        return ResponseEntity.noContent().build();
    }
}
