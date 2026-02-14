package com.taxi.domain.expense.service;

import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.expense.model.OneTimeExpense;
import com.taxi.domain.expense.model.RecurringExpense;
import com.taxi.domain.expense.repository.OneTimeExpenseRepository;
import com.taxi.domain.expense.repository.RecurringExpenseRepository;
import com.taxi.web.dto.expense.AnalyticsDTO;
import com.taxi.web.dto.expense.DriverPerformanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final OneTimeExpenseRepository oneTimeExpenseRepository;
    private final DriverRepository driverRepository;

    /**
     * Generate analytics dashboard metrics for a date period
     */
    public AnalyticsDTO generateAnalytics(LocalDate from, LocalDate to) {
        log.info("Generating analytics for period {} to {}", from, to);

        BigDecimal totalRecurringExpenses = recurringExpenseRepository.findEffectiveBetween(from, to).stream()
            .map(e -> e.calculateAmountForDateRange(from, to))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOneTimeExpenses = oneTimeExpenseRepository.findBetween(from, to).stream()
            .map(OneTimeExpense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = totalRecurringExpenses.add(totalOneTimeExpenses);

        long activeDriverCount = driverRepository.count(); // TODO: Filter by active status
        long activeOwnerCount = driverRepository.findAll().stream()
            .filter(d -> Boolean.TRUE.equals(d.getIsOwner()))
            .count();

        return AnalyticsDTO.builder()
            .periodFrom(from)
            .periodTo(to)
            .totalRecurringExpenses(totalRecurringExpenses)
            .totalOneTimeExpenses(totalOneTimeExpenses)
            .totalExpenses(totalExpenses)
            .activeDriverCount(activeDriverCount)
            .activeOwnerCount(activeOwnerCount)
            .build();
    }

    /**
     * Generate driver performance metrics
     */
    public List<DriverPerformanceDTO> generateDriverPerformance(LocalDate from, LocalDate to) {
        log.info("Generating driver performance metrics for period {} to {}", from, to);

        List<Driver> drivers = driverRepository.findAll();

        return drivers.stream()
            .map(driver -> generateDriverMetrics(driver, from, to))
            .filter(metrics -> metrics.getTotalExpenses().compareTo(BigDecimal.ZERO) > 0 ||
                             metrics.getTotalRevenues().compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.toList());
    }

    private DriverPerformanceDTO generateDriverMetrics(Driver driver, LocalDate from, LocalDate to) {
        // Get recurring expenses for driver
        BigDecimal recurringExpenses = recurringExpenseRepository
            .findByApplicationTypeAndSpecificDriverIdBetween(
                com.taxi.domain.expense.model.ApplicationType.SPECIFIC_OWNER_DRIVER,
                driver.getId(), from, to).stream()
            .map(e -> e.calculateAmountForDateRange(from, to))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get one-time expenses for driver
        BigDecimal oneTimeExpenses = oneTimeExpenseRepository.findForEntityBetween(
            OneTimeExpense.EntityType.DRIVER, driver.getId(), from, to).stream()
            .map(OneTimeExpense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = recurringExpenses.add(oneTimeExpenses);

        // TODO: Add revenues once revenue repository is integrated
        BigDecimal totalRevenues = BigDecimal.ZERO;

        BigDecimal netAmount = totalRevenues.subtract(totalExpenses);

        return DriverPerformanceDTO.builder()
            .driverId(driver.getId())
            .driverName(driver.getFullName())
            .driverNumber(driver.getDriverNumber())
            .isOwner(Boolean.TRUE.equals(driver.getIsOwner()))
            .totalRevenues(totalRevenues)
            .totalExpenses(totalExpenses)
            .netAmount(netAmount)
            .profitMargin(totalRevenues.compareTo(BigDecimal.ZERO) > 0
                ? netAmount.divide(totalRevenues, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal(100))
                : BigDecimal.ZERO)
            .build();
    }
}
