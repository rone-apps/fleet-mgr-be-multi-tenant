package com.taxi.domain.expense.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
