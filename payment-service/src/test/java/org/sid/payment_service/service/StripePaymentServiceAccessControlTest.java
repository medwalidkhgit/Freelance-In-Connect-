package org.sid.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sid.payment_service.config.StripeProperties;
import org.sid.payment_service.dto.CreatePaymentRequest;
import org.sid.payment_service.dto.PaymentResponse;
import org.sid.payment_service.entity.Payment;
import org.sid.payment_service.entity.PaymentStatus;
import org.sid.payment_service.repository.PaymentRepository;
import org.sid.payment_service.repository.StripeAccountRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

class StripePaymentServiceAccessControlTest {

    private PaymentRepository paymentRepository;
    private StripeAccountRepository stripeAccountRepository;
    private StripePaymentService service;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        stripeAccountRepository = mock(StripeAccountRepository.class);
        StripeProperties stripeProperties = new StripeProperties();
        stripeProperties.setPlatformFeePercent(BigDecimal.TEN);
        service = new StripePaymentService(
                paymentRepository,
                stripeAccountRepository,
                stripeProperties,
                new PaymentFeeCalculator()
        );
    }

    @Test
    void companyCanReadOwnPaymentWithClientSecret() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment()));

        PaymentResponse response = service.getPayment(paymentId, auth("company-1", "ROLE_COMPANY"));

        assertThat(response.getClientSecret()).isEqualTo("pi_secret");
    }

    @Test
    void freelancerCanReadOwnPaymentWithoutClientSecret() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment()));

        PaymentResponse response = service.getPayment(paymentId, auth("freelancer-1", "ROLE_FREELANCER"));

        assertThat(response.getClientSecret()).isNull();
    }

    @Test
    void companyCannotReadAnotherCompanyPayment() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment()));

        assertThatThrownBy(() -> service.getPayment(paymentId, auth("company-2", "ROLE_COMPANY")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void internalCanReadPaymentWithoutClientSecret() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment()));

        PaymentResponse response = service.getPayment(paymentId, auth("internal-service", "ROLE_INTERNAL"));

        assertThat(response.getClientSecret()).isNull();
    }

    @Test
    void companyCannotCreatePaymentForAnotherCompany() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setCompanyId("company-1");
        request.setFreelancerId("freelancer-1");
        request.setAmountCents(1000L);

        assertThatThrownBy(() -> service.createPaymentForMissionComplete("mission-1", request, auth("company-2", "ROLE_COMPANY")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verifyNoInteractions(stripeAccountRepository);
    }

    private Payment payment() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setMissionId("mission-1");
        payment.setCompanyId("company-1");
        payment.setFreelancerId("freelancer-1");
        payment.setAmountCents(1000L);
        payment.setCurrency("usd");
        payment.setPlatformFeeCents(100L);
        payment.setNetAmountCents(900L);
        payment.setStatus(PaymentStatus.REQUIRES_PAYMENT);
        payment.setStripePaymentIntentId("pi_test");
        payment.setStripeClientSecret("pi_secret");
        return payment;
    }

    private Authentication auth(String name, String role) {
        return new UsernamePasswordAuthenticationToken(
                name,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
