package org.sid.freelancer_service.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {
    @Bean
    public RestTemplate restTemplate(@Value("${internal.service-token:}") String internalServiceToken) {
        RestTemplate restTemplate = new RestTemplate();
        if (internalServiceToken != null && !internalServiceToken.isBlank()) {
            restTemplate.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add("X-Internal-Token", internalServiceToken);
                return execution.execute(request, body);
            });
        }
        return restTemplate;
    }
}

