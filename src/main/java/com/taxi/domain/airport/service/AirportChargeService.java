package com.taxi.domain.airport.service;

import com.taxi.domain.airport.model.AirportTrip;
import com.taxi.domain.airport.repository.AirportTripRepository;
import com.taxi.domain.expense.repository.ItemRateRepository;
import com.taxi.domain.profile.model.ItemRateUnitType;
import com.taxi.domain.expense.model.ItemRate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service to calculate airport trip charges for driver shifts.
 *
 * LOGIC:
 * 1. Count airport trips for a given cab during logon/logoff hours
 * 2. Apply ItemRate (AIRPORT_TRIP unit type) to calculate charge amount
 * 3. Handle midnight-spanning shifts by querying multiple dates
 *
 * HOUR ROUNDING RULES:
 * - Logon time: floor to hour (logonTime.getHour())
 * - Logoff time:
 *   - If exact hour (minutes=0): exclude that hour (use logoffTime.getHour() - 1)
 *   - If partial hour (minutes>0): include that hour (use logoffTime.getHour())
 *
 * EXAMPLE:
 * - Logon 4:00 AM, Logoff 3:00 PM → hours 4 through 14 (inclusive)
 * - Logon 3:30 AM, Logoff 3:30 PM → hours 3 through 15 (inclusive)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AirportChargeService {

    private final AirportTripRepository airportTripRepository;
    private final ItemRateRepository itemRateRepository;

    /**
     * Result object containing airport trip count and charge details
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class AirportChargeResult {
        private int tripCount;
        private BigDecimal ratePerTrip;
        private BigDecimal totalCharge;
    }

    /**
     * Count airport trips for a driver shift based on cab number and logon/logoff times.
     *
     * @param cabNumber The cab number
     * @param logonTime The logon time (LocalDateTime)
     * @param logoffTime The logoff time (LocalDateTime)
     * @return Total airport trips during the shift hours
     */
    public int countAirportTripsForShift(String cabNumber,
                                          LocalDateTime logonTime,
                                          LocalDateTime logoffTime) {
        if (cabNumber == null || logonTime == null || logoffTime == null) {
            log.warn("countAirportTripsForShift: null parameters");
            return 0;
        }

        int startHour = logonTime.getHour();
        int endHour = logoffTime.getMinute() == 0
            ? logoffTime.getHour() - 1
            : logoffTime.getHour();

        log.debug("   Counting airport trips for cab {} from hour {} to hour {}", cabNumber, startHour, endHour);

        // Handle same-day shifts
        if (logonTime.toLocalDate().equals(logoffTime.toLocalDate())) {
            int trips = countTripsForDateAndHours(cabNumber, logonTime.toLocalDate(), startHour, endHour);
            log.debug("   Same-day shift: {} airport trips", trips);
            return trips;
        }

        // Handle midnight-spanning shifts
        int tripsStartDate = countTripsForDateAndHours(cabNumber, logonTime.toLocalDate(), startHour, 23);
        int tripsEndDate = countTripsForDateAndHours(cabNumber, logoffTime.toLocalDate(), 0, endHour);
        int total = tripsStartDate + tripsEndDate;
        log.debug("   Midnight-spanning shift: {} trips on start date + {} trips on end date = {} total",
            tripsStartDate, tripsEndDate, total);

        return total;
    }

    /**
     * Count airport trips for a specific date and hour range.
     *
     * @param cabNumber The cab number
     * @param date The trip date
     * @param startHour Start hour (inclusive)
     * @param endHour End hour (inclusive)
     * @return Total trips in the hour range
     */
    private int countTripsForDateAndHours(String cabNumber, LocalDate date, int startHour, int endHour) {
        Optional<AirportTrip> airportTrip = airportTripRepository.findByCabNumberAndTripDate(cabNumber, date);

        if (airportTrip.isEmpty()) {
            return 0;
        }

        AirportTrip trip = airportTrip.get();
        int total = 0;
        for (int hour = startHour; hour <= endHour && hour <= 23; hour++) {
            total += trip.getTripsByHour(hour);
        }

        return total;
    }

    /**
     * Get the active AIRPORT_TRIP rate for a date.
     *
     * @param checkDate The date to check
     * @return The ItemRate if found and active, null otherwise
     */
    public ItemRate getAirportTripRate(LocalDate checkDate) {
        List<ItemRate> allRates = itemRateRepository.findActiveOnDate(checkDate);
        return allRates.stream()
            .filter(r -> r.getUnitType() == ItemRateUnitType.AIRPORT_TRIP)
            .findFirst()
            .orElse(null);
    }

    /**
     * Calculate airport charge details for a driver shift.
     * Returns trip count, rate per trip, and total charge.
     *
     * @param cabNumber The cab number
     * @param logonTime The logon time
     * @param logoffTime The logoff time
     * @param checkDate The date to check for active rates
     * @return AirportChargeResult with trip count, rate, and charge
     */
    public AirportChargeResult calculateAirportChargeForShiftDetailed(String cabNumber,
                                                      LocalDateTime logonTime,
                                                      LocalDateTime logoffTime,
                                                      LocalDate checkDate) {
        // Count trips for this shift
        int tripCount = countAirportTripsForShift(cabNumber, logonTime, logoffTime);
        log.debug("   Total trips for shift: {}", tripCount);

        if (tripCount == 0) {
            log.debug("   No airport trips found - charge = $0");
            return AirportChargeResult.builder()
                .tripCount(0)
                .ratePerTrip(BigDecimal.ZERO)
                .totalCharge(BigDecimal.ZERO)
                .build();
        }

        // Find active AIRPORT_TRIP rate for this date
        List<ItemRate> allRates = itemRateRepository.findActiveOnDate(checkDate);
        log.debug("   Found {} active rates on {}", allRates.size(), checkDate);

        List<ItemRate> rates = allRates.stream()
            .filter(r -> r.getUnitType() == ItemRateUnitType.AIRPORT_TRIP)
            .toList();

        if (rates.isEmpty()) {
            log.warn("   ⚠️ No active AIRPORT_TRIP rate found for date {}", checkDate);
            return AirportChargeResult.builder()
                .tripCount(tripCount)
                .ratePerTrip(BigDecimal.ZERO)
                .totalCharge(BigDecimal.ZERO)
                .build();
        }

        // Use the first matching rate
        ItemRate rate = rates.get(0);
        BigDecimal ratePerTrip = rate.getRate();
        BigDecimal amount = BigDecimal.valueOf(tripCount).multiply(ratePerTrip);

        log.info("   ✈️ Airport charge: {} trips × ${}/trip = ${}", tripCount, ratePerTrip, amount);

        return AirportChargeResult.builder()
            .tripCount(tripCount)
            .ratePerTrip(ratePerTrip)
            .totalCharge(amount)
            .build();
    }

    /**
     * Calculate airport charge amount for a driver shift.
     * Uses ItemRate of unit type AIRPORT_TRIP (active on shift date).
     *
     * @param cabNumber The cab number
     * @param logonTime The logon time
     * @param logoffTime The logoff time
     * @param checkDate The date to check for active rates
     * @return Charge amount (BigDecimal.ZERO if no rate configured or no trips)
     */
    public BigDecimal calculateAirportChargeForShift(String cabNumber,
                                                      LocalDateTime logonTime,
                                                      LocalDateTime logoffTime,
                                                      LocalDate checkDate) {
        // Count trips for this shift
        int tripCount = countAirportTripsForShift(cabNumber, logonTime, logoffTime);
        log.debug("   Total trips for shift: {}", tripCount);

        if (tripCount == 0) {
            log.debug("   No airport trips found - charge = $0");
            return BigDecimal.ZERO;
        }

        // Find active AIRPORT_TRIP rate for this date
        List<ItemRate> allRates = itemRateRepository.findActiveOnDate(checkDate);
        log.debug("   Found {} active rates on {}", allRates.size(), checkDate);

        List<ItemRate> rates = allRates.stream()
            .filter(r -> r.getUnitType() == ItemRateUnitType.AIRPORT_TRIP)
            .toList();

        if (rates.isEmpty()) {
            log.warn("   ⚠️ No active AIRPORT_TRIP rate found for date {}", checkDate);
            return BigDecimal.ZERO;
        }

        // Use the first matching rate (assumes only one active rate per unit type)
        ItemRate rate = rates.get(0);

        BigDecimal amount = BigDecimal.valueOf(tripCount).multiply(rate.getRate());
        log.info("   ✈️ Airport charge: {} trips × ${}/trip = ${}", tripCount, rate.getRate(), amount);

        return amount;
    }
}
