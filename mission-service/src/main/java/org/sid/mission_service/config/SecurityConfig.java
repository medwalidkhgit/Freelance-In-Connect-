package org.sid.mission_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

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
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/missions/admin/**").hasRole("INTERNAL")
                        .requestMatchers(HttpMethod.GET, "/api/missions/company/*/all").hasRole("INTERNAL")
                        .requestMatchers(HttpMethod.GET, "/api/missions/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/missions/**").hasRole("INTERNAL")
                        .requestMatchers(HttpMethod.PUT, "/api/missions/**").hasRole("INTERNAL")
                        .requestMatchers(HttpMethod.DELETE, "/api/missions/**").hasRole("INTERNAL")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new KeycloakJwtAuthenticationConverter()
                        ))
                )
                .addFilterAfter(internalServiceTokenFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
