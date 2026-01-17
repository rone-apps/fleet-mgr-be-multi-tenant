package com.taxi.web.controller;

import com.taxi.domain.airport.model.AirportTrip;
import com.taxi.domain.airport.repository.AirportTripRepository;
import com.taxi.domain.payment.model.CreditCardTransaction;
import com.taxi.domain.payment.repository.CreditCardTransactionRepository;
import com.taxi.domain.mileage.model.MileageRecord;
import com.taxi.domain.mileage.repository.MileageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/data-view")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
@Slf4j
public class DataViewController {

    private final CreditCardTransactionRepository creditCardRepository;
    private final MileageRecordRepository mileageRepository;
    private final AirportTripRepository airportTripRepository;

    // ==================== Credit Card Transactions ====================

    @GetMapping("/credit-card-transactions")
    public ResponseEntity<Page<CreditCardTransaction>> getCreditCardTransactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String cabNumber,
            @RequestParam(required = false) String driverNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        log.info("Fetching credit card transactions: {} to {}, cab={}, driver={}", 
                 startDate, endDate, cabNumber, driverNumber);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate", "transactionTime"));
        
        Page<CreditCardTransaction> result;
        
        if (cabNumber != null && !cabNumber.isEmpty() && driverNumber != null && !driverNumber.isEmpty()) {
            result = creditCardRepository.findByTransactionDateBetweenAndCabNumberAndDriverNumber(
                startDate, endDate, cabNumber, driverNumber, pageable);
        } else if (cabNumber != null && !cabNumber.isEmpty()) {
            result = creditCardRepository.findByTransactionDateBetweenAndCabNumber(
                startDate, endDate, cabNumber, pageable);
        } else if (driverNumber != null && !driverNumber.isEmpty()) {
            result = creditCardRepository.findByTransactionDateBetweenAndDriverNumber(
                startDate, endDate, driverNumber, pageable);
        } else {
            result = creditCardRepository.findByTransactionDateBetween(startDate, endDate, pageable);
        }

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/credit-card-transactions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateCreditCardTransaction(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates) {

        log.info("Updating credit card transaction {}: {}", id, updates);

        Optional<CreditCardTransaction> optionalTransaction = creditCardRepository.findById(id);
        if (optionalTransaction.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CreditCardTransaction transaction = optionalTransaction.get();
        
        // Only allow updating driverNumber
        if (updates.containsKey("driverNumber")) {
            transaction.setDriverNumber(updates.get("driverNumber"));
        }

        creditCardRepository.save(transaction);
        return ResponseEntity.ok(transaction);
    }

    // ==================== Mileage Records ====================

    @GetMapping("/mileage-records")
    public ResponseEntity<Page<MileageRecord>> getMileageRecords(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String cabNumber,
            @RequestParam(required = false) String driverNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        log.info("Fetching mileage records: {} to {}, cab={}, driver={}", 
                 startDate, endDate, cabNumber, driverNumber);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "logonTime"));
        
        Page<MileageRecord> result;
        
        if (cabNumber != null && !cabNumber.isEmpty() && driverNumber != null && !driverNumber.isEmpty()) {
            result = mileageRepository.findByLogonDateBetweenAndCabNumberAndDriverNumber(
                startDate, endDate, cabNumber, driverNumber, pageable);
        } else if (cabNumber != null && !cabNumber.isEmpty()) {
            result = mileageRepository.findByLogonDateBetweenAndCabNumber(
                startDate, endDate, cabNumber, pageable);
        } else if (driverNumber != null && !driverNumber.isEmpty()) {
            result = mileageRepository.findByLogonDateBetweenAndDriverNumber(
                startDate, endDate, driverNumber, pageable);
        } else {
            result = mileageRepository.findByLogonDateBetween(startDate, endDate, pageable);
        }

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/mileage-records/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateMileageRecord(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates) {

        log.info("Updating mileage record {}: {}", id, updates);

        Optional<MileageRecord> optionalRecord = mileageRepository.findById(id);
        if (optionalRecord.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MileageRecord record = optionalRecord.get();
        
        // Only allow updating driverNumber
        if (updates.containsKey("driverNumber")) {
            record.setDriverNumber(updates.get("driverNumber"));
        }

        mileageRepository.save(record);
        return ResponseEntity.ok(record);
    }

    // ==================== Airport Trips ====================

    @GetMapping("/airport-trips")
    public ResponseEntity<Page<AirportTrip>> getAirportTrips(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String cabNumber,
            @RequestParam(required = false) String driverNumber,
            @RequestParam(required = false) String shift,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        log.info("Fetching airport trips: {} to {}, cab={}, driver={}, shift={}", 
                 startDate, endDate, cabNumber, driverNumber, shift);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "tripDate"));
        
        Page<AirportTrip> result;
        
        // Build query based on filters
        if (cabNumber != null && !cabNumber.isEmpty()) {
            if (shift != null && !shift.isEmpty()) {
                result = airportTripRepository.findByTripDateBetweenAndCabNumberAndShift(
                    startDate, endDate, cabNumber, shift, pageable);
            } else {
                result = airportTripRepository.findByTripDateBetweenAndCabNumber(
                    startDate, endDate, cabNumber, pageable);
            }
        } else if (shift != null && !shift.isEmpty()) {
            result = airportTripRepository.findByTripDateBetweenAndShift(
                startDate, endDate, shift, pageable);
        } else {
            result = airportTripRepository.findByTripDateBetweenOrderByTripDateDesc(startDate, endDate, pageable);
        }

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/airport-trips/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateAirportTrip(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates) {

        log.info("Updating airport trip {}: {}", id, updates);

        Optional<AirportTrip> optionalTrip = airportTripRepository.findById(id);
        if (optionalTrip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AirportTrip trip = optionalTrip.get();
        
        // Only allow updating driverNumber and shift
        if (updates.containsKey("driverNumber")) {
            trip.setDriverNumber(updates.get("driverNumber"));
        }
        if (updates.containsKey("shift")) {
            String newShift = updates.get("shift");
            if ("DAY".equals(newShift) || "NIGHT".equals(newShift)) {
                trip.setShift(newShift);
            }
        }

        airportTripRepository.save(trip);
        return ResponseEntity.ok(trip);
    }
}
