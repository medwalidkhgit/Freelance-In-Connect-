package org.sid.messaging_service.security;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

public record MessagingUser(Jwt jwt, String keycloakId, boolean admin, boolean company, boolean freelancer,
                            Long companyId, Long freelancerId) {

    public static MessagingUser fromJwt(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return new MessagingUser(
                jwt,
                jwt.getSubject(),
                hasRole(jwt, "ADMIN"),
                hasRole(jwt, "COMPANY"),
                hasRole(jwt, "FREELANCER"),
                firstLongClaim(jwt, "companyId", "company_id", "companyProfileId", "company_profile_id", "profileId", "profile_id", "userId", "user_id"),
                firstLongClaim(jwt, "freelancerId", "freelancer_id", "freelancerProfileId", "freelancer_profile_id", "profileId", "profile_id", "userId", "user_id")
        );
    }

    public static MessagingUser fromPrincipal(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof Jwt jwt) {
            return fromJwt(jwt);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    public Long senderId() {
        if (company) {
            return companyId;
        }
        if (freelancer) {
            return freelancerId;
        }
        return null;
    }

    public String senderRole() {
        if (company) {
            return "COMPANY";
        }
        if (freelancer) {
            return "FREELANCER";
        }
        if (admin) {
            return "ADMIN";
        }
        return "UNKNOWN";
    }

    public boolean hasKeycloakId() {
        return keycloakId != null && !keycloakId.isBlank();
    }

    private static boolean hasRole(Jwt jwt, String expectedRole) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return false;
        }
        return roles.stream()
                .map(Object::toString)
                .anyMatch(role -> role.equals(expectedRole));
    }

    private static Long firstLongClaim(Jwt jwt, String... claimNames) {
        for (String claimName : claimNames) {
            Long value = toLong(jwt.getClaim(claimName));
            if (value != null) {
                return value;
            }
        }
        return toLong(jwt.getSubject());
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (value instanceof List<?> values && !values.isEmpty()) {
            return toLong(values.get(0));
        }
        return null;
    }
}
