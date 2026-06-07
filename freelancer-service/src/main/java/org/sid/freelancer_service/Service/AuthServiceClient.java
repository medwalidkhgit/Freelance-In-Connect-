package org.sid.freelancer_service.Service;

import org.sid.freelancer_service.DTO.KeycloakProfileUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", url = "${services.auth.url:http://auth-service:8081}")
public interface AuthServiceClient {

    @PutMapping("/auth/internal/users/{userId}/profile")
    void updateKeycloakProfile(@PathVariable("userId") String userId,
                               @RequestBody KeycloakProfileUpdateRequest request);
}
