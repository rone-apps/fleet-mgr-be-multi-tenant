package com.taxi.domain.report.service;

import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.shift.model.DriverShift;
import com.taxi.domain.shift.repository.DriverShiftRepository;
import com.taxi.web.dto.report.ChargesRevenueReportDTO;
import com.taxi.web.dto.report.CreditCardRevenueReportDTO;
import com.taxi.web.dto.report.DriverSummaryDTO;
import com.taxi.web.dto.report.DriverSummaryReportDTO;
import com.taxi.web.dto.report.LeaseExpenseReportDTO;
import com.taxi.web.dto.report.LeaseRevenueReportDTO;
import com.taxi.web.dto.report.OwnerReportDTO;
import com.taxi.web.dto.expense.StatementLineItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for generating various financial reports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final DriverRepository driverRepository;
    private final DriverShiftRepository driverShiftRepository;
    private final DriverFinancialCalculationService driverFinancialCalculationService;
    private final FinancialStatementService financialStatementService;
    private final com.taxi.domain.expense.repository.ItemRateRepository itemRateRepository;

    // ═══════════════════════════════════════════════════════════════════════
    // INDIVIDUAL REPORT METHODS - NOW DELEGATE TO SHARED SERVICE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Generate lease revenue report (INDIVIDUAL REPORT)
     * NOW DELEGATES to shared service for consistency
     */
    @Transactional(readOnly = true)
    public LeaseRevenueReportDTO generateLeaseRevenueReport(
            String ownerDriverNumber,
            LocalDate startDate,
            LocalDate endDate) {
        
        log.info("Generating lease revenue report for owner: {}", ownerDriverNumber);
        
        // ✅ DELEGATE TO SHARED SERVICE
        return driverFinancialCalculationService.calculateLeaseRevenue(
                ownerDriverNumber, startDate, endDate);
    }

    /**
     * Generate credit card revenue report (INDIVIDUAL REPORT)
     * NOW DELEGATES to shared service for consistency
     */
    @Transactional(readOnly = true)
    public CreditCardRevenueReportDTO generateCreditCardRevenueReport(
            String driverNumber,
            LocalDate startDate,
            LocalDate endDate) {
        
        log.info("Generating credit card revenue report for driver: {}", driverNumber);
        
        // ✅ DELEGATE TO SHARED SERVICE
        return driverFinancialCalculationService.calculateCreditCardRevenue(
                driverNumber, startDate, endDate);
    }

    /**
     * Generate charges revenue report (INDIVIDUAL REPORT)
     * NOW DELEGATES to shared service for consistency
     */
    @Transactional(readOnly = true)
    public ChargesRevenueReportDTO generateChargesRevenueReport(
            String driverNumber,
            LocalDate startDate,
            LocalDate endDate) {
        
        log.info("Generating charges revenue report for driver: {}", driverNumber);
        
        // ✅ DELEGATE TO SHARED SERVICE
        return driverFinancialCalculationService.calculateChargesRevenue(
                driverNumber, startDate, endDate);
    }

    /**
     * Generate lease expense report (INDIVIDUAL REPORT)
     * NOW DELEGATES to shared service for consistency
     */
    @Transactional(readOnly = true)
    public LeaseExpenseReportDTO generateLeaseExpenseReport(
            String driverNumber,
            LocalDate startDate,
            LocalDate endDate) {
        
        log.info("Generating lease expense report for driver: {}", driverNumber);
        
        // ✅ DELEGATE TO SHARED SERVICE
        return driverFinancialCalculationService.calculateLeaseExpense(
                driverNumber, startDate, endDate);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DRIVER SUMMARY REPORTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Generate comprehensive driver summary report
     * Shows financial summary for all drivers (or specific drivers) in a date range
     * 
     * OPTIMIZED VERSION: Uses bulk queries and reduces database calls
     * 
     * @param startDate Start date for the report period
     * @param endDate End date for the report period
     * @return Complete driver summary report with all financial metrics
     */
    @Transactional(readOnly = true)
    public DriverSummaryReportDTO generateDriverSummaryReport(
            LocalDate startDate,
            LocalDate endDate) {
        
        log.info("Generating driver summary report from {} to {}", startDate, endDate);
        long startTime = System.currentTimeMillis();

        // Initialize the report
        DriverSummaryReportDTO report = DriverSummaryReportDTO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // Get only ACTIVE drivers (including owners)
        List<Driver> allDrivers = driverRepository.findAll(
                org.springframework.data.jpa.domain.Specification.where(
                        (root, query, cb) -> cb.equal(root.get("status"), Driver.DriverStatus.ACTIVE)
                )
        );
        log.info("Processing {} active drivers", allDrivers.size());
        
        // Pre-fetch all data in bulk to reduce database calls
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        log.info("Pre-fetching all driver shifts for date range...");
        List<DriverShift> allDriverShifts = driverShiftRepository
                .findByLogonTimeBetween(startDateTime, endDateTime);
        log.info("Found {} driver shifts", allDriverShifts.size());
        
        // Group shifts by driver number for quick lookup
        java.util.Map<String, List<DriverShift>> shiftsByDriver = allDriverShifts.stream()
                .collect(java.util.stream.Collectors.groupingBy(DriverShift::getDriverNumber));
        
        // Process each driver
        int processedCount = 0;
        int skippedCount = 0;
        for (Driver driver : allDrivers) {
            processedCount++;
            if (processedCount % 10 == 0) {
                log.info("Processed {} / {} drivers", processedCount, allDrivers.size());
            }

            DriverSummaryDTO summary = calculateDriverSummaryOptimized(
                    driver, startDate, endDate, shiftsByDriver, false);
            
            // ✅ ONLY INCLUDE DRIVERS WITH FINANCIAL ACTIVITY
            if (hasFinancialActivity(summary)) {
                report.addDriverSummary(summary);
            } else {
                skippedCount++;
                log.debug("Skipping driver {} - no financial activity in period", driver.getDriverNumber());
            }
        }
        
        if (skippedCount > 0) {
            log.info("Skipped {} drivers with no financial activity", skippedCount);
        }
        
        // Calculate grand totals
        report.calculateGrandTotals();
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Generated driver summary report with {} drivers in {} ms", 
                report.getTotalDrivers(), duration);
        
        return report;
    }


    /**
     * ✅ SINGLE SOURCE OF TRUTH
     * Calculate financial summary for a single driver (SUMMARY REPORT)
     * NOW DELEGATES TO FinancialStatementService.generateOwnerReport()
     * This ensures summary reports use IDENTICAL logic to individual reports
     */
    private DriverSummaryDTO calculateDriverSummaryOptimized(
            Driver driver,
            LocalDate startDate,
            LocalDate endDate,
            java.util.Map<String, List<DriverShift>> shiftsByDriver,
            boolean quickMode) {

        log.debug("Calculating summary for driver: {} using generateOwnerReport (single source of truth) [{}]",
                driver.getDriverNumber(), quickMode ? "QUICK" : "FULL");

        DriverSummaryDTO summary = DriverSummaryDTO.builder()
                .driverId(driver.getId())                    // ✅ Include driver ID for fetching detailed reports
                .driverNumber(driver.getDriverNumber())
                .driverName(driver.getFullName())
                .isOwner(driver.getIsOwner())
                .build();

        try {
            // ✅ DELEGATE TO COMPREHENSIVE REPORT - ENSURES SINGLE SOURCE OF TRUTH
            // This uses the same detailed calculation logic as individual reports
            OwnerReportDTO fullReport =
                    financialStatementService.generateOwnerReport(driver.getId(), startDate, endDate);

            // Calculate totals from the detailed report
            fullReport.calculateTotals();

            // Convert OwnerReportDTO revenue totals to DriverSummaryDTO format
            // Group revenues by type using revenueSubType
            BigDecimal leaseIncome = fullReport.getRevenues().stream()
                    .filter(r -> "LEASE_INCOME".equals(r.getRevenueSubType()))
                    .map(r -> safeBigDecimal(r.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal creditCardRev = fullReport.getRevenues().stream()
                    .filter(r -> "CARD_REVENUE".equals(r.getRevenueSubType()))
                    .map(r -> safeBigDecimal(r.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal chargesRev = fullReport.getRevenues().stream()
                    .filter(r -> "ACCOUNT_REVENUE".equals(r.getRevenueSubType()))
                    .map(r -> safeBigDecimal(r.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal otherRev = fullReport.getRevenues().stream()
                    .filter(r -> "OTHER_REVENUE".equals(r.getRevenueSubType()) || "SHIFT_REVENUE".equals(r.getRevenueSubType()))
                    .map(r -> safeBigDecimal(r.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Set revenue breakdown
            summary.setLeaseRevenue(leaseIncome);
            summary.setCreditCardRevenue(creditCardRev);
            summary.setChargesRevenue(chargesRev);
            summary.setOtherRevenue(otherRev);

            // ✅ SINGLE SOURCE OF TRUTH: Extract ALL values from fullReport
            // The individual report (generateOwnerReport) is now the authority.
            // DO NOT recalculate lease/airport separately — use fullReport's values directly.
            summary.setFixedExpense(safeBigDecimal(fullReport.getTotalRecurringExpenses()));

            // ✅ Lease expense: sum from oneTimeExpenses where categoryCode=LEASE_EXP
            // This matches exactly what the individual report shows
            BigDecimal leaseExpenseTotal = BigDecimal.ZERO;
            BigDecimal otherExpenseTotal = BigDecimal.ZERO;
            BigDecimal airportExpenseTotal = BigDecimal.ZERO;
            int airportTripCount = 0;

            if (fullReport.getOneTimeExpenses() != null) {
                for (StatementLineItem expense : fullReport.getOneTimeExpenses()) {
                    String catCode = expense.getCategoryCode();

                    if ("LEASE_EXP".equals(catCode)) {
                        leaseExpenseTotal = leaseExpenseTotal.add(safeBigDecimal(expense.getAmount()));
                    } else if ("AIRPORT_TRIP".equals(catCode)) {
                        airportExpenseTotal = airportExpenseTotal.add(safeBigDecimal(expense.getAmount()));
                        if (expense.getTripCount() != null) {
                            airportTripCount += expense.getTripCount();
                        }
                    } else {
                        otherExpenseTotal = otherExpenseTotal.add(safeBigDecimal(expense.getAmount()));
                    }
                }
            }

            summary.setAirportTripCount(airportTripCount);
            summary.setAirportTripCost(airportExpenseTotal);

            // ✅ Insurance mileage expense from fullReport
            BigDecimal insuranceExpenseTotal = BigDecimal.ZERO;
            if (fullReport.getInsuranceMileageExpenses() != null && !fullReport.getInsuranceMileageExpenses().isEmpty()) {
                insuranceExpenseTotal = fullReport.getInsuranceMileageExpenses().stream()
                        .map(exp -> safeBigDecimal(exp.getAmount()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            summary.setLeaseExpense(leaseExpenseTotal);
            summary.setInsuranceMileageExpense(insuranceExpenseTotal);
            summary.setVariableExpense(BigDecimal.ZERO);
            summary.setOtherExpense(otherExpenseTotal);

            // Total expense = sum of all categories (matches fullReport.totalExpenses)
            BigDecimal recalculatedTotalExpense = safeBigDecimal(fullReport.getTotalRecurringExpenses())
                    .add(leaseExpenseTotal)
                    .add(otherExpenseTotal)
                    .add(insuranceExpenseTotal)
                    .add(airportExpenseTotal);

            log.debug("   Expense Breakdown for {}: Fixed=${}, Lease=${}, Other=${}, Insurance=${}, Airport=${} (trips={}) → Total=${}",
                    driver.getDriverNumber(),
                    fullReport.getTotalRecurringExpenses(),
                    leaseExpenseTotal,
                    otherExpenseTotal,
                    insuranceExpenseTotal,
                    airportExpenseTotal,
                    airportTripCount,
                    recalculatedTotalExpense);

            // Totals
            summary.setTotalRevenue(safeBigDecimal(fullReport.getTotalRevenues()));
            summary.setTotalExpense(recalculatedTotalExpense); // Use recalculated value for consistency

            // Calculate net and outstanding
            BigDecimal netOwed = safeBigDecimal(fullReport.getTotalRevenues())
                    .subtract(recalculatedTotalExpense);
            summary.setNetOwed(netOwed);
            summary.setPaid(safeBigDecimal(fullReport.getPaidAmount()));
            summary.setOutstanding(netOwed.subtract(safeBigDecimal(fullReport.getPaidAmount())));

            // ✅ ITEMIZED BREAKDOWN FOR DYNAMIC COLUMNS (Excel/PDF export)
            // ✅ ALWAYS populate breakdown items regardless of quickMode
            // The table needs these columns to display properly, even in quick mode
            // Extract revenue breakdown with unique keys and display names
            java.util.Map<String, DriverSummaryDTO.ItemizedBreakdown> revenueMap = new java.util.LinkedHashMap<>();

            // Revenue breakdown from fullReport.getRevenues()
            for (OwnerReportDTO.RevenueLineItem rev : fullReport.getRevenues()) {
                String subType = rev.getRevenueSubType();

                if ("CARD_REVENUE".equals(subType)) {
                    String key = "CC";
                    if (!revenueMap.containsKey(key)) {
                        revenueMap.put(key, DriverSummaryDTO.ItemizedBreakdown.builder()
                                .key(key)
                                .displayName("Credit Card")
                                .amount(BigDecimal.ZERO)
                                .build());
                    }
                    DriverSummaryDTO.ItemizedBreakdown item = revenueMap.get(key);
                    item.setAmount(item.getAmount().add(safeBigDecimal(rev.getAmount())));
                } else if ("ACCOUNT_REVENUE".equals(subType)) {
                    String key = "CHARGES";
                    if (!revenueMap.containsKey(key)) {
                        revenueMap.put(key, DriverSummaryDTO.ItemizedBreakdown.builder()
                                .key(key)
                                .displayName("Charges")
                                .amount(BigDecimal.ZERO)
                                .build());
                    }
                    DriverSummaryDTO.ItemizedBreakdown item = revenueMap.get(key);
                    item.setAmount(item.getAmount().add(safeBigDecimal(rev.getAmount())));
                } else if ("LEASE_INCOME".equals(subType)) {
                    String key = "LEASE_INC";
                    if (!revenueMap.containsKey(key)) {
                        revenueMap.put(key, DriverSummaryDTO.ItemizedBreakdown.builder()
                                .key(key)
                                .displayName("Lease Income")
                                .amount(BigDecimal.ZERO)
                                .build());
                    }
                    DriverSummaryDTO.ItemizedBreakdown item = revenueMap.get(key);
                    item.setAmount(item.getAmount().add(safeBigDecimal(rev.getAmount())));
                } else if ("OTHER_REVENUE".equals(subType) || "SHIFT_REVENUE".equals(subType)) {
                    // For OTHER_REVENUE, use description as unique key
                    String description = rev.getDescription() != null ? rev.getDescription().trim() : "Other Revenue";
                    String key = "OTHER:" + description;
                    if (!revenueMap.containsKey(key)) {
                        revenueMap.put(key, DriverSummaryDTO.ItemizedBreakdown.builder()
                                .key(key)
                                .displayName(description)
                                .amount(BigDecimal.ZERO)
                                .build());
                    }
                    DriverSummaryDTO.ItemizedBreakdown item = revenueMap.get(key);
                    item.setAmount(item.getAmount().add(safeBigDecimal(rev.getAmount())));
                }
            }
            summary.setRevenueBreakdown(new java.util.ArrayList<>(revenueMap.values()));

            // Extract expense breakdown with unique keys and display names
            java.util.Map<String, DriverSummaryDTO.ItemizedBreakdown> expenseMap = new java.util.LinkedHashMap<>();

            // Recurring expenses
            if (fullReport.getRecurringExpenses() != null) {
                for (StatementLineItem exp : fullReport.getRecurringExpenses()) {
                    String categoryName = exp.getCategoryName() != null ? exp.getCategoryName() : "Recurring Expense";
                    String key = "RECURRING:" + categoryName;
                    if (!expenseMap.containsKey(key)) {
                        expenseMap.put(key, DriverSummaryDTO.ItemizedBreakdown.builder()
                                .key(key)
                                .displayName(categoryName)
                                .amount(BigDecimal.ZERO)
                                .build());
                    }
                    DriverSummaryDTO.ItemizedBreakdown item = expenseMap.get(key);
                    item.setAmount(item.getAmount().add(safeBigDecimal(exp.getAmount())));
                }
            }

            // ✅ Expense breakdown from fullReport.oneTimeExpenses (grouped by type)
            if (fullReport.getOneTimeExpenses() != null) {
                for (StatementLineItem exp : fullReport.getOneTimeExpenses()) {
                    String categoryCode = exp.getCategoryCode();

                    if ("LEASE_EXP".equals(categoryCode)) {
                        // Accumulate lease into single breakdown item
                        if (!expenseMap.containsKey("LEASE_EXP")) {
                            expenseMap.put("LEASE_EXP", DriverSummaryDTO.ItemizedBreakdown.builder()
                                    .key("LEASE_EXP").displayName("Lease Expense").amount(BigDecimal.ZERO).build());
                        }
                        expenseMap.get("LEASE_EXP").setAmount(
                                expenseMap.get("LEASE_EXP").getAmount().add(safeBigDecimal(exp.getAmount())));
                    } else if ("AIRPORT_TRIP".equals(categoryCode)) {
                        // Accumulate airport into single breakdown item
                        if (!expenseMap.containsKey("AIRPORT")) {
                            expenseMap.put("AIRPORT", DriverSummaryDTO.ItemizedBreakdown.builder()
                                    .key("AIRPORT").displayName("Airport Trips").amount(BigDecimal.ZERO).build());
                        }
                        expenseMap.get("AIRPORT").setAmount(
                                expenseMap.get("AIRPORT").getAmount().add(safeBigDecimal(exp.getAmount())));
                    } else {
                        // Other one-time expenses, group by category
                        String categoryName = exp.getCategoryName() != null ? exp.getCategoryName() : "Other Expense";
                        String key = "ONETIME:" + categoryName;
                        if (!expenseMap.containsKey(key)) {
                            expenseMap.put(key, DriverSummaryDTO.ItemizedBreakdown.builder()
                                    .key(key).displayName(categoryName).amount(BigDecimal.ZERO).build());
                        }
                        expenseMap.get(key).setAmount(
                                expenseMap.get(key).getAmount().add(safeBigDecimal(exp.getAmount())));
                    }
                }
            }

            // Insurance mileage expenses
            if (insuranceExpenseTotal.compareTo(BigDecimal.ZERO) > 0) {
                expenseMap.put("INSURANCE", DriverSummaryDTO.ItemizedBreakdown.builder()
                        .key("INSURANCE").displayName("Insurance Mileage").amount(insuranceExpenseTotal).build());
            }

            // Update airport display name with trip count
            if (expenseMap.containsKey("AIRPORT") && airportTripCount > 0) {
                expenseMap.get("AIRPORT").setDisplayName(
                        String.format("Airport Trips (%d trips)", airportTripCount));
            }

            // ✅ ALWAYS populate breakdown items - needed for table columns
            summary.setRevenueBreakdown(new java.util.ArrayList<>(revenueMap.values()));
            summary.setExpenseBreakdown(new java.util.ArrayList<>(expenseMap.values()));
            log.debug("Completed breakdown for {}: {} revenue items, {} expense items",
                    driver.getDriverNumber(), revenueMap.size(), expenseMap.size());

            log.debug("Completed summary for {}: Revenue=${}, Expense=${}, NetOwed=${}, Outstanding=${}",
                    driver.getDriverNumber(),
                    summary.getTotalRevenue(),
                    summary.getTotalExpense(),
                    summary.getNetOwed(),
                    summary.getOutstanding());

        } catch (Exception e) {
            log.error("Error calculating summary for driver {}: {}", driver.getDriverNumber(), e.getMessage(), e);
            // Return zero summary on error rather than crashing
            summary.setLeaseRevenue(BigDecimal.ZERO);
            summary.setCreditCardRevenue(BigDecimal.ZERO);
            summary.setChargesRevenue(BigDecimal.ZERO);
            summary.setOtherRevenue(BigDecimal.ZERO);
            summary.setFixedExpense(BigDecimal.ZERO);
            summary.setLeaseExpense(BigDecimal.ZERO);
            summary.setVariableExpense(BigDecimal.ZERO);
            summary.setOtherExpense(BigDecimal.ZERO);
            summary.setPaid(BigDecimal.ZERO);
            summary.setOutstanding(BigDecimal.ZERO);
            summary.setTotalRevenue(BigDecimal.ZERO);
            summary.setTotalExpense(BigDecimal.ZERO);
            summary.setNetOwed(BigDecimal.ZERO);
        }

        return summary;
    }
    
    /**
     * Check if a driver has any financial activity in the date range
     * Returns true if driver has ANY revenue or expenses
     */
    private boolean hasFinancialActivity(DriverSummaryDTO summary) {
        BigDecimal zero = BigDecimal.ZERO;
        
        // Check if driver has any revenue
        boolean hasRevenue = 
            (summary.getLeaseRevenue() != null && summary.getLeaseRevenue().compareTo(zero) != 0) ||
            (summary.getCreditCardRevenue() != null && summary.getCreditCardRevenue().compareTo(zero) != 0) ||
            (summary.getChargesRevenue() != null && summary.getChargesRevenue().compareTo(zero) != 0) ||
            (summary.getOtherRevenue() != null && summary.getOtherRevenue().compareTo(zero) != 0);
        
        // Check if driver has any expenses
        boolean hasExpenses = 
            (summary.getFixedExpense() != null && summary.getFixedExpense().compareTo(zero) != 0) ||
            (summary.getLeaseExpense() != null && summary.getLeaseExpense().compareTo(zero) != 0) ||
            (summary.getVariableExpense() != null && summary.getVariableExpense().compareTo(zero) != 0) ||
            (summary.getOtherExpense() != null && summary.getOtherExpense().compareTo(zero) != 0);
        
        return hasRevenue || hasExpenses;
    }

    /**
     * Generate comprehensive driver summary report with PAGINATION
     * Shows financial summary for ACTIVE drivers only in a date range
     *
     * @param startDate Start date for the report period
     * @param endDate End date for the report period
     * @param pageable Pagination and sorting information
     * @param personType Filter: "DRIVER" (drivers only), "OWNER" (owners only), "ALL" (both)
     * @param quickMode If true, skip detailed breakdowns for faster response
     * @return Paginated driver summary report with all financial metrics
     */
    @Transactional(readOnly = true)
    public DriverSummaryReportDTO generateDriverSummaryReportPaginated(
            LocalDate startDate,
            LocalDate endDate,
            org.springframework.data.domain.Pageable pageable,
            String personType,
            boolean quickMode) {

        log.info("═══════════════════════════════════════════════════════════════════════");
        log.info("▶ REPORT GENERATION START - Driver Summary Report");
        log.info("  Date Range: {} to {} ({} days)", startDate, endDate,
                java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1);
        log.info("  Page: {} of {}, PersonType: {}, QuickMode: {}",
                pageable.getPageNumber() + 1, "?", personType, quickMode);
        log.info("═══════════════════════════════════════════════════════════════════════");
        long startTime = System.currentTimeMillis();
        long dbQueryStart = System.currentTimeMillis();

        // Build specification for filtering
        org.springframework.data.jpa.domain.Specification<Driver> spec =
                org.springframework.data.jpa.domain.Specification.where(
                        (root, query, cb) -> cb.equal(root.get("status"), Driver.DriverStatus.ACTIVE)
                );

        // Add person type filter
        if ("DRIVER".equalsIgnoreCase(personType)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isOwner"), false));
        } else if ("OWNER".equalsIgnoreCase(personType)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isOwner"), true));
        }
        // For "ALL", no additional filter needed

        // Get filtered ACTIVE drivers with pagination
        org.springframework.data.domain.Page<Driver> driverPage =
                driverRepository.findAll(spec, pageable);

        long dbQueryDuration = System.currentTimeMillis() - dbQueryStart;
        log.info("✓ Database query completed in {} ms", dbQueryDuration);
        log.info("  Found {} active drivers (page {} of {}, total {} drivers)",
                driverPage.getNumberOfElements(),
                driverPage.getNumber() + 1,
                driverPage.getTotalPages(),
                driverPage.getTotalElements());
        
        // Initialize the report
        DriverSummaryReportDTO report = DriverSummaryReportDTO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .currentPage(driverPage.getNumber())
                .totalPages(driverPage.getTotalPages())
                .totalElements(driverPage.getTotalElements())
                .pageSize(driverPage.getSize())
                .build();
        
        // Pre-fetch all data in bulk to reduce database calls
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        // Get driver numbers for current page
        List<String> driverNumbers = driverPage.getContent().stream()
                .map(Driver::getDriverNumber)
                .collect(java.util.stream.Collectors.toList());

        long shiftQueryStart = System.currentTimeMillis();
        log.info("  Pre-fetching driver shifts for {} drivers in date range...", driverNumbers.size());

        // Only fetch shifts for drivers in current page
        List<DriverShift> pageDriverShifts = driverShiftRepository
                .findByDriverNumberInAndLogonTimeBetween(driverNumbers, startDateTime, endDateTime);

        long shiftQueryDuration = System.currentTimeMillis() - shiftQueryStart;
        log.info("✓ Shift query completed in {} ms - Found {} driver shifts",
                shiftQueryDuration, pageDriverShifts.size());
        
        // Group shifts by driver number for quick lookup
        java.util.Map<String, List<DriverShift>> shiftsByDriver = pageDriverShifts.stream()
                .collect(java.util.stream.Collectors.groupingBy(DriverShift::getDriverNumber));
        
        // Process each driver in the page
        int processedCount = 0;
        int skippedCount = 0;
        int includedCount = 0;
        long processingStart = System.currentTimeMillis();

        log.info("  Starting driver processing loop...");

        for (Driver driver : driverPage.getContent()) {
            processedCount++;
            if (processedCount % 5 == 0 && processedCount > 0) {
                long elapsed = System.currentTimeMillis() - processingStart;
                double perDriver = (double) elapsed / processedCount;
                log.debug("Progress: {} / {} drivers ({} included, {} skipped) - {:.0f}ms per driver",
                        processedCount, driverPage.getNumberOfElements(), includedCount, skippedCount, perDriver);
            }

            DriverSummaryDTO summary = calculateDriverSummaryOptimized(
                    driver, startDate, endDate, shiftsByDriver, quickMode);

            // ✅ ONLY INCLUDE DRIVERS WITH FINANCIAL ACTIVITY
            if (hasFinancialActivity(summary)) {
                report.addDriverSummary(summary);
                includedCount++;
            } else {
                skippedCount++;
                log.debug("Skipping driver {} - no financial activity in period", driver.getDriverNumber());
            }
        }

        long processingDuration = System.currentTimeMillis() - processingStart;
        if (processedCount > 0) {
            double perDriver = (double) processingDuration / processedCount;
            log.info("✓ Driver processing completed in {} ms ({} included, {} skipped, {:.1f}ms per driver)",
                    processingDuration, includedCount, skippedCount, perDriver);
        }
        
        // Calculate page totals
        long pageTotalsStart = System.currentTimeMillis();
        report.calculatePageTotals();
        long pageTotalsDuration = System.currentTimeMillis() - pageTotalsStart;
        log.info("✓ Page totals calculated in {} ms", pageTotalsDuration);

        // Calculate grand totals (ALL pages) ONLY if on last page
        if (report.getCurrentPage() + 1 == report.getTotalPages()) {
            log.info("  Last page reached (page {} of {}) - calculating grand totals...",
                    report.getCurrentPage() + 1, report.getTotalPages());
            calculateGrandTotalsSimple(report, startDate, endDate);
        } else {
            log.info("  Page {} of {} - grand totals will be calculated on final page",
                    report.getCurrentPage() + 1, report.getTotalPages());
        }

        long totalDuration = System.currentTimeMillis() - startTime;

        log.info("═══════════════════════════════════════════════════════════════════════");
        log.info("✓ REPORT GENERATION COMPLETED SUCCESSFULLY");
        log.info("═══════════════════════════════════════════════════════════════════════");
        log.info("  SUMMARY - Date Range: {} to {} | Page: {} of {} | Mode: {} {}",
                startDate, endDate,
                report.getCurrentPage() + 1, report.getTotalPages(),
                quickMode ? "QUICK" : "DETAILED",
                quickMode ? "(summary only)" : "(full breakdown)");
        log.info("  Drivers: {} on page (included: {}, skipped: {}) | {} total active",
                driverPage.getNumberOfElements(), includedCount, skippedCount, report.getTotalElements());
        log.info("");

        // PAGE TOTALS
        log.info("  ┌─ PAGE TOTALS (Current Page)");
        log.info("  │  Revenue:");
        log.info("  │    Lease:              ${}", report.getPageLeaseRevenue());
        log.info("  │    Credit Card:        ${}", report.getPageCreditCardRevenue());
        log.info("  │    Charges:            ${}", report.getPageChargesRevenue());
        log.info("  │    Other:              ${}", report.getPageOtherRevenue());
        log.info("  │    ────────────────────────");
        log.info("  │    TOTAL REVENUE:      ${}", report.getPageTotalRevenue());
        log.info("  │");
        log.info("  │  Expense:");
        log.info("  │    Fixed:              ${}", report.getPageFixedExpense());
        log.info("  │    Lease:              ${}", report.getPageLeaseExpense());
        log.info("  │    Variable:           ${}", report.getPageVariableExpense());
        log.info("  │    Other:              ${}", report.getPageOtherExpense());
        log.info("  │    ────────────────────────");
        log.info("  │    TOTAL EXPENSE:      ${}", report.getPageTotalExpense());
        log.info("  │");
        log.info("  │  NET OWED:             ${}", report.getPageNetOwed());
        log.info("  │  PAID:                 ${}", report.getPageTotalPaid());
        log.info("  │  OUTSTANDING:          ${}", report.getPageTotalOutstanding());
        log.info("  └");

        // GRAND TOTALS (if last page)
        if (report.getCurrentPage() + 1 == report.getTotalPages()) {
            log.info("  ┌─ GRAND TOTALS (All {} Pages)", report.getTotalPages());
            log.info("  │  Revenue:");
            log.info("  │    Lease:              ${}", report.getGrandTotalLeaseRevenue());
            log.info("  │    Credit Card:        ${}", report.getGrandTotalCreditCardRevenue());
            log.info("  │    Charges:            ${}", report.getGrandTotalChargesRevenue());
            log.info("  │    Other:              ${}", report.getGrandTotalOtherRevenue());
            log.info("  │    ────────────────────────");
            log.info("  │    TOTAL REVENUE:      ${}", report.getGrandTotalRevenue());
            log.info("  │");
            log.info("  │  Expense:");
            log.info("  │    Fixed:              ${}", report.getGrandTotalFixedExpense());
            log.info("  │    Lease:              ${}", report.getGrandTotalLeaseExpense());
            log.info("  │    Variable:           ${}", report.getGrandTotalVariableExpense());
            log.info("  │    Other:              ${}", report.getGrandTotalOtherExpense());
            log.info("  │    ────────────────────────");
            log.info("  │    TOTAL EXPENSE:      ${}", report.getGrandTotalExpense());
            log.info("  │");
            log.info("  │  NET OWED:             ${}", report.getGrandNetOwed());
            log.info("  │  PAID:                 ${}", report.getGrandTotalPaid());
            log.info("  │  OUTSTANDING:          ${}", report.getGrandTotalOutstanding());
            log.info("  └");
        }

        log.info("  Total Processing Time: {} ms ({}.{} seconds)",
                totalDuration, totalDuration / 1000, totalDuration % 1000);
        log.info("═══════════════════════════════════════════════════════════════════════");

        return report;
    }
    
    /**
     * Calculate grand totals for ALL pages (only called on last page)
     * Calculates from all active drivers to ensure accuracy
     * Uses existing calculation service to ensure consistency with individual reports
     */
    private void calculateGrandTotalsSimple(
            DriverSummaryReportDTO report,
            LocalDate startDate,
            LocalDate endDate) {

        log.info("  ▶ Grand Totals Calculation START");
        long startTime = System.currentTimeMillis();

        // Get ALL active drivers
        long fetchStart = System.currentTimeMillis();
        List<Driver> allDrivers = driverRepository.findAll(
                org.springframework.data.jpa.domain.Specification.where(
                        (root, query, cb) -> cb.equal(root.get("status"), Driver.DriverStatus.ACTIVE)
                )
        );
        long fetchDuration = System.currentTimeMillis() - fetchStart;

        log.info("  ✓ Fetched {} total active drivers in {} ms", allDrivers.size(), fetchDuration);
        log.info("    Computing financial metrics for each driver...");

        BigDecimal grandLeaseRev = BigDecimal.ZERO;
        BigDecimal grandCCRev = BigDecimal.ZERO;
        BigDecimal grandChargesRev = BigDecimal.ZERO;
        BigDecimal grandOtherRev = BigDecimal.ZERO;
        BigDecimal grandFixedExp = BigDecimal.ZERO;
        BigDecimal grandLeaseExp = BigDecimal.ZERO;
        BigDecimal grandVarExp = BigDecimal.ZERO;
        BigDecimal grandOtherExp = BigDecimal.ZERO;
        BigDecimal grandTotalRev = BigDecimal.ZERO;
        BigDecimal grandTotalExp = BigDecimal.ZERO;
        BigDecimal grandNetOwed = BigDecimal.ZERO;
        BigDecimal grandPaid = BigDecimal.ZERO;
        BigDecimal grandOutstanding = BigDecimal.ZERO;

        // ✅ Maps to aggregate all individual breakdown items
        java.util.Map<String, DriverSummaryDTO.ItemizedBreakdown> aggregatedRevenueItems = new java.util.LinkedHashMap<>();
        java.util.Map<String, DriverSummaryDTO.ItemizedBreakdown> aggregatedExpenseItems = new java.util.LinkedHashMap<>();

        // ✅ IMPORTANT: Only count drivers that have financial activity
        // This matches the filtering done in the paginated report
        int driversWithActivity = 0;

        for (Driver driver : allDrivers) {
            try {
                DriverSummaryDTO summary = calculateDriverSummaryOptimized(
                        driver, startDate, endDate, new java.util.HashMap<>(), false
                );

                // ✅ ONLY INCLUDE DRIVERS WITH FINANCIAL ACTIVITY
                if (!hasFinancialActivity(summary)) {
                    continue;
                }

                driversWithActivity++;

                grandLeaseRev = grandLeaseRev.add(safeBigDecimal(summary.getLeaseRevenue()));
                grandCCRev = grandCCRev.add(safeBigDecimal(summary.getCreditCardRevenue()));
                grandChargesRev = grandChargesRev.add(safeBigDecimal(summary.getChargesRevenue()));
                grandOtherRev = grandOtherRev.add(safeBigDecimal(summary.getOtherRevenue()));
                grandFixedExp = grandFixedExp.add(safeBigDecimal(summary.getFixedExpense()));
                grandLeaseExp = grandLeaseExp.add(safeBigDecimal(summary.getLeaseExpense()));
                grandVarExp = grandVarExp.add(safeBigDecimal(summary.getVariableExpense()));
                grandOtherExp = grandOtherExp.add(safeBigDecimal(summary.getOtherExpense()));
                grandTotalRev = grandTotalRev.add(safeBigDecimal(summary.getTotalRevenue()));
                grandTotalExp = grandTotalExp.add(safeBigDecimal(summary.getTotalExpense()));
                grandNetOwed = grandNetOwed.add(safeBigDecimal(summary.getNetOwed()));
                grandPaid = grandPaid.add(safeBigDecimal(summary.getPaid()));
                grandOutstanding = grandOutstanding.add(safeBigDecimal(summary.getOutstanding()));

                // ✅ Aggregate individual breakdown items (Airport fees, Insurance, Bonus, etc.)
                if (summary.getRevenueBreakdown() != null) {
                    for (DriverSummaryDTO.ItemizedBreakdown item : summary.getRevenueBreakdown()) {
                        aggregatedRevenueItems.merge(item.getKey(),
                                item,
                                (existing, newItem) -> {
                                    existing.setAmount(safeBigDecimal(existing.getAmount())
                                            .add(safeBigDecimal(newItem.getAmount())));
                                    return existing;
                                });
                    }
                }

                if (summary.getExpenseBreakdown() != null) {
                    for (DriverSummaryDTO.ItemizedBreakdown item : summary.getExpenseBreakdown()) {
                        aggregatedExpenseItems.merge(item.getKey(),
                                item,
                                (existing, newItem) -> {
                                    existing.setAmount(safeBigDecimal(existing.getAmount())
                                            .add(safeBigDecimal(newItem.getAmount())));
                                    return existing;
                                });
                    }
                }

                if (driversWithActivity % 10 == 0) {
                    log.debug("Processed {} drivers with activity so far...", driversWithActivity);
                }

            } catch (Exception e) {
                log.error("Error calculating grand totals for driver {}: {}", driver.getDriverNumber(), e.getMessage());
                // Continue with next driver instead of stopping
            }
        }

        log.info("  ✓ Grand totals calculated from {} drivers with activity",
                driversWithActivity);
        log.info("    (out of {} total active drivers, {} had no activity in period)",
                allDrivers.size(), allDrivers.size() - driversWithActivity);

        // Set detailed grand totals
        report.setGrandTotalLeaseRevenue(grandLeaseRev);
        report.setGrandTotalCreditCardRevenue(grandCCRev);
        report.setGrandTotalChargesRevenue(grandChargesRev);
        report.setGrandTotalOtherRevenue(grandOtherRev);
        report.setGrandTotalFixedExpense(grandFixedExp);
        report.setGrandTotalLeaseExpense(grandLeaseExp);
        report.setGrandTotalVariableExpense(grandVarExp);
        report.setGrandTotalOtherExpense(grandOtherExp);
        report.setGrandTotalRevenue(grandTotalRev);
        report.setGrandTotalExpense(grandTotalExp);
        report.setGrandNetOwed(grandNetOwed);
        report.setGrandTotalPaid(grandPaid);
        report.setGrandTotalOutstanding(grandOutstanding);

        long duration = System.currentTimeMillis() - startTime;
        log.info("  ✓ Grand Totals Calculation COMPLETED in {} ms", duration);
        log.info("    DETAILED BREAKDOWN ({} individual items):",
                aggregatedRevenueItems.size() + aggregatedExpenseItems.size());

        // Log revenue items
        if (!aggregatedRevenueItems.isEmpty()) {
            log.info("      Revenue Items:");
            for (DriverSummaryDTO.ItemizedBreakdown item : aggregatedRevenueItems.values()) {
                log.info("        • {}: ${}", item.getDisplayName(), item.getAmount());
            }
        }

        // Log expense items
        if (!aggregatedExpenseItems.isEmpty()) {
            log.info("      Expense Items:");
            for (DriverSummaryDTO.ItemizedBreakdown item : aggregatedExpenseItems.values()) {
                log.info("        • {}: ${}", item.getDisplayName(), item.getAmount());
            }
        }

        log.info("      ────────────────────────────────────");
        log.info("      Total Revenue: ${}", grandTotalRev);
        log.info("      Total Expense: ${}", grandTotalExp);
        log.info("      Net Owed: ${}, Paid: ${}, Outstanding: ${}",
                grandNetOwed, grandPaid, grandOutstanding);
    }
    
    /**
     * Helper to safely get BigDecimal value
     */
    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }


    

}