package com.taxi.web.dto.tax;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxCalculationResultDTO {

    // Basic info
    private Integer taxYear;
    private String province;
    private String driverName;
    private Long driverId;

    // Income breakdown
    @Builder.Default
    private BigDecimal totalEmploymentIncome = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalOtherIncome = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalIncome = BigDecimal.ZERO;

    // Deductions
    @Builder.Default
    private BigDecimal rrspDeduction = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal donationDeduction = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal otherDeductions = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    // Net/Taxable income
    @Builder.Default
    private BigDecimal netIncome = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal taxableIncome = BigDecimal.ZERO;

    // Federal tax calculation
    @Builder.Default
    private BigDecimal grossFederalTax = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal federalCredits = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal netFederalTax = BigDecimal.ZERO;

    // Provincial tax calculation
    @Builder.Default
    private BigDecimal grossProvincialTax = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal provincialCredits = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal netProvincialTax = BigDecimal.ZERO;

    // CPP and EI contributions
    @Builder.Default
    private BigDecimal cppContributions = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal eiPremiums = BigDecimal.ZERO;

    // Total payable
    @Builder.Default
    private BigDecimal totalTaxPayable = BigDecimal.ZERO;

    // Line items for detailed display
    @Builder.Default
    private List<String> notes = new ArrayList<>();

    // Errors if any
    private String error;
}
