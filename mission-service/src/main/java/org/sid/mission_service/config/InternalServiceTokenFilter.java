package org.sid.mission_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class InternalServiceTokenFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Internal-Token";

    @Value("${internal.service-token}")
    private String internalServiceToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedToken = request.getHeader(HEADER_NAME);
        if (internalServiceToken != null
                && !internalServiceToken.isBlank()
                && internalServiceToken.equals(providedToken)) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            "internal-service",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
