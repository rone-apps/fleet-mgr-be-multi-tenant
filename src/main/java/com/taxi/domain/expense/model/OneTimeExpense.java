package com.taxi.domain.expense.model;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.shift.model.ShiftType;  // ✅ USE SHARED ENUM
import com.taxi.domain.expense.model.ApplicationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * OneTimeExpense - Variable expenses that occur irregularly
 * 
 * Can be associated with:
 * - Cab (e.g., major repair, tire replacement)
 * - Shift (e.g., special cleaning)
 * - Owner (e.g., personal expense)
 * - Driver (e.g., uniform purchase)
 * - Company (e.g., equipment purchase)
 */
@Entity
@Table(name = "one_time_expense",
       indexes = {
           @Index(name = "idx_onetime_category", columnList = "expense_category_id"),
           @Index(name = "idx_onetime_entity", columnList = "entity_type, entity_id"),
           @Index(name = "idx_onetime_date", columnList = "expense_date"),
           @Index(name = "idx_onetime_paid_by", columnList = "paid_by"),
           @Index(name = "idx_onetime_shift_type", columnList = "shift_type")  // ✅ NEW INDEX
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cab", "owner", "driver"})  // ✅ REMOVED cabShift
@EqualsAndHashCode(of = "id")
public class OneTimeExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "expense_category_id", nullable = true)
    private ExpenseCategory expenseCategory;

    // Polymorphic relationship (legacy - kept for backward compatibility)
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 20)
    private EntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    // New application type system - simplified criteria for one-time expenses
    @Enumerated(EnumType.STRING)
    @Column(name = "application_type", length = 30)
    private ApplicationType applicationType;

    @Column(name = "shift_profile_id")
    private Long shiftProfileId;

    @Column(name = "specific_shift_id")
    private Long specificShiftId;

    @Column(name = "specific_owner_id")
    private Long specificOwnerId;

    @Column(name = "specific_driver_id")
    private Long specificDriverId;

    // Optional relationships for convenience
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cab_id", insertable = false, updatable = false)
    private Cab cab;

    // ✅ CHANGED: From CabShift FK to ShiftType enum
    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", length = 10)
    private ShiftType shiftType;  // Only used when entityType = SHIFT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private Driver owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", insertable = false, updatable = false)
    private Driver driver;

    @Transient
    private String driverNumber;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "paid_by", nullable = false, length = 20)
    private PaidBy paidBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "responsible_party", length = 20)
    private ResponsibleParty responsibleParty;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "vendor", length = 200)
    private String vendor;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "is_reimbursable")
    @Builder.Default
    private boolean isReimbursable = false;

    @Column(name = "is_reimbursed")
    @Builder.Default
    private boolean isReimbursed = false;

    @Column(name = "reimbursed_date")
    private LocalDate reimbursedDate;

    @Column(name = "notes", length = 1000)
    private String notes;

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
     * Entity type for polymorphic relationship
     */
    public enum EntityType {
        CAB("Cab"),
        SHIFT("Shift"),
        OWNER("Owner"),
        DRIVER("Driver"),
        COMPANY("Company");

        private final String displayName;

        EntityType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Who physically paid for the expense
     */
    public enum PaidBy {
        DRIVER("Driver"),
        OWNER("Owner"),
        COMPANY("Company"),
        THIRD_PARTY("Third Party");

        private final String displayName;

        PaidBy(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Who is ultimately responsible for the expense
     */
    public enum ResponsibleParty {
        DRIVER("Driver"),
        OWNER("Owner"),
        COMPANY("Company"),
        SHARED("Shared");

        private final String displayName;

        ResponsibleParty(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Business logic: Mark as reimbursed
     */
    public void markAsReimbursed(LocalDate reimbursementDate) {
        if (!isReimbursable) {
            throw new IllegalStateException("This expense is not marked as reimbursable");
        }
        this.isReimbursed = true;
        this.reimbursedDate = reimbursementDate;
    }

    /**
     * Business logic: Check if receipt is attached
     */
    public boolean hasReceipt() {
        return receiptUrl != null && !receiptUrl.trim().isEmpty();
    }

    /**
     * Business logic: Check if reimbursement is pending
     */
    public boolean isReimbursementPending() {
        return isReimbursable && !isReimbursed;
    }

    /**
     * Business logic: Get days since expense
     */
    public long getDaysSinceExpense() {
        return java.time.temporal.ChronoUnit.DAYS.between(expenseDate, LocalDate.now());
    }
}