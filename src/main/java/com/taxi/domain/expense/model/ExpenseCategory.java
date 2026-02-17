package com.taxi.domain.expense.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.shift.model.CabShift;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ExpenseCategory - Configurable expense types
 * Allows admins to create custom expense categories beyond predefined types
 * 
 * Examples:
 * - Dispatch Fee
 * - Internet Charge
 * - Cleaning Service
 * - Safety Equipment
 */
@Entity
@Table(name = "expense_category",
       uniqueConstraints = @UniqueConstraint(columnNames = {"category_code"}),
       indexes = {
           @Index(name = "idx_category_type", columnList = "category_type"),
           @Index(name = "idx_category_active", columnList = "is_active")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_code", nullable = false, unique = true, length = 50)
    private String categoryCode;  // e.g., "DISPATCH_FEE", "INTERNET_CHARGE"

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;  // e.g., "Dispatch Fee", "Internet Charge"

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false, length = 20)
    private CategoryType categoryType;  // FIXED or VARIABLE

    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to", nullable = false, length = 20)
    private AppliesTo appliesTo;  // CAB, SHIFT, OWNER, DRIVER, COMPANY

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "application_type", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    @Convert(converter = ApplicationTypeConverter.class)
    private ApplicationType applicationType;

    @Column(name = "shift_profile_id")
    private Long shiftProfileId;  // Link to shift profile for SHIFT_PROFILE application type

    @Column(name = "specific_shift_id")
    private Long specificShiftId;  // Link to specific shift for SPECIFIC_SHIFT application type

    @Column(name = "specific_person_id")
    private Long specificPersonId;  // Link to specific person (driver or owner) for SPECIFIC_PERSON application type

    @Column(name = "attribute_type_id")
    private Long attributeTypeId;  // Link to attribute type for SHIFTS_WITH_ATTRIBUTE application type

    // Relationships for eager loading in DTOs (read-only via JoinColumn insertable=false, updatable=false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specific_shift_id", insertable = false, updatable = false)
    @JsonIgnore
    private CabShift specificShift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specific_owner_id", insertable = false, updatable = false)
    @JsonIgnore
    private Driver specificOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specific_driver_id", insertable = false, updatable = false)
    @JsonIgnore
    private Driver specificDriver;

    // Legacy support - kept for backward compatibility but will be deprecated
    @OneToOne(mappedBy = "expenseCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private ExpenseCategoryRule categoryRule;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        validateApplicationType();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateApplicationType();
    }

    /**
     * Validate that application type and related fields are consistent
     * This ensures data integrity across all application type scenarios
     */
    private void validateApplicationType() {
        if (applicationType == null) {
            throw new IllegalStateException("Application type is required");
        }

        switch (applicationType) {
            case SHIFT_PROFILE:
                if (shiftProfileId == null) {
                    throw new IllegalStateException("Shift profile ID required for SHIFT_PROFILE application type");
                }
                break;
            case SPECIFIC_SHIFT:
                if (specificShiftId == null) {
                    throw new IllegalStateException("Specific shift ID required for SPECIFIC_SHIFT application type");
                }
                break;
            case SPECIFIC_PERSON:
                if (specificPersonId == null) {
                    throw new IllegalStateException("Person ID (driver or owner) required for SPECIFIC_PERSON application type");
                }
                break;
            case SHIFTS_WITH_ATTRIBUTE:
                if (attributeTypeId == null) {
                    throw new IllegalStateException("Attribute type ID required for SHIFTS_WITH_ATTRIBUTE application type");
                }
                break;
            case ALL_OWNERS:
            case ALL_DRIVERS:
                // No additional validation needed
                break;
        }
    }

    /**
     * Category Type - Fixed or Variable
     */
    public enum CategoryType {
        FIXED("Fixed Expense"),      // Recurring expenses (monthly/daily)
        VARIABLE("Variable Expense"); // One-time or irregular expenses

        private final String displayName;

        CategoryType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * What entity this expense applies to
     */
    public enum AppliesTo {
        CAB("Per Cab"),
        SHIFT("Per Shift"),
        OWNER("Per Owner"),
        DRIVER("Per Driver"),
        COMPANY("Company-wide");

        private final String displayName;

        AppliesTo(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Business logic: Activate category
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Business logic: Deactivate category
     */
    public void deactivate() {
        this.isActive = false;
    }
}
