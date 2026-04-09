package com.taxi.domain.payment.dto;

import com.taxi.domain.payment.model.SpareMachine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpareMachineDTO {
    private Long id;
    private String machineName;
    private Integer virtualCabId;
    private String merchantNumber;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SpareMachineDTO fromEntity(SpareMachine spare) {
        if (spare == null) {
            return null;
        }
        return SpareMachineDTO.builder()
                .id(spare.getId())
                .machineName(spare.getMachineName())
                .virtualCabId(spare.getVirtualCabId())
                .merchantNumber(spare.getMerchantNumber())
                .isActive(spare.getIsActive())
                .createdAt(spare.getCreatedAt())
                .updatedAt(spare.getUpdatedAt())
                .build();
    }
}
