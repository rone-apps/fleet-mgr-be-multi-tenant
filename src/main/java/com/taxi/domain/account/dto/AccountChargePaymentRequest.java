package com.taxi.domain.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountChargePaymentRequest {
    private Long chargeId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private Long paymentMethodId;
    private String referenceNumber;
    private String notes;
}
