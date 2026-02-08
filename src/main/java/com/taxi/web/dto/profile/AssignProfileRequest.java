package com.taxi.web.dto.profile;

import lombok.*;

import java.time.LocalDate;

/**
 * AssignProfileRequest - Request DTO for assigning a profile to a shift
 * Includes date-based tracking for audit trail
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AssignProfileRequest {

    private Long shiftId;
    private Long profileId;
    private LocalDate startDate;
    private String reason;
}
