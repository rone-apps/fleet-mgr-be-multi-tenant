package com.taxi.web.dto.tax;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxTypeDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean isActive;
    private String currentRate;  // display-friendly current rate
    private LocalDateTime createdAt;
}
