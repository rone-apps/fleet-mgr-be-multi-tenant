package com.taxi.web.dto.cab.attribute;

import com.taxi.domain.cab.model.AttributeCost;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for AttributeCost responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeCostDTO {

    private Long id;
    private Long attributeTypeId;
    private String attributeCode;
    private String attributeName;

    private BigDecimal price;
    private String billingUnit;  // MONTHLY or DAILY

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    private boolean isCurrentlyActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    /**
     * Convert entity to DTO
     */
    public static AttributeCostDTO fromEntity(AttributeCost entity) {
        if (entity == null) return null;

        return AttributeCostDTO.builder()
                .id(entity.getId())
                .attributeTypeId(entity.getAttributeType().getId())
                .attributeCode(entity.getAttributeType().getAttributeCode())
                .attributeName(entity.getAttributeType().getAttributeName())
                .price(entity.getPrice())
                .billingUnit(entity.getBillingUnit().name())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .isCurrentlyActive(entity.isCurrentlyActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
