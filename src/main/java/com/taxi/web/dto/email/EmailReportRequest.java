package com.taxi.web.dto.email;

import com.taxi.web.dto.expense.OwnerReportDTO;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending reports via email with PDF attachment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailReportRequest {

    @NotNull(message = "Email address is required")
    @Email(message = "Email address must be valid")
    private String toEmail;

    @NotNull(message = "Driver/Owner name is required")
    private String driverName;

    @NotNull(message = "Report data is required")
    private OwnerReportDTO report;
}
