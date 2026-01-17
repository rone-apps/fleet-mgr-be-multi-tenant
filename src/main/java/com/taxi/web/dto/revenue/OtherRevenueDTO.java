package com.taxi.web.dto.revenue;

import com.taxi.domain.revenue.entity.OtherRevenue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for OtherRevenue responses - avoids lazy loading issues
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherRevenueDTO {
    
    private Long id;
    private BigDecimal amount;
    private LocalDate revenueDate;
    private String entityType;
    private Long entityId;
    private String revenueType;
    private String description;
    private String referenceNumber;
    private String paymentStatus;
    private String paymentMethod;
    private LocalDate paymentDate;
    private String notes;
    
    // Category info
    private Long categoryId;
    private String categoryName;
    
    // Entity display names (resolved from the entity references)
    private String entityDisplayName;
    
    // Related entity info (only IDs and display names to avoid lazy loading)
    private Long cabId;
    private String cabNumber;
    
    private Long driverId;
    private String driverNumber;
    private String driverName;
    
    private Long ownerId;
    private String ownerNumber;
    private String ownerName;
    
    private Long shiftId;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Convert OtherRevenue entity to DTO
     */
    public static OtherRevenueDTO fromEntity(OtherRevenue revenue) {
        if (revenue == null) {
            return null;
        }
        
        OtherRevenueDTOBuilder builder = OtherRevenueDTO.builder()
                .id(revenue.getId())
                .amount(revenue.getAmount())
                .revenueDate(revenue.getRevenueDate())
                .entityType(revenue.getEntityType() != null ? revenue.getEntityType().name() : null)
                .entityId(revenue.getEntityId())
                .revenueType(revenue.getRevenueType() != null ? revenue.getRevenueType().name() : null)
                .description(revenue.getDescription())
                .referenceNumber(revenue.getReferenceNumber())
                .paymentStatus(revenue.getPaymentStatus() != null ? revenue.getPaymentStatus().name() : null)
                .paymentMethod(revenue.getPaymentMethod())
                .paymentDate(revenue.getPaymentDate())
                .notes(revenue.getNotes())
                .createdAt(revenue.getCreatedAt())
                .updatedAt(revenue.getUpdatedAt());
        
        // Category info - safely access lazy loaded entity
        if (revenue.getCategory() != null) {
            builder.categoryId(revenue.getCategory().getId())
                   .categoryName(revenue.getCategory().getCategoryName());
        }
        
        // Cab info
        if (revenue.getCab() != null) {
            builder.cabId(revenue.getCab().getId())
                   .cabNumber(revenue.getCab().getCabNumber());
        }
        
        // Driver info
        if (revenue.getDriver() != null) {
            builder.driverId(revenue.getDriver().getId())
                   .driverNumber(revenue.getDriver().getDriverNumber())
                   .driverName(revenue.getDriver().getFirstName() + " " + revenue.getDriver().getLastName());
        }
        
        // Owner info
        if (revenue.getOwner() != null) {
            builder.ownerId(revenue.getOwner().getId())
                   .ownerNumber(revenue.getOwner().getDriverNumber())
                   .ownerName(revenue.getOwner().getFirstName() + " " + revenue.getOwner().getLastName());
        }
        
        // Shift info
        if (revenue.getShift() != null) {
            builder.shiftId(revenue.getShift().getId());
        }
        
        // Set entity display name based on entity type
        OtherRevenueDTO dto = builder.build();
        dto.setEntityDisplayName(resolveEntityDisplayName(dto));
        
        return dto;
    }
    
    /**
     * Resolve a human-readable display name for the entity
     */
    private static String resolveEntityDisplayName(OtherRevenueDTO dto) {
        if (dto.getEntityType() == null) {
            return "Unknown";
        }
        
        switch (dto.getEntityType()) {
            case "CAB":
                return dto.getCabNumber() != null ? "Cab " + dto.getCabNumber() : "Cab #" + dto.getEntityId();
            case "DRIVER":
                return dto.getDriverName() != null ? dto.getDriverName() : "Driver #" + dto.getEntityId();
            case "OWNER":
                return dto.getOwnerName() != null ? dto.getOwnerName() : "Owner #" + dto.getEntityId();
            case "SHIFT":
                return "Shift #" + dto.getEntityId();
            case "COMPANY":
                return "Company";
            default:
                return dto.getEntityType() + " #" + dto.getEntityId();
        }
    }
}