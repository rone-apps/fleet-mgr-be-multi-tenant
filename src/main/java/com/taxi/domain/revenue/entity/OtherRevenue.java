package com.taxi.domain.revenue.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.expense.model.ApplicationType;
import com.taxi.domain.shift.model.CabShift;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "other_revenue",
    indexes = {
        @Index(name = "idx_revenue_category", columnList = "revenue_category_id"),
        @Index(name = "idx_revenue_entity", columnList = "entity_type,entity_id"),
        @Index(name = "idx_revenue_date", columnList = "revenue_date"),
        @Index(name = "idx_revenue_type", columnList = "revenue_type"),
        @Index(name = "idx_revenue_payment_status", columnList = "payment_status"),
        @Index(name = "idx_other_revenue_app_type", columnList = "application_type"),
        @Index(name = "idx_other_revenue_shift_profile", columnList = "shift_profile_id"),
        @Index(name = "idx_other_revenue_specific_shift", columnList = "specific_shift_id"),
        @Index(name = "idx_other_revenue_specific_person", columnList = "specific_person_id"),
        @Index(name = "idx_other_revenue_attribute_type", columnList = "attribute_type_id"),
        @Index(name = "idx_other_revenue_app_type_date", columnList = "application_type,revenue_date")
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherRevenue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "revenue_date", nullable = false)
    private LocalDate revenueDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;
    
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "revenue_type", nullable = false)
    private RevenueType revenueType;
    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @Column(name = "payment_method", length = 100)
    private String paymentMethod;
    
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    
    @Column(length = 1000)
    private String notes;
    
    // Foreign Keys
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revenue_category_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private RevenueCategory category;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cab_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cab cab;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Driver driver;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Driver owner;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CabShift shift;

    // ✅ NEW: Application Type System (matching OneTimeExpense pattern)
    // Allows revenues to be applied using the same targeting criteria as expenses
    @Enumerated(EnumType.STRING)
    @Column(name = "application_type", length = 30)
    private ApplicationType applicationType;

    // Specific relationship IDs based on application type
    @Column(name = "shift_profile_id")
    private Long shiftProfileId;

    @Column(name = "specific_shift_id")
    private Long specificShiftId;

    @Column(name = "specific_person_id")
    private Long specificPersonId;

    @Column(name = "attribute_type_id")
    private Long attributeTypeId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Validate application type on persist/update
     * Ensures required relationship IDs are set for the chosen application type
     */
    @PrePersist
    @PreUpdate
    private void validateApplicationType() {
        if (applicationType == null) {
            return; // Optional - backward compatibility
        }

        switch (applicationType) {
            case SHIFT_PROFILE:
                if (shiftProfileId == null) {
                    throw new IllegalStateException("shiftProfileId is required for SHIFT_PROFILE application type");
                }
                break;
            case SPECIFIC_SHIFT:
                if (specificShiftId == null) {
                    throw new IllegalStateException("specificShiftId is required for SPECIFIC_SHIFT application type");
                }
                break;
            case SPECIFIC_PERSON:
                if (specificPersonId == null) {
                    throw new IllegalStateException("specificPersonId is required for SPECIFIC_PERSON application type");
                }
                break;
            case SHIFTS_WITH_ATTRIBUTE:
                if (attributeTypeId == null) {
                    throw new IllegalStateException("attributeTypeId is required for SHIFTS_WITH_ATTRIBUTE application type");
                }
                break;
            case ALL_OWNERS:
            case ALL_DRIVERS:
            case ALL_ACTIVE_SHIFTS:
                // No additional validation needed - these apply globally
                break;
        }
    }

    // Enums
    public enum EntityType {
        CAB, DRIVER, OWNER, SHIFT, COMPANY
    }
    
    public enum RevenueType {
        BONUS,
        CREDIT,
        ADJUSTMENT,
        REFERRAL,
        INCENTIVE,
        COMMISSION,
        REFUND,
        REIMBURSEMENT,
        OTHER
    }
    
    public enum PaymentStatus {
        PENDING,
        PAID,
        CANCELLED,
        PROCESSING
    }
}