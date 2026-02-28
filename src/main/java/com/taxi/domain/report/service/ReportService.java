package com.taxi.domain.report.service;

import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.shift.model.DriverShift;
import com.taxi.domain.shift.repository.DriverShiftRepository;
import com.taxi.web.dto.report.ChargesRevenueReportDTO;
import com.taxi.web.dto.report.CreditCardRevenueReportDTO;
import com.taxi.web.dto.report.DriverSummaryDTO;
import com.taxi.web.dto.report.DriverSummaryReportDTO;
import com.taxi.web.dto.report.FixedExpenseReportDTO;
import com.taxi.web.dto.report.LeaseExpenseReportDTO;
import com.taxi.web.dto.report.LeaseRevenueReportDTO;
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
    private final com.taxi.domain.expense.service.FinancialStatementService financialStatementService;
    private final com.taxi.domain.cab.repository.CabRepository cabRepository;
    private final com.taxi.domain.shift.repository.ShiftOwnershipRepository shiftOwnershipRepository;
    private final com.taxi.domain.lease.service.LeaseCalculationService leaseCalculationService;

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
                    driver, startDate, endDate, shiftsByDriver);
            
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
     * Calculate lease expense for a driver from their shifts
     * This matches the Lease Report logic for consistency
     * Includes both base rate and mileage-based lease
     */
    private BigDecimal calculateLeaseExpenseFromShifts(Driver driver, LocalDate startDate, LocalDate endDate) {
        BigDecimal totalLease = BigDecimal.ZERO;

        try {
            // Get all driver shifts in the date range
            List<DriverShift> allShifts = driverShiftRepository.findByDriverNumberAndDateRange(
                    driver.getDriverNumber(), startDate, endDate);

            log.debug("Calculating lease for {} shifts of driver {}", allShifts.size(), driver.getDriverNumber());

            for (DriverShift ds : allShifts) {
                try {
                    // Find cab
                    var cabOpt = cabRepository.findByCabNumber(ds.getCabNumber());
                    if (cabOpt.isEmpty()) {
                        continue;
                    }
                    com.taxi.domain.cab.model.Cab cab = cabOpt.get();

                    // Find cab shift (DAY or NIGHT)
                    com.taxi.domain.shift.model.ShiftType cabShiftType =
                        "DAY".equals(ds.getPrimaryShiftType()) ?
                        com.taxi.domain.shift.model.ShiftType.DAY :
                        com.taxi.domain.shift.model.ShiftType.NIGHT;

                    var cabShiftOpt = cab.getShifts().stream()
                        .filter(s -> s.getShiftType() == cabShiftType).findFirst();

                    if (cabShiftOpt.isEmpty()) {
                        continue;
                    }

                    // Find owner on this date
                    LocalDate shiftDate = ds.getLogonTime().toLocalDate();
                    var ownershipOpt = shiftOwnershipRepository.findOwnershipOnDate(
                            cabShiftOpt.get().getId(), shiftDate);

                    if (ownershipOpt.isEmpty()) {
                        continue;
                    }

                    Driver owner = ownershipOpt.get().getOwner();

                    // Skip if self-driven (driver is the owner)
                    if (owner.getDriverNumber().equals(ds.getDriverNumber())) {
                        continue;
                    }

                    // ✅ Calculate BASE rate for this shift
                    BigDecimal baseRate = driverFinancialCalculationService.getApplicableLeaseRate(
                            owner.getDriverNumber(), ds.getCabNumber(), ds.getPrimaryShiftType(),
                            ds.getLogonTime(), cab);

                    // ✅ Calculate MILEAGE-BASED lease (if applicable)
                    BigDecimal shiftLease = baseRate;
                    BigDecimal miles = ds.getTotalDistance();

                    if (miles != null && miles.compareTo(BigDecimal.ZERO) > 0) {
                        // Get mileage rate from lease configuration
                        try {
                            // Get lease rate using same logic as DriverFinancialCalculationService
                            com.taxi.domain.lease.model.LeaseRate leaseRate = leaseCalculationService.findApplicableRate(
                                null,  // cabType - could be obtained from CabShift attributes
                                false, // hasAirportLicense - could be obtained from CabShift attributes
                                ds.getLogonTime()
                            );

                            if (leaseRate != null) {
                                BigDecimal mileageRate = leaseRate.getMileageRate();
                                if (mileageRate != null && mileageRate.compareTo(BigDecimal.ZERO) > 0) {
                                    BigDecimal mileageLease = mileageRate.multiply(miles);
                                    shiftLease = baseRate.add(mileageLease);
                                    log.debug("Shift {}: base=${}, mileage=${}*{}mi = ${}",
                                        ds.getId(), baseRate, mileageRate, miles, shiftLease);
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Could not get lease rate for mileage calculation on shift {}: {}",
                                ds.getId(), e.getMessage());
                            shiftLease = baseRate;
                        }
                    }

                    totalLease = totalLease.add(safeBigDecimal(shiftLease));

                } catch (Exception e) {
                    log.debug("Error calculating lease for shift {}: {}", ds.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error calculating lease expenses for driver {}: {}", driver.getDriverNumber(), e.getMessage());
        }

        log.debug("Total lease expense for driver {}: {}", driver.getDriverNumber(), totalLease);
        return totalLease;
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
            java.util.Map<String, List<DriverShift>> shiftsByDriver) {

        log.debug("Calculating summary for driver: {} using generateOwnerReport (single source of truth)", driver.getDriverNumber());

        DriverSummaryDTO summary = DriverSummaryDTO.builder()
                .driverId(driver.getId())                    // ✅ Include driver ID for fetching detailed reports
                .driverNumber(driver.getDriverNumber())
                .driverName(driver.getFullName())
                .isOwner(driver.getIsOwner())
                .build();

        try {
            // ✅ DELEGATE TO COMPREHENSIVE REPORT - ENSURES SINGLE SOURCE OF TRUTH
            // This uses the same detailed calculation logic as individual reports
            com.taxi.web.dto.expense.OwnerReportDTO fullReport =
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

            // Expenses breakdown (all from fullReport)
            summary.setFixedExpense(safeBigDecimal(fullReport.getTotalRecurringExpenses()));

            // ✅ Calculate lease expenses from driver shifts (matches Lease Report logic for consistency)
            BigDecimal leaseExpenseTotal = calculateLeaseExpenseFromShifts(driver, startDate, endDate);

            // ✅ Calculate other one-time expenses from fullReport (excluding lease which we calculated from shifts)
            BigDecimal otherExpenseTotal = BigDecimal.ZERO;
            if (fullReport.getOneTimeExpenses() != null && !fullReport.getOneTimeExpenses().isEmpty()) {
                for (com.taxi.web.dto.expense.StatementLineItem expense : fullReport.getOneTimeExpenses()) {
                    // Exclude lease expenses - we calculated those from shifts
                    if (!((expense.getCategoryCode() != null && expense.getCategoryCode().equals("LEASE_EXP")) ||
                        (expense.getApplicationType() != null && expense.getApplicationType().equals("LEASE_RENT")))) {
                        otherExpenseTotal = otherExpenseTotal.add(safeBigDecimal(expense.getAmount()));
                        log.debug("Added to other expense: {} - {}", expense.getCategoryCode(), expense.getAmount());
                    }
                }
            }

            summary.setLeaseExpense(leaseExpenseTotal);
            summary.setVariableExpense(BigDecimal.ZERO); // Not tracked separately in detailed report
            summary.setOtherExpense(otherExpenseTotal);

            // Totals from detailed report
            summary.setTotalRevenue(safeBigDecimal(fullReport.getTotalRevenues()));
            summary.setTotalExpense(safeBigDecimal(fullReport.getTotalExpenses()));

            // Calculate net and outstanding
            BigDecimal netOwed = safeBigDecimal(fullReport.getTotalRevenues())
                    .subtract(safeBigDecimal(fullReport.getTotalExpenses()));
            summary.setNetOwed(netOwed);
            summary.setPaid(safeBigDecimal(fullReport.getPaidAmount()));
            summary.setOutstanding(netOwed.subtract(safeBigDecimal(fullReport.getPaidAmount())));

            // ✅ ITEMIZED BREAKDOWN FOR DYNAMIC COLUMNS (Excel/PDF export)
            // Extract revenue breakdown with unique keys and display names
            java.util.Map<String, DriverSummaryDTO.ItemizedBreakdown> revenueMap = new java.util.LinkedHashMap<>();

            // Revenue breakdown from fullReport.getRevenues()
            for (com.taxi.web.dto.expense.OwnerReportDTO.RevenueLineItem rev : fullReport.getRevenues()) {
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
                for (com.taxi.web.dto.expense.StatementLineItem exp : fullReport.getRecurringExpenses()) {
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

            // One-time expenses (split lease vs non-lease)
            if (fullReport.getOneTimeExpenses() != null) {
                for (com.taxi.web.dto.expense.StatementLineItem exp : fullReport.getOneTimeExpenses()) {
                    String categoryCode = exp.getCategoryCode();
                    String applicationType = exp.getApplicationType();

                    // Check if lease expense
                    if ((categoryCode != null && categoryCode.equals("LEASE_EXP")) ||
                        (applicationType != null && applicationType.equals("LEASE_RENT"))) {
                        String key = "LEASE_EXP";
                        if (!expenseMap.containsKey(key)) {
                            expenseMap.put(key, DriverSummaryDTO.ItemizedBreakdown.builder()
                                    .key(key)
                                    .displayName("Lease Expense")
                                    .amount(BigDecimal.ZERO)
                                    .build());
                        }
                        DriverSummaryDTO.ItemizedBreakdown item = expenseMap.get(key);
                        item.setAmount(item.getAmount().add(safeBigDecimal(exp.getAmount())));
                    } else {
                        // Non-lease one-time expense, group by category
                        String categoryName = exp.getCategoryName() != null ? exp.getCategoryName() : "Other Expense";
                        String key = "ONETIME:" + categoryName;
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
            }

            // Insurance mileage expenses
            if (fullReport.getInsuranceMileageExpenses() != null && !fullReport.getInsuranceMileageExpenses().isEmpty()) {
                BigDecimal totalInsurance = fullReport.getInsuranceMileageExpenses().stream()
                        .map(exp -> safeBigDecimal(exp.getAmount()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (totalInsurance.compareTo(BigDecimal.ZERO) > 0) {
                    expenseMap.put("INSURANCE", DriverSummaryDTO.ItemizedBreakdown.builder()
                            .key("INSURANCE")
                            .displayName("Insurance Mileage")
                            .amount(totalInsurance)
                            .build());
                }
            }

            summary.setExpenseBreakdown(new java.util.ArrayList<>(expenseMap.values()));

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
     * @return Paginated driver summary report with all financial metrics
     */
    @Transactional(readOnly = true)
    public DriverSummaryReportDTO generateDriverSummaryReportPaginated(
            LocalDate startDate,
            LocalDate endDate,
            org.springframework.data.domain.Pageable pageable) {
        
        log.info("Generating PAGINATED driver summary report from {} to {}, page={}, size={}", 
                startDate, endDate, pageable.getPageNumber(), pageable.getPageSize());
        long startTime = System.currentTimeMillis();
        
        // Get only ACTIVE drivers with pagination
        org.springframework.data.domain.Page<Driver> driverPage = 
                driverRepository.findAll(
                        org.springframework.data.jpa.domain.Specification.where(
                                (root, query, cb) -> cb.equal(root.get("status"), Driver.DriverStatus.ACTIVE)
                        ),
                        pageable
                );
        
        log.info("Processing {} active drivers (page {} of {})", 
                driverPage.getNumberOfElements(),
                driverPage.getNumber() + 1,
                driverPage.getTotalPages());
        
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
        
        log.info("Pre-fetching driver shifts for {} drivers...", driverNumbers.size());
        
        // Only fetch shifts for drivers in current page
        List<DriverShift> pageDriverShifts = driverShiftRepository
                .findByDriverNumberInAndLogonTimeBetween(driverNumbers, startDateTime, endDateTime);
        
        log.info("Found {} driver shifts for current page", pageDriverShifts.size());
        
        // Group shifts by driver number for quick lookup
        java.util.Map<String, List<DriverShift>> shiftsByDriver = pageDriverShifts.stream()
                .collect(java.util.stream.Collectors.groupingBy(DriverShift::getDriverNumber));
        
        // Process each driver in the page
        int processedCount = 0;
        int skippedCount = 0;
        for (Driver driver : driverPage.getContent()) {
            processedCount++;
            if (processedCount % 10 == 0) {
                log.info("Processed {} / {} drivers", processedCount, driverPage.getNumberOfElements());
            }
            
            DriverSummaryDTO summary = calculateDriverSummaryOptimized(
                    driver, startDate, endDate, shiftsByDriver);
            
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
        
        // Calculate page totals
        report.calculatePageTotals();
        
        // Calculate grand totals (ALL pages) ONLY if on last page
        if (report.getCurrentPage() + 1 == report.getTotalPages()) {
            log.info("Last page reached - calculating grand totals for all {} drivers", report.getTotalElements());
            calculateGrandTotalsSimple(report, startDate, endDate);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Generated paginated driver summary report with {} drivers in {} ms (page {} of {})", 
                report.getTotalDrivers(), duration, 
                report.getCurrentPage() + 1, report.getTotalPages());
        
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

        log.info("Calculating grand totals for ALL active drivers (last page reached)");
        long startTime = System.currentTimeMillis();

        // Get ALL active drivers
        List<Driver> allDrivers = driverRepository.findAll(
                org.springframework.data.jpa.domain.Specification.where(
                        (root, query, cb) -> cb.equal(root.get("status"), Driver.DriverStatus.ACTIVE)
                )
        );

        log.info("Processing {} total active drivers for grand totals", allDrivers.size());

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

        // ✅ IMPORTANT: Only count drivers that have financial activity
        // This matches the filtering done in the paginated report
        int driversWithActivity = 0;

        for (Driver driver : allDrivers) {
            try {
                DriverSummaryDTO summary = calculateDriverSummaryOptimized(
                        driver, startDate, endDate, new java.util.HashMap<>()
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

                if (driversWithActivity % 10 == 0) {
                    log.debug("Processed {} drivers with activity so far...", driversWithActivity);
                }

            } catch (Exception e) {
                log.error("Error calculating grand totals for driver {}: {}", driver.getDriverNumber(), e.getMessage());
                // Continue with next driver instead of stopping
            }
        }

        log.info("Grand totals calculated from {} drivers with activity (out of {} total active drivers)",
                driversWithActivity, allDrivers.size());

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
        log.info("Grand totals completed in {} ms", duration);
        log.info("Grand totals: Revenue=${}, Expense=${}, Net=${}, Outstanding=${}",
                grandTotalRev, grandTotalExp, grandNetOwed, grandOutstanding);
        log.info("Breakdown - Lease Rev: ${}, CC Rev: ${}, Charges Rev: ${}, Other Rev: ${}",
                grandLeaseRev, grandCCRev, grandChargesRev, grandOtherRev);
        log.info("Breakdown - Fixed Exp: ${}, Lease Exp: ${}, Var Exp: ${}, Other Exp: ${}",
                grandFixedExp, grandLeaseExp, grandVarExp, grandOtherExp);
    }
    
    /**
     * Helper to safely get BigDecimal value
     */
    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }


    

}