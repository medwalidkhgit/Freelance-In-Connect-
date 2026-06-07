package org.sid.payment_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sid.payment_service.dto.CreatePaymentRequest;
import org.sid.payment_service.dto.PaymentResponse;
import org.sid.payment_service.service.StripeAccountService;
import org.sid.payment_service.service.StripePaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_security_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "stripe.secret-key=sk_test_security",
        "stripe.webhook-secret=whsec_test_security",
        "internal.service-token=test-internal-token",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/b2b-platform"
})
@AutoConfigureMockMvc
class PaymentSecurityTest {

    private static final String PAYMENT_BODY = """
            {
              "companyId": "company-1",
              "freelancerId": "freelancer-1",
              "amountCents": 1000
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StripePaymentService stripePaymentService;

    @MockitoBean
    private StripeAccountService stripeAccountService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void createPaymentWithoutAuthenticationIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/payments/mission/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPaymentWithInvalidInternalTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/payments/mission/1")
                        .header("X-Internal-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPaymentWithValidInternalTokenIsAllowed() throws Exception {
        when(stripePaymentService.createPaymentForMissionComplete(
                eq("1"), any(CreatePaymentRequest.class), any(Authentication.class)))
                .thenReturn(paymentResponse());

        mockMvc.perform(post("/api/payments/mission/1")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void createPaymentWithCompanyJwtIsAllowed() throws Exception {
        when(jwtDecoder.decode("company-token")).thenReturn(jwtWithRole("company-token", "company-1", "COMPANY"));
        when(stripePaymentService.createPaymentForMissionComplete(
                eq("1"), any(CreatePaymentRequest.class), any(Authentication.class)))
                .thenReturn(paymentResponse());

        mockMvc.perform(post("/api/payments/mission/1")
                        .header("Authorization", "Bearer company-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void createPaymentWithFreelancerJwtIsForbidden() throws Exception {
        when(jwtDecoder.decode("freelancer-token")).thenReturn(jwtWithRole("freelancer-token", "freelancer-1", "FREELANCER"));

        mockMvc.perform(post("/api/payments/mission/1")
                        .header("Authorization", "Bearer freelancer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void webhookWithoutStripeSignatureIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private Jwt jwtWithRole(String token, String role) {
        return jwtWithRole(token, "user-1", role);
    }

    private Jwt jwtWithRole(String token, String subject, String role) {
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", subject)
                .claim("realm_access", Map.of("roles", List.of(role)))
                .issuer("http://localhost:8080/realms/b2b-platform")
                .build();
    }

    private PaymentResponse paymentResponse() {
        PaymentResponse response = new PaymentResponse();
        response.setId("payment-1");
        response.setStatus("REQUIRES_PAYMENT");
        response.setAmountCents(1000L);
        response.setPlatformFeeCents(100L);
        response.setNetAmountCents(900L);
        response.setCurrency("usd");
        response.setStripePaymentIntentId("pi_test");
        response.setClientSecret("pi_test_secret");
        return response;
    }
}
