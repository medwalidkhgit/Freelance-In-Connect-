package org.sid.application_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
public class FeignBearerTokenInterceptor {

    @Value("${internal.service-token:}")
    private String internalServiceToken;

    @Bean
    public RequestInterceptor bearerTokenRequestInterceptor() {
        return template -> {
            if (internalServiceToken != null && !internalServiceToken.isBlank()) {
                template.header("X-Internal-Token", internalServiceToken);
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                template.header("Authorization", "Bearer " + jwt.getTokenValue());
            }
        };
    }
}
