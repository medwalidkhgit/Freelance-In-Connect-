package org.sid.admin_service.services;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${services.auth.url:http://auth-service:8081}")
public interface AuthServiceClient {

    @DeleteMapping("/auth/internal/users/{userId}")
    void deleteKeycloakUser(@PathVariable("userId") String userId);
}
