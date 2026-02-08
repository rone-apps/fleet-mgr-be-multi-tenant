package com.taxi.domain.cab.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.cab.model.ShareType;
import com.taxi.domain.cab.model.CabShiftType;
import com.taxi.domain.cab.model.CabOwnerHistory;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.cab.repository.CabOwnerHistoryRepository;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.ShiftType;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.domain.shift.service.ShiftStatusService;
import com.taxi.web.dto.cab.CabDTO;
import com.taxi.web.dto.cab.CreateCabRequest;
import com.taxi.web.dto.cab.UpdateCabRequest;
import com.taxi.web.dto.cab.CabOwnerHistoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing cabs/vehicles
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CabService {

    private final CabRepository cabRepository;
    private final DriverRepository driverRepository;
    private final CabOwnerHistoryRepository cabOwnerHistoryRepository;
    private final CabShiftRepository cabShiftRepository;
    private final ShiftStatusService shiftStatusService;

    /**
     * Get all cabs
     */
    @Transactional(readOnly = true)
    public List<CabDTO> getAllCabs() {
        log.info("Getting all cabs");
        return cabRepository.findAll().stream()
                .map(CabDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * DEPRECATED: Get active cabs only
     *
     * Status is now managed at shift level, not cab level.
     * This method is kept for backward compatibility but will return all cabs.
     * Consider using shift-level queries instead.
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    @Transactional(readOnly = true)
    public List<CabDTO> getActiveCabs() {
        log.warn("DEPRECATED: getActiveCabs() is deprecated. Status is now managed at shift level.");
        // Return all cabs since status filtering no longer applies at cab level
        return getAllCabs();
    }

    /**
     * DEPRECATED: Get cabs by status
     *
     * Status is now managed at shift level, not cab level.
     * This method is kept for backward compatibility but will return all cabs.
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    @Transactional(readOnly = true)
    public List<CabDTO> getCabsByStatus(Object status) {
        log.warn("DEPRECATED: getCabsByStatus() is deprecated. Status is now managed at shift level.");
        // Return all cabs since status filtering no longer applies at cab level
        return getAllCabs();
    }

    /**
     * DEPRECATED: Get cabs by type
     *
     * Cab type is now managed at shift level, not cab level.
     * Each shift of the same cab can have different types.
     * This method is kept for backward compatibility but will return all cabs.
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    @Transactional(readOnly = true)
    public List<CabDTO> getCabsByType(CabType type) {
        log.warn("DEPRECATED: getCabsByType() is deprecated. Cab type is now managed at shift level. " +
                "Use shift-level queries for filtering by type.");
        // Return all cabs since type filtering no longer applies at cab level
        return getAllCabs();
    }

    /**
     * DEPRECATED: Get cabs with airport license
     *
     * Airport license is now managed at shift level, not cab level.
     * Each shift can have independent airport license settings.
     * This method is kept for backward compatibility but will return all cabs.
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    @Transactional(readOnly = true)
    public List<CabDTO> getCabsWithAirportLicense() {
        log.warn("DEPRECATED: getCabsWithAirportLicense() is deprecated. " +
                "Airport license is now managed at shift level. Use shift-level queries instead.");
        // Return all cabs since airport license filtering no longer applies at cab level
        return getAllCabs();
    }

    /**
     * Get cab by ID
     */
    @Transactional(readOnly = true)
    public CabDTO getCabById(Long id) {
        log.info("Getting cab by ID: {}", id);
        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + id));
        return CabDTO.fromEntity(cab);
    }

    /**
     * Get cab by cab number
     */
    @Transactional(readOnly = true)
    public CabDTO getCabByCabNumber(String cabNumber) {
        log.info("Getting cab by cab number: {}", cabNumber);
        Cab cab = cabRepository.findByCabNumber(cabNumber)
                .orElseThrow(() -> new RuntimeException("Cab not found with number: " + cabNumber));
        return CabDTO.fromEntity(cab);
    }

    /**
     * Create a new cab
     *
     * REFACTORED: This method now auto-creates DAY and NIGHT shifts for the new cab.
     * Attributes are now assigned at the shift level, not the cab level.
     * Each shift can have independent attributes and status tracking.
     */
    @Transactional
    public CabDTO createCab(CreateCabRequest request) {
        log.info("Creating new cab with registration: {}", request.getRegistrationNumber());

        // Validate registration number doesn't exist
        if (cabRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new RuntimeException("Registration number already exists: " + request.getRegistrationNumber());
        }

        // Validate cab type (attributes now at shift level, but validate for assignment)
        CabType cabType = null;
        if (request.getCabType() != null) {
            try {
                cabType = CabType.valueOf(request.getCabType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid cab type: " + request.getCabType());
            }
        }

        // Validate share type if provided
        ShareType shareType = null;
        if (request.getShareType() != null && !request.getShareType().isEmpty()) {
            try {
                shareType = ShareType.valueOf(request.getShareType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid share type: " + request.getShareType());
            }
        }

        // Get owner driver if specified
        Driver ownerDriver = null;
        if (request.getOwnerDriverId() != null) {
            ownerDriver = driverRepository.findById(request.getOwnerDriverId())
                    .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + request.getOwnerDriverId()));

            // Validate that driver is marked as owner
            if (!Boolean.TRUE.equals(ownerDriver.getIsOwner())) {
                throw new RuntimeException("Driver must be marked as owner to own a cab");
            }
        }

        // Generate cab number
        String cabNumber = generateCabNumber();

        // Create cab (simplified - only vehicle information)
        Cab cab = Cab.builder()
                .cabNumber(cabNumber)
                .registrationNumber(request.getRegistrationNumber())
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .color(request.getColor())
                .ownerDriver(ownerDriver)
                .notes(request.getNotes())
                .build();

        cab = cabRepository.save(cab);
        log.info("Cab created with number: {}", cab.getCabNumber());

        // ====================================================================
        // AUTO-CREATE SHIFTS for the new cab (Phase 1 refactoring)
        // Each cab automatically gets 2 shifts: DAY and NIGHT
        // Attributes and status are now managed at the shift level
        // ====================================================================

        // Create DAY shift
        CabShift dayShift = CabShift.builder()
                .cab(cab)
                .shiftType(ShiftType.DAY)
                .startTime("06:00")
                .endTime("18:00")
                .currentOwner(ownerDriver)  // Use cab owner as shift owner (can be transferred independently)
                .status(CabShift.ShiftStatus.ACTIVE)
                // Attributes assigned at shift level
                .cabType(cabType)
                .shareType(shareType)
                .hasAirportLicense(request.getHasAirportLicense() != null ? request.getHasAirportLicense() : false)
                .airportLicenseNumber(request.getAirportLicenseNumber())
                .airportLicenseExpiry(request.getAirportLicenseExpiry())
                .build();

        CabShift savedDayShift = cabShiftRepository.save(dayShift);
        log.info("Auto-created DAY shift for cab {} - shift ID: {}", cab.getCabNumber(), savedDayShift.getId());

        // Create NIGHT shift
        CabShift nightShift = CabShift.builder()
                .cab(cab)
                .shiftType(ShiftType.NIGHT)
                .startTime("18:00")
                .endTime("06:00")
                .currentOwner(ownerDriver)  // Use cab owner as shift owner (can be transferred independently)
                .status(CabShift.ShiftStatus.ACTIVE)
                // Attributes assigned at shift level
                .cabType(cabType)
                .shareType(shareType)
                .hasAirportLicense(request.getHasAirportLicense() != null ? request.getHasAirportLicense() : false)
                .airportLicenseNumber(request.getAirportLicenseNumber())
                .airportLicenseExpiry(request.getAirportLicenseExpiry())
                .build();

        CabShift savedNightShift = cabShiftRepository.save(nightShift);
        log.info("Auto-created NIGHT shift for cab {} - shift ID: {}", cab.getCabNumber(), savedNightShift.getId());

        // Create initial status history for both shifts (active by default)
        shiftStatusService.activateShift(
            savedDayShift.getId(),
            LocalDate.now(),
            "SYSTEM",
            "Initial creation with cab"
        );
        log.debug("Created initial status history for DAY shift {}", savedDayShift.getId());

        shiftStatusService.activateShift(
            savedNightShift.getId(),
            LocalDate.now(),
            "SYSTEM",
            "Initial creation with cab"
        );
        log.debug("Created initial status history for NIGHT shift {}", savedNightShift.getId());

        // Create owner history record if owner assigned
        if (ownerDriver != null) {
            createOwnerHistoryRecord(cab, ownerDriver, LocalDate.now(), "Initial owner assignment");
        }

        return CabDTO.fromEntity(cab);
    }

    /**
     * Update an existing cab (vehicle information only)
     *
     * REFACTORED: Cab-level attributes (cabType, shareType, airport license, status)
     * are now managed at the shift level. Use CabShiftAttributeService to update
     * shift-level attributes.
     *
     * This method only updates vehicle identification and ownership information.
     */
    @Transactional
    public CabDTO updateCab(Long id, UpdateCabRequest request) {
        log.info("Updating cab with ID: {}", id);

        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + id));

        // Update vehicle identification fields if provided
        if (request.getRegistrationNumber() != null) {
            // Check if registration is being changed and if new registration already exists
            if (!request.getRegistrationNumber().equals(cab.getRegistrationNumber()) &&
                cabRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
                throw new RuntimeException("Registration number already exists: " + request.getRegistrationNumber());
            }
            cab.setRegistrationNumber(request.getRegistrationNumber());
        }

        if (request.getMake() != null) {
            cab.setMake(request.getMake());
        }

        if (request.getModel() != null) {
            cab.setModel(request.getModel());
        }

        if (request.getYear() != null) {
            cab.setYear(request.getYear());
        }

        if (request.getColor() != null) {
            cab.setColor(request.getColor());
        }

        if (request.getNotes() != null) {
            cab.setNotes(request.getNotes());
        }

        // Handle owner change
        if (request.getOwnerDriverId() != null) {
            Driver currentOwner = cab.getOwnerDriver();
            Long newOwnerId = request.getOwnerDriverId();

            // Check if owner is actually changing
            if (currentOwner == null || !currentOwner.getId().equals(newOwnerId)) {
                // Get new owner
                Driver newOwner = driverRepository.findById(newOwnerId)
                        .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + newOwnerId));

                // Validate that driver is marked as owner
                if (!Boolean.TRUE.equals(newOwner.getIsOwner())) {
                    throw new RuntimeException("Driver must be marked as owner to own a cab");
                }

                // Close previous owner history if exists
                if (currentOwner != null) {
                    closeOwnerHistory(cab, LocalDate.now());
                }

                // Set new owner
                cab.setOwnerDriver(newOwner);

                // Create new owner history record
                createOwnerHistoryRecord(cab, newOwner, LocalDate.now(), "Owner changed");
                log.info("Cab {} owner changed to driver {}", cab.getCabNumber(), newOwner.getDriverNumber());
            }
        }

        // NOTE: Attribute fields (cabType, shareType, airport license) and status
        // are now managed at the shift level. If request contains these fields,
        // they are ignored. API clients should use shift-level endpoints instead.
        if (request.getCabType() != null || request.getShareType() != null ||
                request.getCabShiftType() != null || request.getHasAirportLicense() != null ||
                request.getAirportLicenseNumber() != null || request.getAirportLicenseExpiry() != null ||
                request.getStatus() != null) {
            log.warn("Cab update request contains shift-level attributes which are now ignored. " +
                    "Use CabShiftAttributeController to update shift attributes.");
        }

        cab = cabRepository.save(cab);
        log.info("Cab updated: {}", cab.getCabNumber());

        return CabDTO.fromEntity(cab);
    }

    /**
     * Create owner history record
     */
    private void createOwnerHistoryRecord(Cab cab, Driver owner, LocalDate startDate, String notes) {
        CabOwnerHistory history = CabOwnerHistory.builder()
                .cab(cab)
                .ownerDriver(owner)
                .startDate(startDate)
                .endDate(null)  // Current owner
                .notes(notes)
                .build();
        
        cabOwnerHistoryRepository.save(history);
        log.info("Created owner history record for cab {} with owner {}", cab.getCabNumber(), owner.getDriverNumber());
    }

    /**
     * Close current owner history record
     */
    private void closeOwnerHistory(Cab cab, LocalDate endDate) {
        cabOwnerHistoryRepository.findCurrentOwner(cab).ifPresent(history -> {
            history.setEndDate(endDate);
            cabOwnerHistoryRepository.save(history);
            log.info("Closed owner history record for cab {}", cab.getCabNumber());
        });
    }

    /**
     * DEPRECATED: Cab-level status management has been replaced with shift-level status tracking
     *
     * Status is now managed per shift (DAY/NIGHT) via ShiftStatusService.
     * Use ShiftStatusController endpoints to activate/deactivate individual shifts.
     *
     * This method is kept for backward compatibility but will log a deprecation warning.
     *
     * @param id The cab ID
     * @return The cab DTO (status field will be ignored)
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    @Transactional
    public CabDTO setMaintenance(Long id) {
        log.warn("DEPRECATED: setMaintenance() is deprecated. Status is now managed at shift level. " +
                "Use ShiftStatusService to manage shift status.");

        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + id));

        // No-op since cab status has been removed
        log.info("Cab {} status management requested but is deprecated", cab.getCabNumber());

        return CabDTO.fromEntity(cab);
    }

    /**
     * DEPRECATED: Cab-level status management has been replaced with shift-level status tracking
     *
     * Status is now managed per shift (DAY/NIGHT) via ShiftStatusService.
     * Use ShiftStatusController endpoints to activate/deactivate individual shifts.
     *
     * This method is kept for backward compatibility but will log a deprecation warning.
     *
     * @param id The cab ID
     * @return The cab DTO (status field will be ignored)
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    @Transactional
    public CabDTO activate(Long id) {
        log.warn("DEPRECATED: activate() is deprecated. Status is now managed at shift level. " +
                "Use ShiftStatusService to manage shift status.");

        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + id));

        // No-op since cab status has been removed
        log.info("Cab {} activation requested but is deprecated", cab.getCabNumber());

        return CabDTO.fromEntity(cab);
    }

    /**
     * DEPRECATED: Cab-level status management has been replaced with shift-level status tracking
     *
     * Status is now managed per shift (DAY/NIGHT) via ShiftStatusService.
     * Use ShiftStatusController endpoints to activate/deactivate individual shifts.
     *
     * This method is kept for backward compatibility but will log a deprecation warning.
     *
     * @param id The cab ID
     * @return The cab DTO (status field will be ignored)
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    @Transactional
    public CabDTO retire(Long id) {
        log.warn("DEPRECATED: retire() is deprecated. Status is now managed at shift level. " +
                "Use ShiftStatusService to manage shift status.");

        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + id));

        // No-op since cab status has been removed
        log.info("Cab {} retirement requested but is deprecated", cab.getCabNumber());

        return CabDTO.fromEntity(cab);
    }

    /**
     * Delete cab (hard delete)
     */
    @Transactional
    public void deleteCab(Long id) {
        log.info("Deleting cab with ID: {}", id);

        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + id));

        cabRepository.delete(cab);
        log.info("Cab deleted: {}", cab.getCabNumber());
    }

    /**
     * Get ownership history for a cab
     */
    @Transactional(readOnly = true)
    public List<CabOwnerHistoryDTO> getOwnerHistory(Long cabId) {
        log.info("Getting owner history for cab ID: {}", cabId);
        
        Cab cab = cabRepository.findById(cabId)
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + cabId));
        
        return cabOwnerHistoryRepository.findByCabOrderByStartDateDesc(cab).stream()
                .map(CabOwnerHistoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Generate unique cab number
     */
    private String generateCabNumber() {
        long count = cabRepository.count();
        String cabNumber;
        int counter = (int) count + 1;

        do {
            cabNumber = String.format("CAB-%03d", counter);
            counter++;
        } while (cabRepository.existsByCabNumber(cabNumber));

        return cabNumber;
    }
}
