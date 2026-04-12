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

import com.taxi.domain.drivertrip.repository.DriverTripRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final OneTimeExpenseRepository oneTimeExpenseRepository;
    private final DriverRepository driverRepository;
    private final DriverTripRepository driverTripRepository;

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
            .findByApplicationTypeAndSpecificPersonIdBetween(
                com.taxi.domain.expense.model.ApplicationType.SPECIFIC_PERSON,
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

    /**
     * Get trip analytics data for heatmap and time charts
     * Aggregates trips by pickup address, hour of day, and day of week
     */
    public Map<String, Object> getTripAnalytics(
            LocalDate startDate,
            LocalDate endDate,
            String accountNumber,
            Long driverId,
            Integer startHour,
            Integer endHour) {

        Map<String, Object> result = new HashMap<>();

        // Get top pickup addresses
        List<Object[]> addressResults = driverTripRepository.findTopPickupAddresses(
            startDate, endDate, accountNumber, driverId, startHour, endHour);

        List<Map<String, Object>> pickupAddresses = addressResults.stream()
            .map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("address", row[0]);
                map.put("count", ((Number) row[1]).longValue());
                return map;
            })
            .collect(Collectors.toList());

        result.put("pickupAddresses", pickupAddresses);

        // Get trips by hour of day
        List<Object[]> hourResults = driverTripRepository.findTripsByHour(
            startDate, endDate, accountNumber, driverId);

        List<Map<String, Object>> byHour = hourResults.stream()
            .map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("hour", ((Number) row[0]).intValue());
                map.put("count", ((Number) row[1]).longValue());
                return map;
            })
            .collect(Collectors.toList());

        result.put("byHour", byHour);

        // Get trips by day of week (1=Sun ... 7=Sat)
        List<Object[]> dayResults = driverTripRepository.findTripsByDayOfWeek(
            startDate, endDate, accountNumber, driverId);

        List<Map<String, Object>> byDayOfWeek = dayResults.stream()
            .map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("dayOfWeek", ((Number) row[0]).intValue());
                map.put("count", ((Number) row[1]).longValue());
                return map;
            })
            .collect(Collectors.toList());

        result.put("byDayOfWeek", byDayOfWeek);

        // Calculate total trips from hour data
        long totalTrips = byHour.stream()
            .mapToLong(h -> ((Number) h.get("count")).longValue())
            .sum();

        result.put("totalTrips", totalTrips);

        log.info("Trip analytics generated: {} pickup addresses, {} hours, {} days, {} total trips",
            pickupAddresses.size(), byHour.size(), byDayOfWeek.size(), totalTrips);

        return result;
    }

    /**
     * Get distinct account numbers from driver trips
     */
    public List<String> getDistinctAccountNumbers() {
        return driverTripRepository.findDistinctAccountNumbers();
    }
}
