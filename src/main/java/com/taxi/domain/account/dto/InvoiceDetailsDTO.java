package com.taxi.domain.account.dto;

import com.taxi.domain.account.model.Invoice;
import com.taxi.domain.account.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDetailsDTO {
    private Long id;
    private String invoiceNumber;
    private String accountId;

    private Long customerId;
    private String customerName;

    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;

    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal balanceDue;

    private Invoice.InvoiceStatus status;
    private String notes;
    private String terms;

    private List<LineItemDTO> lineItems;
    private List<PaymentDTO> payments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItemDTO {
        private Long id;
        private Long chargeId;
        private String description;
        private LocalDate tripDate;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDTO {
        private Long id;
        private String paymentNumber;
        private LocalDate paymentDate;
        private BigDecimal amount;
        private String paymentMethodCode;
        private String referenceNumber;
        private String notes;
    }
}
