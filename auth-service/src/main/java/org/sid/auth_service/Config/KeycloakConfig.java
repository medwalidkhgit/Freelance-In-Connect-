package org.sid.auth_service.Config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.OAuth2Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class KeycloakConfig {

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin-client-id}") // L'ID du client admin
    private String adminClientId;

    @Value("${keycloak.admin-client-secret}") // La clé secrète du client admin
    private String adminClientSecret;

    @Value("${keycloak.http.connect-timeout-ms:3000}")
    private long connectTimeoutMs;

    @Value("${keycloak.http.read-timeout-ms:5000}")
    private long readTimeoutMs;

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS) // Connexion en mode "Client Credentials" pour les interactions d'administration
                .clientId(adminClientId)
                .clientSecret(adminClientSecret)
                .build();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

}