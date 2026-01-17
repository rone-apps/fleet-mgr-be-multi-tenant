package com.taxi.web.dto.expense;

import lombok.Data;
import java.time.LocalDate;

/**
 * Request DTO for deactivating a recurring expense
 */
@Data
public class DeactivateRequest {
    private LocalDate endDate;
}
