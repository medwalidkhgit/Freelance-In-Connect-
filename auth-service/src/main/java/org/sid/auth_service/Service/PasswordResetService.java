package org.sid.auth_service.Service;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.sid.auth_service.Client.CompanyServiceClient;
import org.sid.auth_service.Client.FreelancerServiceClient;
import org.sid.auth_service.DTO.ForgotPasswordRequest;
import org.sid.auth_service.DTO.ResetPasswordRequest;
import org.sid.auth_service.DTO.VerifyOtpRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PasswordResetService {

    private final CompanyServiceClient companyServiceClient;
    private final FreelancerServiceClient freelancerServiceClient;
    private final KeycloakService keycloakService;
    private final OtpMailService otpMailService;
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.ttl-minutes:10}")
    private long otpTtlMinutes;

    public PasswordResetService(CompanyServiceClient companyServiceClient,
                                FreelancerServiceClient freelancerServiceClient,
                                KeycloakService keycloakService,
                                OtpMailService otpMailService) {
        this.companyServiceClient = companyServiceClient;
        this.freelancerServiceClient = freelancerServiceClient;
        this.keycloakService = keycloakService;
        this.otpMailService = otpMailService;
    }

    public ResponseEntity<String> requestOtp(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (!emailExists(email)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email introuvable.");
        }

        String otp = generateOtp();
        otpStore.put(email, new OtpEntry(otp, Instant.now().plusSeconds(otpTtlMinutes * 60), false));
        try {
            sendOtp(email, otp);
        } catch (MailException e) {
            otpStore.remove(email);
            log.error("Envoi OTP impossible pour {} : {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Envoi OTP impossible. Verifiez la configuration SMTP.");
        }
        return ResponseEntity.ok("Code OTP envoye.");
    }

    public ResponseEntity<String> verifyOtp(VerifyOtpRequest request) {
        return isValidOtp(normalizeEmail(request.getEmail()), request.getOtp(), false)
                ? ResponseEntity.ok("Code OTP valide.")
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Code OTP invalide ou expire.");
    }

    public ResponseEntity<String> resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Les mots de passe ne correspondent pas.");
        }
        if (!isValidOtp(email, request.getOtp(), true)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Code OTP invalide ou expire.");
        }
        if (!keycloakService.resetPasswordByEmail(email, request.getNewPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Reinitialisation Keycloak impossible.");
        }

        otpStore.remove(email);
        return ResponseEntity.ok("Mot de passe reinitialise.");
    }

    private boolean emailExists(String email) {
        return existsInCompany(email) || existsInFreelancer(email);
    }

    private boolean existsInCompany(String email) {
        try {
            companyServiceClient.companyEmailExists(email);
            return true;
        } catch (FeignException.NotFound e) {
            return false;
        } catch (FeignException e) {
            log.warn("Verification email company impossible pour {} : HTTP {}", email, e.status());
            return false;
        }
    }

    private boolean existsInFreelancer(String email) {
        try {
            freelancerServiceClient.freelancerEmailExists(email);
            return true;
        } catch (FeignException.NotFound e) {
            return false;
        } catch (FeignException e) {
            log.warn("Verification email freelancer impossible pour {} : HTTP {}", email, e.status());
            return false;
        }
    }

    private boolean isValidOtp(String email, String otp, boolean consume) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null || entry.consumed() || entry.expiresAt().isBefore(Instant.now())) {
            otpStore.remove(email);
            return false;
        }
        boolean valid = entry.code().equals(otp);
        if (valid && consume) {
            otpStore.put(email, new OtpEntry(entry.code(), entry.expiresAt(), true));
        }
        return valid;
    }

    private void sendOtp(String email, String otp) {
        otpMailService.sendOtp(
                email,
                "FIC - Code de reinitialisation",
                "Votre code OTP FIC est : " + otp + "\nIl expire dans " + otpTtlMinutes + " minutes.",
                otp
        );
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private record OtpEntry(String code, Instant expiresAt, boolean consumed) {
    }
}
