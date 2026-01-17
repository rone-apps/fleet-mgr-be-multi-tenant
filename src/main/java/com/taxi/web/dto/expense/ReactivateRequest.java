package com.taxi.web.dto.expense;

import lombok.Data;
import java.time.LocalDate;

/**
 * Request DTO for reactivating a recurring expense
 */
@Data
public class ReactivateRequest {
    private LocalDate effectiveDate;
}
