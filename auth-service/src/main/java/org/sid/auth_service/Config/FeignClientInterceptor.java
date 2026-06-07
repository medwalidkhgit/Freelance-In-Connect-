package org.sid.auth_service.Config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Value("${internal.service-token:}")
    private String internalServiceToken;

    @Override
    public void apply(RequestTemplate template) {
        if (internalServiceToken != null && !internalServiceToken.isBlank()) {
            template.header("X-Internal-Token", internalServiceToken);
        }

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            String authHeader = attributes.getRequest()
                    .getHeader("Authorization");
            if (authHeader != null) {
                template.header("Authorization", authHeader);
            }
        }
    }
}
