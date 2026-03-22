package com.taxi.domain.drivertrip.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.drivertrip.dto.DriverTripImportResult;
import com.taxi.domain.drivertrip.model.DriverTrip;
import com.taxi.domain.drivertrip.repository.DriverTripRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
public class TaxiCallerDriverTripImportService {

    private static final Logger logger = LoggerFactory.getLogger(TaxiCallerDriverTripImportService.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    private DriverTripRepository driverTripRepository;

    @Autowired
    private CabRepository cabRepository;

    @Autowired
    private DriverRepository driverRepository;

    /**
     * Import driver job reports from TaxiCaller and save to driver_trips table.
     *
     * TaxiCaller JSON format:
     * {
     *   "date": "21/03/2026",
     *   "drop_off": "...",
     *   "start": "21/03/2026 09:10",
     *   "pick-up": "...",
     *   "vehicle_num": "35",
     *   "driver": "MINHAS, BALJINDER",
     *   "passenger": "unknown",
     *   "job_id": "311876975",
     *   "end": "21/03/2026 09:40",
     *   "company": "69244",
     *   "tariff": "40.25",
     *   "driver_username": 782161,
     *   "account": ""
     * }
     */
    @Transactional
    public DriverTripImportResult importDriverJobReports(JSONArray trips) {
        DriverTripImportResult result = new DriverTripImportResult();

        try {
            if (trips == null || trips.length() == 0) {
                logger.warn("No driver trip data to import");
                return result;
            }

            result.setTotalRecords(trips.length());
            logger.info("Processing {} driver job reports from TaxiCaller", trips.length());

            for (int i = 0; i < trips.length(); i++) {
                try {
                    JSONObject tripData = trips.getJSONObject(i);
                    String jobId = tripData.optString("job_id", null);

                    if (jobId == null || jobId.trim().isEmpty()) {
                        result.incrementError("Missing job_id at index " + i);
                        logger.warn("Skipping record at index {} - missing job_id", i);
                        continue;
                    }

                    DriverTrip trip = transformToDriverTrip(tripData);

                    // Check for duplicate by unique constraint
                    Optional<DriverTrip> existing = driverTripRepository.findByUniqueConstraint(
                            trip.getJobCode(),
                            trip.getDriver() != null ? trip.getDriver().getId() : null,
                            trip.getCab() != null ? trip.getCab().getId() : null,
                            trip.getTripDate()
                    );

                    if (existing.isPresent()) {
                        logger.debug("Duplicate driver trip, job_id: {}", jobId);
                        result.incrementDuplicate(jobId);
                    } else {
                        driverTripRepository.save(trip);
                        result.incrementSuccess();
                    }

                    if ((i + 1) % 50 == 0) {
                        logger.info("Processed {} / {} driver trip records", i + 1, trips.length());
                    }

                } catch (Exception e) {
                    String jobId = "unknown";
                    try {
                        jobId = trips.getJSONObject(i).optString("job_id", "unknown");
                    } catch (Exception ex) {
                        // ignore
                    }

                    String errorMsg = String.format("Error processing driver trip at index %d (job_id: %s): %s",
                            i, jobId, e.getMessage());
                    result.incrementError(errorMsg);
                    logger.error(errorMsg, e);
                }
            }

            logger.info("Driver trip import completed: {}", result.toString());

        } catch (Exception e) {
            logger.error("Fatal error during driver trip import", e);
            result.incrementError("Fatal error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Transform TaxiCaller JSON to DriverTrip entity
     */
    private DriverTrip transformToDriverTrip(JSONObject tripData) {
        DriverTrip trip = DriverTrip.builder().build();

        // Job ID
        trip.setJobCode(tripData.optString("job_id"));

        // Driver username (can be int or string in JSON)
        String driverUsername = String.valueOf(tripData.opt("driver_username"));
        trip.setDriverUsername(driverUsername);

        // Driver name
        String driverName = tripData.optString("driver", "");
        trip.setDriverName(driverName);

        // Find Driver entity by driver_username (stored as driverNumber)
        if (driverUsername != null && !driverUsername.isEmpty() && !"null".equals(driverUsername)) {
            try {
                Optional<Driver> driverOpt = driverRepository.findByDriverNumber(driverUsername);
                driverOpt.ifPresent(trip::setDriver);
                if (driverOpt.isEmpty()) {
                    logger.debug("Driver not found for username: {}", driverUsername);
                }
            } catch (Exception e) {
                logger.warn("Error finding driver for username {}: {}", driverUsername, e.getMessage());
            }
        }

        // Find Cab by vehicle_num
        String vehicleNum = tripData.optString("vehicle_num", "");
        if (!vehicleNum.isEmpty()) {
            try {
                Optional<Cab> cabOpt = cabRepository.findByCabNumber(vehicleNum.trim());
                cabOpt.ifPresent(trip::setCab);
                if (cabOpt.isEmpty()) {
                    logger.debug("Cab not found for vehicle_num: {}", vehicleNum);
                }
            } catch (Exception e) {
                logger.warn("Error finding cab for vehicle_num {}: {}", vehicleNum, e.getMessage());
            }
        }

        // Parse dates
        String dateStr = tripData.optString("date", "");
        trip.setTripDate(parseDate(dateStr));

        // Parse start/end times
        String startStr = tripData.optString("start", "");
        String endStr = tripData.optString("end", "");
        trip.setStartTime(parseTime(startStr));
        trip.setEndTime(parseTime(endStr));

        // Addresses
        trip.setPickupAddress(tripData.optString("pick-up", ""));
        trip.setDropoffAddress(tripData.optString("drop_off", ""));

        // Passenger
        trip.setPassengerName(tripData.optString("passenger", ""));

        // Account
        trip.setAccountNumber(tripData.optString("account", ""));

        // Company ID
        trip.setCompanyId(tripData.optString("company", ""));

        // Fare (tariff field)
        trip.setFareAmount(parseBigDecimal(tripData.optString("tariff", "0")));
        trip.setTipAmount(BigDecimal.ZERO);

        return trip;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.error("Failed to parse date: {}", dateStr, e);
            return null;
        }
    }

    private LocalTime parseTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr.trim(), DATETIME_FORMATTER);
            return dateTime.toLocalTime();
        } catch (DateTimeParseException e) {
            logger.error("Failed to parse time from: {}", dateTimeStr, e);
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            String cleaned = value.trim().replaceAll("[^0-9.-]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            logger.warn("Could not parse decimal value: {}", value);
            return BigDecimal.ZERO;
        }
    }
}
