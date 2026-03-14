package com.taxi.domain.tax.model;

import com.taxi.domain.revenue.entity.RevenueCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_category_assignment",
       indexes = {
           @Index(name = "idx_cca_commission_type", columnList = "commission_type_id"),
           @Index(name = "idx_cca_revenue_cat", columnList = "revenue_category_id"),
           @Index(name = "idx_cca_active", columnList = "is_active")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class CommissionCategoryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_type_id", nullable = false)
    private CommissionType commissionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revenue_category_id", nullable = false)
    private RevenueCategory revenueCategory;

    @Column(name = "assigned_at", nullable = false)
    private LocalDate assignedAt;

    @Column(name = "unassigned_at")
    private LocalDate unassignedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "notes", length = 500)
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
}
