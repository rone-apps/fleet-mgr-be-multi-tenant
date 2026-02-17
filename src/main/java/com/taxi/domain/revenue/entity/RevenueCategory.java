package com.taxi.domain.revenue.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxi.domain.expense.model.ApplicationType;
import com.taxi.domain.expense.model.ApplicationTypeConverter;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.driver.model.Driver;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing revenue categories
 * Matches structure of ExpenseCategory for consistency
 */
@Entity
@Table(name = "revenue_category",
       uniqueConstraints = {
           @UniqueConstraint(name = "UK_revenue_category_code", columnNames = "category_code")
       },
       indexes = {
           @Index(name = "idx_revenue_category_type", columnList = "category_type"),
           @Index(name = "idx_revenue_category_active", columnList = "is_active"),
           @Index(name = "idx_revenue_category_applies_to", columnList = "applies_to")
       })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_code", nullable = false, unique = true, length = 50)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to", nullable = false, length = 20)
    private AppliesTo appliesTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false, length = 20)
    private CategoryType categoryType;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "application_type", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    @Convert(converter = ApplicationTypeConverter.class)
    private ApplicationType applicationType;

    @Column(name = "shift_profile_id")
    private Long shiftProfileId;  // Link to shift profile for SHIFT_PROFILE application type

    @Column(name = "specific_shift_id")
    private Long specificShiftId;  // Link to specific shift for SPECIFIC_SHIFT application type

    @Column(name = "specific_person_id")
    private Long specificPersonId;  // Link to specific person (driver or owner) for SPECIFIC_PERSON application type

    // Relationships for eager loading in DTOs (read-only via JoinColumn insertable=false, updatable=false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specific_shift_id", insertable = false, updatable = false)
    private CabShift specificShift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specific_person_id", insertable = false, updatable = false)
    private Driver specificPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specific_driver_id", insertable = false, updatable = false)
    private Driver specificDriver;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    // Enums matching expense_category
    public enum AppliesTo {
        CAB,
        COMPANY,
        DRIVER,
        OWNER,
        SHIFT
    }

    public enum CategoryType {
        FIXED,
        VARIABLE
    }

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
            case ALL_OWNERS:
            case ALL_DRIVERS:
                // No additional validation needed
                break;
        }
    }

    /**
     * Check if this category is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Check if this category applies to a specific entity type
     */
    public boolean appliesTo(AppliesTo entityType) {
        return this.appliesTo == entityType;
    }

    /**
     * Check if this is a fixed revenue category
     */
    public boolean isFixed() {
        return CategoryType.FIXED == this.categoryType;
    }

    /**
     * Check if this is a variable revenue category
     */
    public boolean isVariable() {
        return CategoryType.VARIABLE == this.categoryType;
    }

    /**
     * Activate this category
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Deactivate this category
     */
    public void deactivate() {
        this.isActive = false;
    }

    @Override
    public String toString() {
        return "RevenueCategory{" +
                "id=" + id +
                ", categoryCode='" + categoryCode + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", appliesTo=" + appliesTo +
                ", categoryType=" + categoryType +
                ", isActive=" + isActive +
                '}';
    }
}