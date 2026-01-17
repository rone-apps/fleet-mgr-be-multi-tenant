package com.taxi.domain.financial;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant2cab", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_cab_merchant_active", 
                           columnNames = {"cab_number", "merchant_number", "start_date"})
       },
       indexes = {
           @Index(name = "idx_cab_number", columnList = "cab_number"),
           @Index(name = "idx_merchant_number", columnList = "merchant_number"),
           @Index(name = "idx_active_mappings", columnList = "cab_number, end_date")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Merchant2Cab {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Cab number is required")
    @Column(name = "cab_number", nullable = false, length = 20)
    private String cabNumber;
    
    @NotBlank(message = "Merchant number is required")
    @Column(name = "merchant_number", nullable = false, length = 100)
    private String merchantNumber;
    
    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(length = 500)
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    @Transient
    public boolean isActive() {
        return endDate == null || endDate.isAfter(LocalDate.now());
    }
}
