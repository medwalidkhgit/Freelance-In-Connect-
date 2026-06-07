package org.sid.freelancer_service.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final InternalServiceTokenFilter internalServiceTokenFilter;

    public SecurityConfig(InternalServiceTokenFilter internalServiceTokenFilter) {
        this.internalServiceTokenFilter = internalServiceTokenFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // Actuator
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Appel interne depuis auth-service
                        .requestMatchers(HttpMethod.POST, "/api/freelances").hasRole("INTERNAL")
                        .requestMatchers(HttpMethod.GET, "/api/freelances/internal/**").hasRole("INTERNAL")

                        // Profil personnel freelancer
                        .requestMatchers(HttpMethod.GET, "/api/freelances/me").hasRole("FREELANCER")
                        .requestMatchers(HttpMethod.PUT, "/api/freelances/me").hasRole("FREELANCER")

                        // Admin uniquement
                        .requestMatchers(HttpMethod.GET, "/api/freelances/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/freelances/{id:\\d+}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/freelances/{id:\\d+}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/freelances/{id:\\d+}/suspend").hasRole("ADMIN")

                        // Missions d'une company
                        .requestMatchers(HttpMethod.GET, "/api/freelances/missions/company/*")
                        .hasAnyRole("COMPANY", "ADMIN")

                        // Missions générales
                        .requestMatchers(HttpMethod.GET, "/api/freelances/missions")
                        .hasAnyRole("FREELANCER", "COMPANY", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/freelances/missions/**")
                        .hasAnyRole("FREELANCER", "COMPANY", "ADMIN")

                        // Liste des profils freelancers
                        .requestMatchers(HttpMethod.GET, "/api/freelances")
                        .hasRole("ADMIN")

                        // Voir un profil freelancer par id
                        .requestMatchers(HttpMethod.GET, "/api/freelances/{id:\\d+}")
                        .hasAnyRole("FREELANCER", "COMPANY", "ADMIN")

                        // Tout le reste interdit
                        .anyRequest().denyAll()
                )

                // IMPORTANT : validation du JWT Keycloak
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new KeycloakJwtAuthenticationConverter()
                        ))
                )

                // Filtre pour les appels internes auth-service -> freelancer-service
                .addFilterBefore(
                        internalServiceTokenFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
