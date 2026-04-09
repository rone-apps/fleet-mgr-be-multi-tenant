package com.taxi.domain.tax.service;

import com.taxi.domain.tax.model.*;
import com.taxi.domain.tax.repository.*;
import com.taxi.web.dto.tax.TaxCalculationResultDTO;
import com.taxi.web.dto.tax.TaxCreditDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TaxCalculationService {

    private final DriverTaxEntryRepository driverTaxEntryRepository;
    private final DriverTaxProfileRepository driverTaxProfileRepository;
    private final IncomeTaxBracketRepository incomeTaxBracketRepository;
    private final TaxCreditRepository taxCreditRepository;
    private final CppEiRateRepository cppEiRateRepository;

    public TaxCalculationResultDTO calculateTax(Long driverId, Integer taxYear) {
        log.info("Calculating tax for driver {} for tax year {}", driverId, taxYear);

        TaxCalculationResultDTO result = TaxCalculationResultDTO.builder()
            .driverId(driverId)
            .taxYear(taxYear)
            .build();

        try {
            // Load driver tax profile
            DriverTaxProfile profile = driverTaxProfileRepository.findByDriverIdAndTaxYear(driverId, taxYear)
                .orElse(null);

            if (profile == null) {
                result.setError("Tax profile not found for driver " + driverId + " for year " + taxYear);
                return result;
            }

            result.setProvince(profile.getProvince());

            // Load tax entries
            List<DriverTaxEntry> entries = driverTaxEntryRepository.findByDriverIdAndTaxYear(driverId, taxYear);

            // Calculate income from T slips
            BigDecimal totalIncome = entries.stream()
                .filter(e -> e.getEntryType().equals("T_SLIP"))
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.setTotalEmploymentIncome(totalIncome);
            result.setTotalIncome(totalIncome);

            // Calculate deductions
            BigDecimal rrspDeduction = entries.stream()
                .filter(e -> e.getEntryType().equals("RRSP"))
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.setRrspDeduction(rrspDeduction);

            BigDecimal donationDeduction = calculateDonationCredit(entries);
            result.setDonationDeduction(donationDeduction);

            BigDecimal otherDeductions = entries.stream()
                .filter(e -> e.getEntryType().equals("OTHER_DEDUCTION"))
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.setOtherDeductions(otherDeductions);

            BigDecimal totalDeductions = rrspDeduction.add(donationDeduction).add(otherDeductions);
            result.setTotalDeductions(totalDeductions);

            // Net income
            BigDecimal netIncome = totalIncome.subtract(totalDeductions);
            result.setNetIncome(netIncome);
            result.setTaxableIncome(netIncome.max(BigDecimal.ZERO));

            // Load tax brackets and credits
            List<IncomeTaxBracket> federalBrackets = incomeTaxBracketRepository.findByTaxYearAndJurisdictionOrderByBracketOrderAsc(taxYear, "FEDERAL");
            List<IncomeTaxBracket> provincialBrackets = incomeTaxBracketRepository.findByTaxYearAndJurisdictionOrderByBracketOrderAsc(taxYear, profile.getProvince());

            // Calculate federal tax
            BigDecimal grossFederalTax = calculateProgressiveTax(result.getTaxableIncome(), federalBrackets);
            result.setGrossFederalTax(grossFederalTax);

            // Load federal credits
            List<TaxCredit> federalCredits = taxCreditRepository.findByTaxYearAndJurisdictionAndIsActiveTrueOrderByIdAsc(taxYear, "FEDERAL");
            BigDecimal federalCreditAmount = calculateCredits(federalCredits, profile);
            result.setFederalCredits(federalCreditAmount);

            BigDecimal netFederalTax = grossFederalTax.subtract(federalCreditAmount).max(BigDecimal.ZERO);
            result.setNetFederalTax(netFederalTax);

            // Calculate provincial tax
            BigDecimal grossProvincialTax = calculateProgressiveTax(result.getTaxableIncome(), provincialBrackets);
            result.setGrossProvincialTax(grossProvincialTax);

            // Load provincial credits
            List<TaxCredit> provincialCredits = taxCreditRepository.findByTaxYearAndJurisdictionAndIsActiveTrueOrderByIdAsc(taxYear, profile.getProvince());
            BigDecimal provincialCreditAmount = calculateCredits(provincialCredits, profile);
            result.setProvincialCredits(provincialCreditAmount);

            BigDecimal netProvincialTax = grossProvincialTax.subtract(provincialCreditAmount).max(BigDecimal.ZERO);
            result.setNetProvincialTax(netProvincialTax);

            // Calculate CPP and EI
            CppEiRate cppEiRate = cppEiRateRepository.findByTaxYear(taxYear)
                .orElse(null);

            if (cppEiRate != null) {
                // CPP: (earnedIncome - basicExemption) * rate, max at (maxPensionable - basicExemption) * rate
                BigDecimal cppEarnings = totalIncome.subtract(cppEiRate.getCppBasicExemption()).max(BigDecimal.ZERO);
                BigDecimal cppMaxEarnings = cppEiRate.getCppMaxPensionable().subtract(cppEiRate.getCppBasicExemption());
                BigDecimal cppContrib = cppEarnings.min(cppMaxEarnings).multiply(cppEiRate.getCppEmployeeRate())
                    .setScale(2, RoundingMode.HALF_UP);
                result.setCppContributions(cppContrib);

                // EI: earnedIncome * rate, max at maxInsurable * rate
                BigDecimal eiEarnings = totalIncome.min(cppEiRate.getEiMaxInsurable());
                BigDecimal eiPrem = eiEarnings.multiply(cppEiRate.getEiEmployeeRate())
                    .setScale(2, RoundingMode.HALF_UP);
                result.setEiPremiums(eiPrem);
            }

            // Total payable
            BigDecimal totalPayable = netFederalTax
                .add(netProvincialTax)
                .add(result.getCppContributions())
                .add(result.getEiPremiums());
            result.setTotalTaxPayable(totalPayable);

        } catch (Exception e) {
            log.error("Error calculating tax", e);
            result.setError("Calculation error: " + e.getMessage());
        }

        return result;
    }

    private BigDecimal calculateProgressiveTax(BigDecimal income, List<IncomeTaxBracket> brackets) {
        BigDecimal tax = BigDecimal.ZERO;

        for (IncomeTaxBracket bracket : brackets) {
            if (income.compareTo(bracket.getMinIncome()) <= 0) {
                break;
            }

            BigDecimal taxableInThisBracket;
            if (bracket.getMaxIncome() == null) {
                // Top bracket
                taxableInThisBracket = income.subtract(bracket.getMinIncome());
            } else {
                BigDecimal maxInBracket = bracket.getMaxIncome();
                if (income.compareTo(maxInBracket) >= 0) {
                    taxableInThisBracket = maxInBracket.subtract(bracket.getMinIncome());
                } else {
                    taxableInThisBracket = income.subtract(bracket.getMinIncome());
                }
            }

            tax = tax.add(taxableInThisBracket.multiply(bracket.getRate()));
        }

        return tax.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDonationCredit(List<DriverTaxEntry> entries) {
        BigDecimal donationAmount = entries.stream()
            .filter(e -> e.getEntryType().equals("DONATION"))
            .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Donation credit: 15% on first $200, 29.32% on rest (federal rates)
        if (donationAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal threshold = new BigDecimal("200.00");
        BigDecimal credit = BigDecimal.ZERO;

        if (donationAmount.compareTo(threshold) <= 0) {
            credit = donationAmount.multiply(new BigDecimal("0.15"));
        } else {
            BigDecimal lowCredit = threshold.multiply(new BigDecimal("0.15"));
            BigDecimal highCredit = donationAmount.subtract(threshold).multiply(new BigDecimal("0.2932"));
            credit = lowCredit.add(highCredit);
        }

        return credit.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCredits(List<TaxCredit> credits, DriverTaxProfile profile) {
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (TaxCredit credit : credits) {
            BigDecimal creditAmount = BigDecimal.ZERO;

            if (credit.getCreditCode().equals("BPA")) {
                creditAmount = credit.getAmount();
                if (credit.getRate() != null) {
                    creditAmount = credit.getAmount().multiply(credit.getRate());
                }
            } else if (credit.getCreditCode().equals("AGE_AMOUNT")) {
                // Apply if age 65+
                if (profile.getBirthYear() != null) {
                    int age = 2024 - profile.getBirthYear();
                    if (age >= 65) {
                        creditAmount = credit.getAmount();
                        if (credit.getRate() != null) {
                            creditAmount = credit.getAmount().multiply(credit.getRate());
                        }
                    }
                }
            } else if (credit.getCreditCode().equals("DISABILITY")) {
                // Apply if driver has disability
                if (Boolean.TRUE.equals(profile.getHasDisability())) {
                    creditAmount = credit.getAmount();
                    if (credit.getRate() != null) {
                        creditAmount = credit.getAmount().multiply(credit.getRate());
                    }
                }
            } else if (credit.getCreditCode().equals("CAREGIVER")) {
                // Apply if has dependents
                if (profile.getNumDependents() > 0) {
                    creditAmount = credit.getAmount();
                    if (credit.getRate() != null) {
                        creditAmount = credit.getAmount().multiply(credit.getRate());
                    }
                }
            }
            // Note: donation credits are calculated separately

            totalCredit = totalCredit.add(creditAmount);
        }

        return totalCredit.setScale(2, RoundingMode.HALF_UP);
    }
}
