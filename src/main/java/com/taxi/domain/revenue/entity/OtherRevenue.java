package com.taxi.domain.revenue.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.shift.model.CabShift;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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
        @Index(name = "idx_revenue_payment_status", columnList = "payment_status")
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
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