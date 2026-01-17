package com.taxi.domain.revenue.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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