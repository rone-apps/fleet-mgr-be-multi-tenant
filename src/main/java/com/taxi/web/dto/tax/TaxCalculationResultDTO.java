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
    private BigDecimal totalEmploymentIncome = BigDecimal.ZERO;
    private BigDecimal totalOtherIncome = BigDecimal.ZERO;
    private BigDecimal totalIncome = BigDecimal.ZERO;

    // Deductions
    private BigDecimal rrspDeduction = BigDecimal.ZERO;
    private BigDecimal donationDeduction = BigDecimal.ZERO;
    private BigDecimal otherDeductions = BigDecimal.ZERO;
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    // Net/Taxable income
    private BigDecimal netIncome = BigDecimal.ZERO;
    private BigDecimal taxableIncome = BigDecimal.ZERO;

    // Federal tax calculation
    private BigDecimal grossFederalTax = BigDecimal.ZERO;
    private BigDecimal federalCredits = BigDecimal.ZERO;
    private BigDecimal netFederalTax = BigDecimal.ZERO;

    // Provincial tax calculation
    private BigDecimal grossProvincialTax = BigDecimal.ZERO;
    private BigDecimal provincialCredits = BigDecimal.ZERO;
    private BigDecimal netProvincialTax = BigDecimal.ZERO;

    // CPP and EI contributions
    private BigDecimal cppContributions = BigDecimal.ZERO;
    private BigDecimal eiPremiums = BigDecimal.ZERO;

    // Total payable
    private BigDecimal totalTaxPayable = BigDecimal.ZERO;

    // Line items for detailed display
    @Builder.Default
    private List<String> notes = new ArrayList<>();

    // Errors if any
    private String error;
}
