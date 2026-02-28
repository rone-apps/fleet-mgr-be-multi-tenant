package com.taxi.domain.report.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.expense.model.OneTimeExpense;
import com.taxi.domain.expense.model.RecurringExpense;
import com.taxi.domain.expense.repository.OneTimeExpenseRepository;
import com.taxi.domain.expense.repository.RecurringExpenseRepository;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.web.dto.report.FixedExpenseItemDTO;
import com.taxi.web.dto.report.FixedExpenseReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Fixed Expense Report Service
 * Uses existing RecurringExpense and OneTimeExpense entities
 * 
 * BUSINESS RULE: INACTIVE CAB/SHIFT FILTERING
 * ✅ Expenses tied to INACTIVE shifts are excluded
 * ✅ Expenses tied to RETIRED or MAINTENANCE cabs are excluded
 * ✅ Only ACTIVE cabs with ACTIVE shifts incur expenses
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FixedExpenseReportService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final OneTimeExpenseRepository oneTimeExpenseRepository;
    private final DriverRepository driverRepository;
    private final CabShiftRepository cabShiftRepository;

    /**
     * Check if a cab has at least one active shift
     * Note: Cab status has been moved to shift level
     * DUPLICATE: Also exists in DriverFinancialCalculationService - consider consolidation
     */
    private boolean isCabActive(Cab cab) {
        if (cab == null || cab.getShifts() == null) return false;
        return cab.getShifts().stream()
                .anyMatch(shift -> shift.getStatus() == CabShift.ShiftStatus.ACTIVE);
    }

    /**
     * Check if a shift is active
     * DUPLICATE: Also exists in DriverFinancialCalculationService
     */
    private boolean isShiftActive(CabShift shift) {
        if (shift == null) return false;
        return shift.getStatus() == CabShift.ShiftStatus.ACTIVE;
    }

    /**
     * Check if both cab and shift are active (used for expense calculations)
     * DUPLICATE: Also exists in DriverFinancialCalculationService
     */
    private boolean isCabShiftActive(CabShift shift) {
        if (shift == null) return false;
        return isCabActive(shift.getCab()) && isShiftActive(shift);
    }

    @Transactional(readOnly = true)
    public FixedExpenseReportDTO generateFixedExpenseReport(
            String driverNumber,
            LocalDate startDate,
            LocalDate endDate) {
        
        log.info("Generating fixed expense report for driver: {} from {} to {}", 
                driverNumber, startDate, endDate);
        
        Driver driver = driverRepository.findByDriverNumber(driverNumber)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverNumber));
        
        FixedExpenseReportDTO report = FixedExpenseReportDTO.builder()
                .driverNumber(driverNumber)
                .driverName(driver.getFullName())
                .startDate(startDate)
                .endDate(endDate)
                .build();
        
        // Process recurring expenses
        processRecurringExpenses(driver, startDate, endDate, report);
        
        // Process one-time expenses
        processOneTimeExpenses(driver, startDate, endDate, report);
        
        report.calculateSummary();
        
        log.info("Generated report with {} items, total: ${}", 
                report.getTotalExpenses(), report.getTotalAmount());
        
        return report;
    }
    
    private void processRecurringExpenses(Driver driver, LocalDate start, LocalDate end,
            FixedExpenseReportDTO report) {

        List<RecurringExpense> expenses = recurringExpenseRepository
                .findEffectiveBetween(start, end);

        for (RecurringExpense expense : expenses) {
            // ✅ FIXED: Add null check for entityType
            if (expense.getEntityType() == null) {
                log.warn("RecurringExpense id {} has null entityType - skipping", expense.getId());
                continue;
            }

            switch (expense.getEntityType()) {
                case CAB -> processRecurringCab(expense, driver, start, end, report);
                case SHIFT -> processRecurringShift(expense, driver, start, end, report);
                case OWNER, DRIVER -> processRecurringDriver(expense, driver, start, end, report);
            }
        }
    }
    
    private void processRecurringCab(RecurringExpense expense, Driver driver,
            LocalDate start, LocalDate end, FixedExpenseReportDTO report) {
        
        // Cab relationship is set by controller when creating/updating expense
        Cab cab = expense.getCab();
        if (cab == null) {
            log.warn("Cab not found for expense id: {}", expense.getId());
            return;
        }
        
        // ✅ BUSINESS RULE: Skip expenses for INACTIVE cabs
        if (!isCabActive(cab)) {
            log.debug("   Skipping expense for cab with no active shifts: {}",
                    cab.getCabNumber());
            return;
        }
        
        List<CabShift> shifts = cabShiftRepository.findByCab(cab);
        if (shifts.isEmpty()) return;
        
        // Filter to only ACTIVE shifts
        List<CabShift> activeShifts = shifts.stream()
                .filter(this::isShiftActive)
                .toList();
        
        if (activeShifts.isEmpty()) {
            log.debug("   Skipping expense - cab {} has no ACTIVE shifts", cab.getCabNumber());
            return;
        }
        
        BigDecimal splitAmount = expense.getAmount()
                .divide(BigDecimal.valueOf(activeShifts.size()), 2, RoundingMode.HALF_UP);
        
        for (CabShift shift : activeShifts) {
            if (shift.getCurrentOwner() != null && 
                shift.getCurrentOwner().getDriverNumber().equals(driver.getDriverNumber())) {
                
                addMonthlyCharges(expense, shift, shift.getCurrentOwner(), splitAmount,
                        activeShifts.size() == 2 ? "50% of cab expense" : 
                            String.format("%.0f%% of cab expense", (100.0 / activeShifts.size())),
                        start, end, report);
            }
        }
    }
    
    private void processRecurringShift(RecurringExpense expense, Driver driver,
            LocalDate start, LocalDate end, FixedExpenseReportDTO report) {

        // SHIFT expenses apply to ALL active shifts owned by this driver for ACTIVE cabs
        List<CabShift> ownedShifts = cabShiftRepository.findByCurrentOwner(driver);
        
        int skippedInactive = 0;
        
        for (CabShift shift : ownedShifts) {
            // ✅ BUSINESS RULE: Skip INACTIVE shifts
            if (!isShiftActive(shift)) {
                skippedInactive++;
                continue;
            }
            
            // ✅ BUSINESS RULE: Skip shifts on RETIRED or MAINTENANCE cabs
            if (!isCabActive(shift.getCab())) {
                skippedInactive++;
                log.debug("   Skipping shift expense - cab {} has no active shifts",
                        shift.getCab().getCabNumber());
                continue;
            }

            addMonthlyCharges(expense, shift, shift.getCurrentOwner(), expense.getAmount(),
                    "Full shift expense", start, end, report);
        }
        
        if (skippedInactive > 0) {
            log.debug("   Skipped {} INACTIVE shifts or shifts on INACTIVE cabs for driver {}", 
                    skippedInactive, driver.getDriverNumber());
        }
    }
    
    private void processRecurringDriver(RecurringExpense expense, Driver driver,
            LocalDate start, LocalDate end, FixedExpenseReportDTO report) {
        
        // Driver/Owner relationship is set by controller when creating/updating expense
        Driver expenseDriver = expense.getDriver() != null ? 
                expense.getDriver() : expense.getOwner();
        
        if (expenseDriver != null && 
            expenseDriver.getDriverNumber().equals(driver.getDriverNumber())) {
            
            addMonthlyChargesForDriver(expense, driver, expense.getAmount(),
                    "Full driver expense", start, end, report);
        }
    }
    
    private void addMonthlyCharges(RecurringExpense expense, CabShift shift, Driver owner,
            BigDecimal amount, String splitNote, LocalDate start, LocalDate end,
            FixedExpenseReportDTO report) {
        
        LocalDate current = start.withDayOfMonth(1);
        LocalDate last = end.withDayOfMonth(1);
        
        while (!current.isAfter(last)) {
            if (isActive(expense, current)) {
                BigDecimal chargeAmount = calculateAmount(expense, amount, current, start, end);
                
                report.addExpenseItem(FixedExpenseItemDTO.builder()
                        .expenseId(expense.getId())
                        .description(expense.getExpenseCategory().getCategoryName() + " (" + 
                                current.getMonth() + " " + current.getYear() + ")")
                        .category(expense.getExpenseCategory().getCategoryName())
                        .expenseType("RECURRING")
                        .assignedTo(shift.getCab().getCabNumber() + " " + shift.getShiftType())
                        .assignedToType(expense.getEntityType().toString())
                        .originalAmount(expense.getAmount())
                        .chargedAmount(chargeAmount)
                        .splitNote(splitNote)
                        .startDate(expense.getEffectiveFrom())
                        .endDate(expense.getEffectiveTo())
                        .ownerDriverNumber(owner.getDriverNumber())
                        .ownerDriverName(owner.getFullName())
                        .cabNumber(shift.getCab().getCabNumber())
                        .shiftType(shift.getShiftType().toString())
                        .build());
            }
            current = current.plusMonths(1);
        }
    }
    
    private void addMonthlyChargesForDriver(RecurringExpense expense, Driver driver,
            BigDecimal amount, String splitNote, LocalDate start, LocalDate end,
            FixedExpenseReportDTO report) {
        
        LocalDate current = start.withDayOfMonth(1);
        LocalDate last = end.withDayOfMonth(1);
        
        while (!current.isAfter(last)) {
            if (isActive(expense, current)) {
                BigDecimal chargeAmount = calculateAmount(expense, amount, current, start, end);
                
                report.addExpenseItem(FixedExpenseItemDTO.builder()
                        .expenseId(expense.getId())
                        .description(expense.getExpenseCategory().getCategoryName() + " (" + 
                                current.getMonth() + " " + current.getYear() + ")")
                        .category(expense.getExpenseCategory().getCategoryName())
                        .expenseType("RECURRING")
                        .assignedTo(driver.getFullName())
                        .assignedToType(expense.getEntityType().toString())
                        .originalAmount(expense.getAmount())
                        .chargedAmount(chargeAmount)
                        .splitNote(splitNote)
                        .startDate(expense.getEffectiveFrom())
                        .endDate(expense.getEffectiveTo())
                        .ownerDriverNumber(driver.getDriverNumber())
                        .ownerDriverName(driver.getFullName())
                        .build());
            }
            current = current.plusMonths(1);
        }
    }
    
    private boolean isActive(RecurringExpense expense, LocalDate date) {
        LocalDate from = expense.getEffectiveFrom();
        LocalDate to = expense.getEffectiveTo();
        
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        
        return expense.isActive();
    }
    
    private BigDecimal calculateAmount(RecurringExpense expense, BigDecimal baseAmount,
            LocalDate chargeMonth, LocalDate reportStart, LocalDate reportEnd) {
        
        // If report period is partial month, prorate the amount
        LocalDate monthStart = chargeMonth.withDayOfMonth(1);
        LocalDate monthEnd = chargeMonth.withDayOfMonth(chargeMonth.lengthOfMonth());
        
        LocalDate effectiveStart = monthStart;
        LocalDate effectiveEnd = monthEnd;
        
        // Adjust for report boundaries
        if (reportStart.isAfter(monthStart)) effectiveStart = reportStart;
        if (reportEnd.isBefore(monthEnd)) effectiveEnd = reportEnd;
        
        // Adjust for expense effective dates
        if (expense.getEffectiveFrom() != null && expense.getEffectiveFrom().isAfter(effectiveStart)) {
            effectiveStart = expense.getEffectiveFrom();
        }
        if (expense.getEffectiveTo() != null && expense.getEffectiveTo().isBefore(effectiveEnd)) {
            effectiveEnd = expense.getEffectiveTo();
        }
        
        // Calculate days
        long daysInCharge = java.time.temporal.ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;
        long daysInMonth = monthEnd.getDayOfMonth();
        
        if (daysInCharge >= daysInMonth) {
            return baseAmount;
        }
        
        // Prorate
        return baseAmount
                .multiply(BigDecimal.valueOf(daysInCharge))
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
    }
    
    private void processOneTimeExpenses(Driver driver, LocalDate start, LocalDate end,
            FixedExpenseReportDTO report) {

        List<OneTimeExpense> expenses = oneTimeExpenseRepository
                .findByExpenseDateBetween(start, end);

        for (OneTimeExpense expense : expenses) {
            // ✅ FIXED: Add null check for entityType
            if (expense.getEntityType() == null) {
                log.warn("OneTimeExpense id {} has null entityType - skipping", expense.getId());
                continue;
            }

            switch (expense.getEntityType()) {
                case CAB -> processOneTimeCab(expense, driver, report);
                case SHIFT -> processOneTimeShift(expense, driver, report);
                case OWNER, DRIVER -> processOneTimeDriver(expense, driver, report);
            }
        }
    }
    
    private void processOneTimeCab(OneTimeExpense expense, Driver driver, 
            FixedExpenseReportDTO report) {
        
        Cab cab = expense.getCab();
        if (cab == null) {
            log.warn("Cab not found for one-time expense id: {}", expense.getId());
            return;
        }
        
        // ✅ BUSINESS RULE: Skip expenses for INACTIVE cabs
        if (!isCabActive(cab)) {
            log.debug("   Skipping one-time expense for cab with no active shifts: {}",
                    cab.getCabNumber());
            return;
        }
        
        List<CabShift> shifts = cabShiftRepository.findByCab(cab);
        if (shifts.isEmpty()) return;
        
        // Filter to only ACTIVE shifts
        List<CabShift> activeShifts = shifts.stream()
                .filter(this::isShiftActive)
                .toList();
        
        if (activeShifts.isEmpty()) {
            log.debug("   Skipping one-time expense - cab {} has no ACTIVE shifts", 
                    cab.getCabNumber());
            return;
        }
        
        BigDecimal splitAmount = expense.getAmount()
                .divide(BigDecimal.valueOf(activeShifts.size()), 2, RoundingMode.HALF_UP);
        
        for (CabShift shift : activeShifts) {
            if (shift.getCurrentOwner() != null && 
                shift.getCurrentOwner().getDriverNumber().equals(driver.getDriverNumber())) {
                
                report.addExpenseItem(FixedExpenseItemDTO.builder()
                        .expenseId(expense.getId())
                        .description(expense.getExpenseCategory().getCategoryName())
                        .category(expense.getExpenseCategory().getCategoryName())
                        .expenseType("ONE_TIME")
                        .assignedTo(shift.getCab().getCabNumber() + " " + shift.getShiftType())
                        .assignedToType(expense.getEntityType().toString())
                        .originalAmount(expense.getAmount())
                        .chargedAmount(splitAmount)
                        .splitNote(activeShifts.size() == 2 ? "50% of cab expense" :
                                String.format("%.0f%% of cab expense", (100.0 / activeShifts.size())))
                        .startDate(expense.getExpenseDate())
                        .ownerDriverNumber(shift.getCurrentOwner().getDriverNumber())
                        .ownerDriverName(shift.getCurrentOwner().getFullName())
                        .cabNumber(shift.getCab().getCabNumber())
                        .shiftType(shift.getShiftType().toString())
                        .build());
            }
        }
    }
    
    private void processOneTimeShift(OneTimeExpense expense, Driver driver,
            FixedExpenseReportDTO report) {
        
        // OneTimeExpense uses cab + shiftType, not direct CabShift reference
        Cab cab = expense.getCab();
        if (cab == null || expense.getShiftType() == null) {
            log.warn("Cab or ShiftType not found for one-time expense id: {}", expense.getId());
            return;
        }
        
        CabShift shift = cabShiftRepository.findByCabAndShiftType(cab, expense.getShiftType())
                .orElse(null);
        if (shift == null) {
            log.warn("CabShift not found for cab {} shift {} (expense id: {})", 
                    cab.getCabNumber(), expense.getShiftType(), expense.getId());
            return;
        }
        
        // ✅ BUSINESS RULE: Skip INACTIVE shifts or shifts on cabs with no active shifts
        if (!isCabShiftActive(shift)) {
            log.debug("   Skipping one-time shift expense - shift is INACTIVE or cab has no active shifts");
            return;
        }
        
        if (shift.getCurrentOwner() != null && 
            shift.getCurrentOwner().getDriverNumber().equals(driver.getDriverNumber())) {
            
            report.addExpenseItem(FixedExpenseItemDTO.builder()
                    .expenseId(expense.getId())
                    .description(expense.getExpenseCategory().getCategoryName())
                    .category(expense.getExpenseCategory().getCategoryName())
                    .expenseType("ONE_TIME")
                    .assignedTo(shift.getCab().getCabNumber() + " " + shift.getShiftType())
                    .assignedToType(expense.getEntityType().toString())
                    .originalAmount(expense.getAmount())
                    .chargedAmount(expense.getAmount())
                    .splitNote("Full shift expense")
                    .startDate(expense.getExpenseDate())
                    .ownerDriverNumber(shift.getCurrentOwner().getDriverNumber())
                    .ownerDriverName(shift.getCurrentOwner().getFullName())
                    .cabNumber(shift.getCab().getCabNumber())
                    .shiftType(shift.getShiftType().toString())
                    .build());
        }
    }
    
    private void processOneTimeDriver(OneTimeExpense expense, Driver driver,
            FixedExpenseReportDTO report) {
        
        Driver expenseDriver = expense.getDriver() != null ? 
                expense.getDriver() : expense.getOwner();
        
        if (expenseDriver != null && 
            expenseDriver.getDriverNumber().equals(driver.getDriverNumber())) {
            
            report.addExpenseItem(FixedExpenseItemDTO.builder()
                    .expenseId(expense.getId())
                    .description(expense.getExpenseCategory().getCategoryName())
                    .category(expense.getExpenseCategory().getCategoryName())
                    .expenseType("ONE_TIME")
                    .assignedTo(driver.getFullName())
                    .assignedToType(expense.getEntityType().toString())
                    .originalAmount(expense.getAmount())
                    .chargedAmount(expense.getAmount())
                    .splitNote("Full driver expense")
                    .startDate(expense.getExpenseDate())
                    .ownerDriverNumber(driver.getDriverNumber())
                    .ownerDriverName(driver.getFullName())
                    .build());
        }
    }
}