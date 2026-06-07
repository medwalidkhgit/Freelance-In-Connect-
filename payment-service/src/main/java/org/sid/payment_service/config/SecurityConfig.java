package org.sid.payment_service.config;

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
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/stripe/webhook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/mission/**")
                        .hasAnyRole("INTERNAL", "COMPANY", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/payments/**")
                        .hasAnyRole("INTERNAL", "COMPANY", "FREELANCER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/stripe/accounts/freelancers/**")
                        .hasAnyRole("INTERNAL", "FREELANCER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/stripe/accounts")
                        .hasAnyRole("INTERNAL", "ADMIN")
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter()))
                )
                .addFilterBefore(internalServiceTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
