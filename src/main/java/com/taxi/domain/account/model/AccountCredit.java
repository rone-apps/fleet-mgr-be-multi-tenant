package com.taxi.domain.account.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_credit", indexes = {
        @Index(name = "idx_credit_customer", columnList = "customer_id"),
        @Index(name = "idx_credit_account", columnList = "account_id"),
        @Index(name = "idx_credit_date", columnList = "created_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, length = 50)
    private String accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private AccountCustomer customer;

    @Column(name = "credit_amount", nullable = false)
    private BigDecimal creditAmount;

    @Column(name = "used_amount")
    private BigDecimal usedAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", nullable = false)
    private BigDecimal remainingAmount;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType; // OVERPAYMENT, REFUND, ADJUSTMENT

    @Column(name = "source_reference", length = 100)
    private String sourceReference; // Invoice number, Payment number, etc.

    @Column(name = "description")
    private String description;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Helper methods
    public void useCredit(BigDecimal amount) {
        if (amount.compareTo(this.remainingAmount) > 0) {
            throw new IllegalArgumentException("Cannot use more credit than remaining amount");
        }
        this.usedAmount = this.usedAmount.add(amount);
        this.remainingAmount = this.remainingAmount.subtract(amount);

        if (this.remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.isActive = false;
        }
    }

    public boolean hasRemainingCredit() {
        return this.isActive && this.remainingAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}
