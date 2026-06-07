package org.sid.company_service.Config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FeignInterceptor implements RequestInterceptor {

    @Value("${internal.service-token}")
    private String internalServiceToken;

    @Override
    public void apply(RequestTemplate template) {
        template.header("X-Internal-Token", internalServiceToken);
    }
}
