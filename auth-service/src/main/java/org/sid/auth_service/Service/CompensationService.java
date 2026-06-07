package org.sid.auth_service.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class CompensationService {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 500;

    private final KeycloakService keycloakService;
    private final ConcurrentLinkedQueue<FailedCompensation> failedCompensations =
            new ConcurrentLinkedQueue<>();

    public CompensationService(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    public void compensate(String userId, String email) {
        log.warn("Démarrage compensation Keycloak pour userId={} email={}", userId, email);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            boolean deleted = keycloakService.deleteUser(userId);

            if (deleted) {
                log.info("Compensation Keycloak réussie pour userId={} (tentative {})", userId, attempt);
                return;
            }

            log.warn("Compensation Keycloak échouée tentative {}/{} pour userId={}", attempt, MAX_RETRIES, userId);

            if (attempt < MAX_RETRIES) {
                long delay = RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1); // 500ms, 1000ms, 2000ms
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Retry interrompu pour userId={}", userId);
                    break;
                }
            }
        }

        FailedCompensation failed = new FailedCompensation(userId, email, System.currentTimeMillis());
        failedCompensations.add(failed);
        log.error("ALERTE CRITIQUE — Compensation définitivement échouée : userId={} email={}. " +
                "Enregistré pour intervention. Total en attente : {}", userId, email, failedCompensations.size());
    }

    public ConcurrentLinkedQueue<FailedCompensation> getFailedCompensations() {
        return failedCompensations;
    }

    public record FailedCompensation(String userId, String email, long failedAtMs) {}
}