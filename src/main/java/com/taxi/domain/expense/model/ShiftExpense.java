package com.taxi.domain.expense.model;

import com.taxi.domain.shift.model.DriverSegment;
import com.taxi.domain.shift.model.ShiftLog;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing expenses incurred during a shift
 * Tracks who paid and who is responsible for the expense
 * 
 * Business Rule: Expenses are deducted from gross revenue before calculating net
 */
@Entity
@Table(name = "shift_expense", 
       indexes = {
           @Index(name = "idx_expense_log", columnList = "shift_log_id, timestamp"),
           @Index(name = "idx_expense_type", columnList = "expense_type, timestamp"),
           @Index(name = "idx_expense_paid_by", columnList = "paid_by, timestamp")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"shiftLog", "segment"})
@EqualsAndHashCode(of = "id")
public class ShiftExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_log_id", nullable = false)
    private ShiftLog shiftLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    private DriverSegment segment;  // Nullable - which driver segment incurred this (if attributable)

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_type", nullable = false, length = 30)
    private ExpenseType expenseType;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "paid_by", nullable = false, length = 20)
    private PaidBy paidBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "responsible_party", length = 20)
    private ResponsibleParty responsibleParty;  // Who should ultimately bear the cost

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;  // Link to receipt image/PDF

    @Column(name = "vendor", length = 100)
    private String vendor;  // Gas station, mechanic, etc.

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
     * Expense types
     */
    public enum ExpenseType {
        FUEL("Fuel/Gas"),
        MAINTENANCE("Maintenance/Repair"),
        TOLL("Toll"),
        PARKING("Parking"),
        CLEANING("Car Cleaning"),
        DISPATCH_FEE("Dispatch Fee"),           // Operating company fee
        INTERNET_CHARGE("Internet/Connectivity"), // Operating company fee
        INSURANCE("Insurance"),
        ACCIDENT_DAMAGE("Accident/Damage"),
        TICKET_FINE("Ticket/Fine"),
        CAR_WASH("Car Wash"),
        TIRE_REPLACEMENT("Tire Replacement"),
        OIL_CHANGE("Oil Change"),
        INSPECTION("Vehicle Inspection"),
        REGISTRATION("Registration Fee"),
        LICENSE_RENEWAL("License Renewal"),
        OTHER("Other Expense");

        private final String displayName;

        ExpenseType(String displayName) {
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
        COMPANY("Company");

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
        DRIVER("Driver"),           // Driver's responsibility (fuel during their shift)
        OWNER("Owner"),             // Owner's responsibility (maintenance, insurance)
        COMPANY("Company"),         // Company's responsibility (dispatch, internet)
        SHARED("Shared");           // Split between parties

        private final String displayName;

        ResponsibleParty(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Business logic: Check if this is a company operating expense
     */
    public boolean isCompanyExpense() {
        return expenseType == ExpenseType.DISPATCH_FEE 
                || expenseType == ExpenseType.INTERNET_CHARGE;
    }

    /**
     * Business logic: Check if this is a maintenance expense
     */
    public boolean isMaintenanceExpense() {
        return expenseType == ExpenseType.MAINTENANCE
                || expenseType == ExpenseType.TIRE_REPLACEMENT
                || expenseType == ExpenseType.OIL_CHANGE
                || expenseType == ExpenseType.INSPECTION;
    }

    /**
     * Business logic: Check if receipt is attached
     */
    public boolean hasReceipt() {
        return receiptUrl != null && !receiptUrl.trim().isEmpty();
    }

    /**
     * Business logic: Determine default responsible party based on expense type
     */
    public static ResponsibleParty getDefaultResponsibleParty(ExpenseType expenseType) {
        return switch (expenseType) {
            case FUEL, TOLL, PARKING, CLEANING, CAR_WASH -> ResponsibleParty.DRIVER;
            case DISPATCH_FEE, INTERNET_CHARGE -> ResponsibleParty.COMPANY;
            case MAINTENANCE, INSURANCE, ACCIDENT_DAMAGE, TIRE_REPLACEMENT, 
                 OIL_CHANGE, INSPECTION, REGISTRATION, LICENSE_RENEWAL -> ResponsibleParty.OWNER;
            case TICKET_FINE -> ResponsibleParty.DRIVER;  // Usually driver's fault
            default -> ResponsibleParty.SHARED;
        };
    }
}
