package org.sid.application_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/applications").hasRole("FREELANCER")
                        .requestMatchers(HttpMethod.GET, "/api/applications/me/**").hasRole("FREELANCER")
                        .requestMatchers(HttpMethod.GET, "/api/applications/mission/**").hasRole("COMPANY")
                        .requestMatchers(HttpMethod.GET, "/api/applications/company/**").hasRole("COMPANY")
                        .requestMatchers(HttpMethod.PUT, "/api/applications/{id:\\d+}/status").hasRole("COMPANY")
                        .requestMatchers(HttpMethod.GET, "/api/applications/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter()))
                );
        return http.build();
    }
}
