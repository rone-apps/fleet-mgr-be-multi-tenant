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
     * Get the active generic AIRPORT_TRIP rate for a date (no attribute linkage).
     */
    public ItemRate getAirportTripRate(LocalDate checkDate) {
        // First try: rate with no attribute_type_id (generic rate)
        List<ItemRate> genericRates = itemRateRepository.findActiveByUnitTypeNoAttribute(
                ItemRateUnitType.AIRPORT_TRIP, checkDate);
        if (!genericRates.isEmpty()) {
            return genericRates.get(0);
        }

        // Last resort: any AIRPORT_TRIP rate
        List<ItemRate> allRates = itemRateRepository.findActiveOnDate(checkDate);
        return allRates.stream()
            .filter(r -> r.getUnitType() == ItemRateUnitType.AIRPORT_TRIP)
            .findFirst()
            .orElse(null);
    }

    /**
     * Get the active AIRPORT_TRIP rate for a specific attribute type on a date.
     * 1. Try attribute-specific rate (e.g., TRANSPONDER=$7.00, AIRPORT_PLATE=$6.50)
     * 2. Fall back to generic (no-attribute) AIRPORT_TRIP rate
     */
    public ItemRate getAirportTripRateForAttribute(Long attributeTypeId, LocalDate checkDate) {
        if (attributeTypeId != null) {
            List<ItemRate> attributeRates = itemRateRepository.findActiveByUnitTypeAndAttributeType(
                ItemRateUnitType.AIRPORT_TRIP, attributeTypeId, checkDate);
            if (!attributeRates.isEmpty()) {
                ItemRate rate = attributeRates.get(0);
                log.debug("Using attribute-specific AIRPORT_TRIP rate: {} (${}) for attributeTypeId={}",
                        rate.getName(), rate.getRate(), attributeTypeId);
                return rate;
            }
            log.debug("No attribute-specific AIRPORT_TRIP rate for attributeTypeId={}, falling back to generic", attributeTypeId);
        }
        // Fallback to generic rate (no attribute)
        return getAirportTripRate(checkDate);
    }

    /**
     * Calculate airport charge details for a driver shift.
     * Returns trip count, rate per trip, and total charge.
     * Uses the first generic AIRPORT_TRIP rate found.
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
        return calculateAirportChargeForShiftDetailed(cabNumber, logonTime, logoffTime, checkDate, null);
    }

    /**
     * Calculate airport charge details for a driver shift with attribute-specific rate.
     * If attributeTypeId is provided, uses the rate linked to that attribute.
     * Falls back to generic AIRPORT_TRIP rate if no attribute-specific rate exists.
     *
     * @param cabNumber The cab number
     * @param logonTime The logon time
     * @param logoffTime The logoff time
     * @param checkDate The date to check for active rates
     * @param attributeTypeId Optional attribute type ID for attribute-specific rate lookup
     * @return AirportChargeResult with trip count, rate, and charge
     */
    public AirportChargeResult calculateAirportChargeForShiftDetailed(String cabNumber,
                                                      LocalDateTime logonTime,
                                                      LocalDateTime logoffTime,
                                                      LocalDate checkDate,
                                                      Long attributeTypeId) {
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

        // Find rate — use attribute-specific rate if attributeTypeId is provided
        ItemRate rate = getAirportTripRateForAttribute(attributeTypeId, checkDate);

        if (rate == null) {
            log.warn("   No active AIRPORT_TRIP rate found for date {} (attributeTypeId={})", checkDate, attributeTypeId);
            return AirportChargeResult.builder()
                .tripCount(tripCount)
                .ratePerTrip(BigDecimal.ZERO)
                .totalCharge(BigDecimal.ZERO)
                .build();
        }

        BigDecimal ratePerTrip = rate.getRate();
        BigDecimal amount = BigDecimal.valueOf(tripCount).multiply(ratePerTrip);

        log.info("   Airport charge: {} trips x ${}/trip = ${} (rate: {})", tripCount, ratePerTrip, amount, rate.getName());

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
        return calculateAirportChargeForShiftDetailed(cabNumber, logonTime, logoffTime, checkDate, null)
            .getTotalCharge();
    }

    /**
     * Calculate airport charge amount for a driver shift with attribute-specific rate.
     */
    public BigDecimal calculateAirportChargeForShift(String cabNumber,
                                                      LocalDateTime logonTime,
                                                      LocalDateTime logoffTime,
                                                      LocalDate checkDate,
                                                      Long attributeTypeId) {
        return calculateAirportChargeForShiftDetailed(cabNumber, logonTime, logoffTime, checkDate, attributeTypeId)
            .getTotalCharge();
    }
}
