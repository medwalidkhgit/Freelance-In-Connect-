package org.sid.payment_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class PaymentFeeCalculator {
    public long calculateFee(long amountCents, BigDecimal feePercent) {
        if (feePercent == null) {
            return 0L;
        }
        BigDecimal fee = BigDecimal.valueOf(amountCents)
                .multiply(feePercent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return fee.longValue();
    }
}

