package com.taxi.web.dto.tax;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverTaxProfileDTO {
    private Long id;
    private Long driverId;
    private Integer taxYear;
    private String province;
    private String language;
    private String maritalStatus;
    private Integer numDependents;
    private Integer birthYear;
    private Boolean hasDisability;
    private Boolean spouseDisability;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
