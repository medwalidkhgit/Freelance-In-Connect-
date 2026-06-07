package org.sid.auth_service.Service;

import lombok.extern.slf4j.Slf4j;
import org.sid.auth_service.DTO.AuthResponse;
import org.sid.auth_service.DTO.LoginOtpResponse;
import org.sid.auth_service.DTO.LoginOtpResendRequest;
import org.sid.auth_service.DTO.LoginOtpVerifyRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LoginOtpService {

    private final OtpMailService otpMailService;
    private final KeycloakService keycloakService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, LoginChallenge> challenges = new ConcurrentHashMap<>();

    @Value("${app.otp.ttl-minutes:10}")
    private long otpTtlMinutes;

    public LoginOtpService(OtpMailService otpMailService, KeycloakService keycloakService) {
        this.otpMailService = otpMailService;
        this.keycloakService = keycloakService;
    }

    public ResponseEntity<?> createChallenge(String email, AuthResponse authResponse) {
        String normalizedEmail = normalizeEmail(email);
        String challengeId = UUID.randomUUID().toString();
        String otp = generateOtp();
        Instant expiresAt = Instant.now().plusSeconds(otpTtlMinutes * 60);
        LoginChallenge challenge = new LoginChallenge(normalizedEmail, otp, expiresAt, authResponse);
        challenges.put(challengeId, challenge);

        try {
            sendLoginOtp(normalizedEmail, otp);
        } catch (MailException e) {
            challenges.remove(challengeId);
            keycloakService.logout(authResponse.getRefreshToken());
            log.error("Envoi OTP de connexion impossible pour {} : {}", normalizedEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Envoi OTP impossible. Verifiez la configuration SMTP.");
        }

        return ResponseEntity.ok(new LoginOtpResponse(
                true,
                challengeId,
                normalizedEmail,
                "Code OTP envoye.",
                otpTtlMinutes * 60
        ));
    }

    public ResponseEntity<?> verify(LoginOtpVerifyRequest request) {
        LoginChallenge challenge = challenges.get(request.getChallengeId());
        if (challenge == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Session OTP introuvable ou expiree.");
        }
        if (challenge.expiresAt().isBefore(Instant.now())) {
            challenges.remove(request.getChallengeId());
            keycloakService.logout(challenge.authResponse().getRefreshToken());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Code OTP expire. Veuillez recommencer la connexion.");
        }
        if (!challenge.otp().equals(request.getOtp())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Code OTP invalide.");
        }

        challenges.remove(request.getChallengeId());
        return ResponseEntity.ok(challenge.authResponse());
    }

    public ResponseEntity<?> resend(LoginOtpResendRequest request) {
        LoginChallenge challenge = challenges.get(request.getChallengeId());
        if (challenge == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Session OTP introuvable ou expiree.");
        }
        if (challenge.expiresAt().isBefore(Instant.now())) {
            challenges.remove(request.getChallengeId());
            keycloakService.logout(challenge.authResponse().getRefreshToken());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Code OTP expire. Veuillez recommencer la connexion.");
        }

        String otp = generateOtp();
        Instant expiresAt = Instant.now().plusSeconds(otpTtlMinutes * 60);
        challenges.put(request.getChallengeId(),
                new LoginChallenge(challenge.email(), otp, expiresAt, challenge.authResponse()));

        try {
            sendLoginOtp(challenge.email(), otp);
        } catch (MailException e) {
            log.error("Renvoi OTP de connexion impossible pour {} : {}", challenge.email(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Renvoi OTP impossible. Verifiez la configuration SMTP.");
        }

        return ResponseEntity.ok("Nouveau code OTP envoye.");
    }

    public ResponseEntity<?> getDevOtp(String challengeId) {
        LoginChallenge challenge = challenges.get(challengeId);
        if (challenge == null || challenge.expiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("OTP introuvable ou expire.");
        }
        return ResponseEntity.ok(Map.of(
                "challengeId", challengeId,
                "email", challenge.email(),
                "otp", challenge.otp(),
                "expiresAt", challenge.expiresAt().toString()
        ));
    }

    private void sendLoginOtp(String email, String otp) {
        otpMailService.sendOtp(
                email,
                "FIC - Code de connexion",
                "Votre code OTP de connexion FIC est : " + otp
                        + "\nIl expire dans " + otpTtlMinutes + " minutes.",
                otp
        );
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private record LoginChallenge(String email, String otp, Instant expiresAt, AuthResponse authResponse) {
    }
}
