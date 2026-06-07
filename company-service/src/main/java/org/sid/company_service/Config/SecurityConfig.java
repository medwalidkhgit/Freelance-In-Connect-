package org.sid.company_service.Config;

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
                        .requestMatchers(HttpMethod.POST, "/api/companies").hasRole("INTERNAL")
                        .requestMatchers(HttpMethod.GET, "/api/companies/internal/**").hasRole("INTERNAL")
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/companies/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/companies/me/**").authenticated()
                        .requestMatchers("/api/companies/missions/**").hasRole("COMPANY")
                        .requestMatchers(HttpMethod.GET, "/api/companies/{id:\\d+}").permitAll() // profil public
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new KeycloakJwtAuthenticationConverter()
                        ))
                )
                .addFilterBefore(internalServiceTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
