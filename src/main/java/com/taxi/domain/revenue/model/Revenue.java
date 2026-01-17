package com.taxi.domain.revenue.model;

import com.taxi.domain.shift.model.DriverSegment;
import com.taxi.domain.shift.model.ShiftLog;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing revenue/income for a shift
 * Tracks all types of income: trip fares, credit cards, charge accounts, etc.
 * 
 * Business Rule: All revenue belongs to the shift owner (whether they drive or not)
 */
@Entity
@Table(name = "revenue", 
       indexes = {
           @Index(name = "idx_revenue_log", columnList = "shift_log_id, timestamp"),
           @Index(name = "idx_revenue_type", columnList = "revenue_type, timestamp"),
           @Index(name = "idx_revenue_payment", columnList = "payment_method, reference_number")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"shiftLog", "segment"})
@EqualsAndHashCode(of = "id")
public class Revenue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_log_id", nullable = false)
    private ShiftLog shiftLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    private DriverSegment segment;  // Nullable - which driver segment earned this (if attributable)

    @Enumerated(EnumType.STRING)
    @Column(name = "revenue_type", nullable = false, length = 30)
    private RevenueType revenueType;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;  // For traceability (card transaction ID, account number, etc.)

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "customer_name", length = 100)
    private String customerName;  // For charge accounts

    @Column(name = "customer_account", length = 50)
    private String customerAccount;  // For charge accounts

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Revenue types
     */
    public enum RevenueType {
        TRIP_FARE("Trip Fare"),                    // Meter fare
        CREDIT_CARD("Credit Card Payment"),        // Card payment
        CHARGE_ACCOUNT("Charge Account"),          // Corporate/customer account
        AIRPORT_PICKUP("Airport Pickup Fee"),      // Special airport fee
        TIP("Tip"),                                // Driver tip
        TOLL_REIMBURSEMENT("Toll Reimbursement"), // Toll paid by customer
        WAITING_CHARGE("Waiting Charge"),          // Waiting time charge
        EXTRA_PASSENGER("Extra Passenger Fee"),    // Additional passenger fee
        LUGGAGE_FEE("Luggage Fee"),               // Luggage handling fee
        OTHER("Other Revenue");                    // Miscellaneous

        private final String displayName;

        RevenueType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Payment methods
     */
    public enum PaymentMethod {
        CASH("Cash"),
        CREDIT_CARD("Credit Card"),
        DEBIT_CARD("Debit Card"),
        CHARGE_ACCOUNT("Charge Account"),
        DIGITAL_WALLET("Digital Wallet"),  // PayPal, Venmo, etc.
        MOBILE_APP("Mobile App Payment"),  // Uber, Lyft-like apps
        CHECK("Check"),
        OTHER("Other");

        private final String displayName;

        PaymentMethod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Business logic: Check if this is a cash transaction
     */
    public boolean isCash() {
        return paymentMethod == PaymentMethod.CASH;
    }

    /**
     * Business logic: Check if this requires settlement (card, account, etc.)
     */
    public boolean requiresSettlement() {
        return paymentMethod == PaymentMethod.CREDIT_CARD
                || paymentMethod == PaymentMethod.DEBIT_CARD
                || paymentMethod == PaymentMethod.CHARGE_ACCOUNT
                || paymentMethod == PaymentMethod.DIGITAL_WALLET
                || paymentMethod == PaymentMethod.MOBILE_APP;
    }

    /**
     * Business logic: Check if this is an account-based transaction
     */
    public boolean isAccountBased() {
        return paymentMethod == PaymentMethod.CHARGE_ACCOUNT;
    }
}
