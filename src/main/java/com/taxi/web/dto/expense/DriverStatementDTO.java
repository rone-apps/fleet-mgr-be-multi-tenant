package com.taxi.web.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverStatementDTO {

    private Long driverId;
    private String driverName;
    private String driverNumber;

    private LocalDate periodFrom;
    private LocalDate periodTo;

    @Builder.Default
    private List<StatementLineItem> recurringCharges = new ArrayList<>();

    @Builder.Default
    private List<StatementLineItem> oneTimeCharges = new ArrayList<>();

    private BigDecimal recurringTotal;
    private BigDecimal oneTimeTotal;
    private BigDecimal grandTotal;

    public void calculateTotals() {
        recurringTotal = recurringCharges.stream()
                .map(StatementLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        oneTimeTotal = oneTimeCharges.stream()
                .map(StatementLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        grandTotal = recurringTotal.add(oneTimeTotal);
    }
}
