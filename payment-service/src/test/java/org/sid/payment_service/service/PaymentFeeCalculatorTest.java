package org.sid.payment_service.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFeeCalculatorTest {

    private final PaymentFeeCalculator calculator = new PaymentFeeCalculator();

    @Test
    void calculatesFeeWithPercent() {
        long fee = calculator.calculateFee(10000L, BigDecimal.valueOf(10));
        assertThat(fee).isEqualTo(1000L);
    }

    @Test
    void returnsZeroWhenPercentMissing() {
        long fee = calculator.calculateFee(5000L, null);
        assertThat(fee).isZero();
    }
}

