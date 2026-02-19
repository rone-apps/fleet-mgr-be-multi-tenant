package com.taxi.domain.account.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoice")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "account_id", nullable = false, length = 50)
    private String accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AccountCustomer customer;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;

    // Amounts
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "balance_due", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal balanceDue = BigDecimal.ZERO;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    // Notes
    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "terms", length = 500)
    private String terms;

    // Audit
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Email tracking
    @Column(name = "last_email_sent_at")
    private LocalDateTime lastEmailSentAt;

    @Column(name = "last_email_sent_to", length = 255)
    private String lastEmailSentTo;

    @Column(name = "email_send_count")
    @Builder.Default
    private Integer emailSendCount = 0;

    // Relationships
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.PERSIST)
    @Builder.Default
    @JsonManagedReference
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonManagedReference
    private List<Payment> payments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business methods
    public void addLineItem(InvoiceLineItem lineItem) {
        lineItems.add(lineItem);
        lineItem.setInvoice(this);
    }

    public void removeLineItem(InvoiceLineItem lineItem) {
        lineItems.remove(lineItem);
        lineItem.setInvoice(null);
    }

    public void calculateTotals() {
        // Calculate subtotal from line items
        this.subtotal = lineItems.stream()
                .map(InvoiceLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate tax
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = subtotal.multiply(taxRate).divide(new BigDecimal("100"));
        } else {
            this.taxAmount = BigDecimal.ZERO;
        }

        // Calculate total
        this.totalAmount = subtotal.add(taxAmount);

        // Calculate balance due
        this.balanceDue = totalAmount.subtract(amountPaid);
    }

    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setInvoice(this);
        
        // Recalculate amount paid
        this.amountPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Recalculate balance
        this.balanceDue = totalAmount.subtract(amountPaid);
        
        // Update status
        updateStatus();
    }

    public void updateStatus() {
        if (balanceDue.compareTo(BigDecimal.ZERO) == 0) {
            this.status = InvoiceStatus.PAID;
            this.paidAt = LocalDateTime.now();
        } else if (balanceDue.compareTo(totalAmount) < 0) {
            this.status = InvoiceStatus.PARTIAL;
        } else if (LocalDate.now().isAfter(dueDate)) {
            this.status = InvoiceStatus.OVERDUE;
        } else if (status == InvoiceStatus.DRAFT) {
            this.status = InvoiceStatus.SENT;
        }
    }

    public void markAsSent() {
        this.status = InvoiceStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void recordEmailSent(String emailAddress) {
        this.lastEmailSentAt = LocalDateTime.now();
        this.lastEmailSentTo = emailAddress;
        this.emailSendCount = (emailSendCount != null ? emailSendCount : 0) + 1;
        // Mark as sent if not already
        if (this.status == InvoiceStatus.DRAFT) {
            this.status = InvoiceStatus.SENT;
        }
    }

    public void markAsCancelled() {
        this.status = InvoiceStatus.CANCELLED;
    }

    public boolean isOverdue() {
        return status != InvoiceStatus.PAID && 
               status != InvoiceStatus.CANCELLED && 
               LocalDate.now().isAfter(dueDate);
    }

    public boolean isPaid() {
        return status == InvoiceStatus.PAID;
    }

    public boolean isPartiallyPaid() {
        return status == InvoiceStatus.PARTIAL;
    }

    public BigDecimal getPaymentPercentage() {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amountPaid.multiply(new BigDecimal("100"))
                .divide(totalAmount, 2, BigDecimal.ROUND_HALF_UP);
    }

    public enum InvoiceStatus {
        DRAFT,      // Invoice created but not sent
        SENT,       // Invoice sent to customer
        PAID,       // Fully paid
        PARTIAL,    // Partially paid
        OVERDUE,    // Past due date and not paid
        CANCELLED   // Invoice cancelled
    }
}
