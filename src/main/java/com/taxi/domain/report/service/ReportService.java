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
        
        // Get all drivers (including owners)
        List<Driver> allDrivers = driverRepository.findAll();
        log.info("Processing {} drivers", allDrivers.size());
        
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
     * Calculate financial summary for a single driver (SUMMARY REPORT)
     * NOW USES SAME shared service as individual reports
     */
    private DriverSummaryDTO calculateDriverSummaryOptimized(
            Driver driver,
            LocalDate startDate,
            LocalDate endDate,
            java.util.Map<String, List<DriverShift>> shiftsByDriver) {
        
        log.debug("Calculating summary for driver: {}", driver.getDriverNumber());
        
        DriverSummaryDTO summary = DriverSummaryDTO.builder()
                .driverNumber(driver.getDriverNumber())
                .driverName(driver.getFullName())
                .isOwner(driver.getIsOwner())
                .build();
        
        // ✅ ALL CALCULATIONS NOW USE SHARED SERVICE
        
        // Lease Revenue (for owners)
        if (driver.getIsOwner()) {
            LeaseRevenueReportDTO leaseRevReport = driverFinancialCalculationService
                    .calculateLeaseRevenue(driver.getDriverNumber(), startDate, endDate);
            summary.setLeaseRevenue(leaseRevReport.getGrandTotalLease());
        } else {
            summary.setLeaseRevenue(BigDecimal.ZERO);
        }
        
        // Credit Card Revenue
        CreditCardRevenueReportDTO ccReport = driverFinancialCalculationService
                .calculateCreditCardRevenue(driver.getDriverNumber(), startDate, endDate);
        summary.setCreditCardRevenue(ccReport.getGrandTotal());
        
        // Account Charges Revenue
        ChargesRevenueReportDTO chargesReport = driverFinancialCalculationService
                .calculateChargesRevenue(driver.getDriverNumber(), startDate, endDate);
        summary.setChargesRevenue(chargesReport.getGrandTotal());
        
        // Fixed Expenses
        FixedExpenseReportDTO fixedExpReport = driverFinancialCalculationService
                .calculateFixedExpenses(driver.getDriverNumber(), startDate, endDate);
        summary.setFixedExpense(fixedExpReport.getTotalAmount());
        
        // Lease Expense
        LeaseExpenseReportDTO leaseExpReport = driverFinancialCalculationService
                .calculateLeaseExpense(driver.getDriverNumber(), startDate, endDate);
        summary.setLeaseExpense(leaseExpReport.getGrandTotalLease());
        
        // Variable Expenses (not yet implemented)
        summary.setVariableExpense(BigDecimal.ZERO);
        
        // Payments (not yet implemented)
        summary.setPaid(BigDecimal.ZERO);
        
        // Calculate derived fields (totalRevenue, totalExpense, netOwed, etc.)
        summary.calculateAll();
        
        log.debug("Completed summary for {}: Revenue=${}, Expense=${}, NetOwed=${}", 
                driver.getDriverNumber(), 
                summary.getTotalRevenue(), 
                summary.getTotalExpense(), 
                summary.getNetOwed());
        
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
     * Simplified version - just sums all active drivers
     */
    private void calculateGrandTotalsSimple(
            DriverSummaryReportDTO report,
            LocalDate startDate,
            LocalDate endDate) {
        
        // Get ALL active drivers
        List<Driver> allDrivers = driverRepository.findAll(
                org.springframework.data.jpa.domain.Specification.where(
                        (root, query, cb) -> cb.equal(root.get("status"), Driver.DriverStatus.ACTIVE)
                )
        );
        
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
        
        // Simple calculation for each driver
        int totalDriversProcessed = 0;
        int driversWithActivity = 0;
        for (Driver driver : allDrivers) {
            totalDriversProcessed++;
            DriverSummaryDTO summary = calculateDriverSummaryOptimized(
                    driver, startDate, endDate, new java.util.HashMap<>()
            );
            
            // ✅ ONLY COUNT DRIVERS WITH FINANCIAL ACTIVITY
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
        }
        
        log.info("Grand totals calculated from {} drivers with activity (out of {} total active drivers)", 
                driversWithActivity, totalDriversProcessed);
        
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
        
        log.info("Grand totals: Revenue=${}, Expense=${}, Net=${}", 
                grandTotalRev, grandTotalExp, grandNetOwed);
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