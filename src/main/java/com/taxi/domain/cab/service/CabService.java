package com.taxi.domain.cab.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabStatus;
import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.cab.model.ShareType;
import com.taxi.domain.cab.model.CabShiftType;
import com.taxi.domain.cab.model.CabOwnerHistory;
import com.taxi.domain.cab.model.CabShiftTypeHistory;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.cab.repository.CabOwnerHistoryRepository;
import com.taxi.domain.cab.repository.CabShiftTypeHistoryRepository;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.ShiftOwnership;
import com.taxi.domain.shift.model.ShiftType;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.domain.shift.repository.ShiftOwnershipRepository;
import com.taxi.domain.shift.service.ShiftStatusService;
import com.taxi.domain.profile.model.ShiftProfile;
import com.taxi.domain.profile.repository.ShiftProfileRepository;
import com.taxi.web.dto.cab.CabDTO;
import com.taxi.web.dto.cab.CreateCabRequest;
import com.taxi.web.dto.cab.CreateShiftRequest;
import com.taxi.web.dto.cab.UpdateCabRequest;
import com.taxi.web.dto.cab.CabOwnerHistoryDTO;
import com.taxi.web.dto.cab.ReactivateCabRequest;
import com.taxi.web.dto.cab.ReactivateShiftRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final CabShiftTypeHistoryRepository cabShiftTypeHistoryRepository;
    private final ShiftStatusService shiftStatusService;
    private final ShiftOwnershipRepository shiftOwnershipRepository;
    private final ShiftProfileRepository shiftProfileRepository;

    /**
     * Get all cabs with shift-derived attributes
     */
    @Transactional(readOnly = true)
    public List<CabDTO> getAllCabs() {
        log.info("Getting all cabs");
        return cabRepository.findAllWithShifts().stream()
                .map(CabDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get active cabs only
     */
    @Transactional(readOnly = true)
    public List<CabDTO> getActiveCabs() {
        return cabRepository.findAllWithShifts().stream()
                .filter(c -> c.getStatus() == CabStatus.ACTIVE)
                .map(CabDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get cabs by owner ID (for drivers to see their cabs)
     * Filters by checking which cabs have shifts owned by the specified driver
     */
    @Transactional(readOnly = true)
    public List<CabDTO> getCabsByOwnerId(Long ownerId) {
        log.info("Getting cabs for owner (driver): {}", ownerId);
        return cabRepository.findAllWithShifts().stream()
                .filter(c -> c.getShifts() != null &&
                           c.getShifts().stream()
                            .anyMatch(shift -> shift.getCurrentOwner() != null &&
                                             shift.getCurrentOwner().getId().equals(ownerId)))
                .map(CabDTO::fromEntity)
                .collect(Collectors.toList());
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
     * Get cab by ID (shifts loaded eagerly via @Transactional)
     */
    @Transactional(readOnly = true)
    public CabDTO getCabById(Long id) {
        log.info("Getting cab by ID: {}", id);
        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + id));
        // Force initialization of shifts within transaction
        if (cab.getShifts() != null) cab.getShifts().size();
        return CabDTO.fromEntity(cab);
    }

    /**
     * Get cab by cab number (shifts loaded eagerly via @Transactional)
     */
    @Transactional(readOnly = true)
    public CabDTO getCabByCabNumber(String cabNumber) {
        log.info("Getting cab by cab number: {}", cabNumber);
        Cab cab = cabRepository.findByCabNumber(cabNumber)
                .orElseThrow(() -> new RuntimeException("Cab not found with number: " + cabNumber));
        // Force initialization of shifts within transaction
        if (cab.getShifts() != null) cab.getShifts().size();
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
        log.info("Creating new cab with cab number: {}, registration: {}", request.getCabNumber(), request.getRegistrationNumber());

        // Validate cab number if provided - must be numeric
        if (request.getCabNumber() != null && !request.getCabNumber().isBlank()) {
            if (!request.getCabNumber().matches("\\d+")) {
                throw new RuntimeException("Cab number must be numeric");
            }
            if (cabRepository.existsByCabNumber(request.getCabNumber())) {
                throw new RuntimeException("Cab number already exists: " + request.getCabNumber());
            }
        }

        // Validate registration number doesn't exist (if provided)
        if (request.getRegistrationNumber() != null && !request.getRegistrationNumber().isBlank()
                && cabRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new RuntimeException("Registration number already exists: " + request.getRegistrationNumber());
        }

        // Use provided cab number or auto-generate
        String cabNumber = (request.getCabNumber() != null && !request.getCabNumber().isBlank())
                ? request.getCabNumber()
                : generateCabNumber();

        // Determine cab shift type from shifts list or request
        CabShiftType cabShiftType = CabShiftType.DOUBLE; // Default
        if (request.getShifts() != null && !request.getShifts().isEmpty()) {
            cabShiftType = request.getShifts().size() == 1 ? CabShiftType.SINGLE : CabShiftType.DOUBLE;
        } else if (request.getCabShiftType() != null && !request.getCabShiftType().isEmpty()) {
            try {
                cabShiftType = CabShiftType.valueOf(request.getCabShiftType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid cab shift type: " + request.getCabShiftType());
            }
        }

        // Create cab (simplified - only vehicle information)
        Cab cab = Cab.builder()
                .cabNumber(cabNumber)
                .registrationNumber(request.getRegistrationNumber())
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .color(request.getColor())
                .shiftType(cabShiftType)
                .fleetAddedDate(request.getFleetAddedDate() != null ? request.getFleetAddedDate() : LocalDate.now())
                .notes(request.getNotes())
                .build();

        cab = cabRepository.save(cab);
        log.info("Cab created with number: {}, fleet added date: {}", cab.getCabNumber(), cab.getFleetAddedDate());

        // ====================================================================
        // CREATE SHIFTS from request configuration
        // ====================================================================

        if (request.getShifts() != null && !request.getShifts().isEmpty()) {
            log.info("Creating {} shifts for cab {}", request.getShifts().size(), cab.getCabNumber());

            LocalDate ownershipStartDate = cab.getFleetAddedDate();

            for (CreateShiftRequest shiftReq : request.getShifts()) {
                // Validate shift type
                ShiftType shiftType;
                try {
                    shiftType = ShiftType.valueOf(shiftReq.getShiftType().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid shift type: " + shiftReq.getShiftType());
                }

                // Get owner if specified
                Driver owner = null;
                if (shiftReq.getOwnerId() != null) {
                    owner = driverRepository.findById(shiftReq.getOwnerId())
                            .orElseThrow(() -> new RuntimeException("Driver not found: " + shiftReq.getOwnerId()));

                    if (!Boolean.TRUE.equals(owner.getIsOwner())) {
                        throw new RuntimeException("Driver " + owner.getDriverNumber() + " is not marked as owner");
                    }
                }

                // Parse attributes
                CabType cabType = shiftReq.getCabType() != null
                        ? CabType.valueOf(shiftReq.getCabType().toUpperCase())
                        : null;

                ShareType shareType = shiftReq.getShareType() != null && !shiftReq.getShareType().isEmpty()
                        ? ShareType.valueOf(shiftReq.getShareType().toUpperCase())
                        : null;

                // Default shift times if not provided
                String startTime = shiftReq.getStartTime() != null
                        ? shiftReq.getStartTime()
                        : (shiftType == ShiftType.DAY ? "06:00" : "18:00");

                String endTime = shiftReq.getEndTime() != null
                        ? shiftReq.getEndTime()
                        : (shiftType == ShiftType.DAY ? "18:00" : "06:00");

                // Get or assign default shift profile
                ShiftProfile profile = null;
                if (shiftReq.getProfileId() != null) {
                    // Use specified profile
                    profile = shiftProfileRepository.findById(shiftReq.getProfileId())
                            .orElseThrow(() -> new RuntimeException("Shift profile not found: " + shiftReq.getProfileId()));

                    if (!Boolean.TRUE.equals(profile.getIsActive())) {
                        throw new RuntimeException("Cannot assign inactive profile: " + profile.getProfileCode());
                    }
                } else {
                    // Auto-assign default profile
                    profile = shiftProfileRepository.findByIsDefaultTrue()
                            .orElse(null);

                    if (profile != null) {
                        log.info("Auto-assigning default profile '{}' to {} shift", profile.getProfileCode(), shiftType);
                    } else {
                        log.warn("No default shift profile found - shift will be created without profile");
                    }
                }

                // Create shift
                CabShift shift = CabShift.builder()
                        .cab(cab)
                        .shiftType(shiftType)
                        .startTime(startTime)
                        .endTime(endTime)
                        .currentOwner(owner)
                        .currentProfile(profile)
                        .status(owner != null ? CabShift.ShiftStatus.ACTIVE : CabShift.ShiftStatus.INACTIVE)
                        .cabType(cabType)
                        .shareType(shareType)
                        .hasAirportLicense(shiftReq.getHasAirportLicense() != null ? shiftReq.getHasAirportLicense() : false)
                        .airportLicenseNumber(shiftReq.getAirportLicenseNumber())
                        .airportLicenseExpiry(shiftReq.getAirportLicenseExpiry())
                        .build();

                CabShift savedShift = cabShiftRepository.save(shift);

                // Increment profile usage count
                if (profile != null) {
                    profile.incrementUsage();
                    shiftProfileRepository.save(profile);
                }

                log.info("Created {} shift for cab {} - shift ID: {}, profile: {}",
                    shiftType, cab.getCabNumber(), savedShift.getId(),
                    profile != null ? profile.getProfileCode() : "NONE");

                // Create status history if shift has owner (active)
                if (owner != null) {
                    shiftStatusService.activateShift(
                        savedShift.getId(),
                        ownershipStartDate,
                        "SYSTEM",
                        "Initial creation with cab"
                    );
                    log.debug("Created status history for {} shift", shiftType);

                    // Create ownership record
                    ShiftOwnership ownership = ShiftOwnership.builder()
                        .shift(savedShift)
                        .owner(owner)
                        .startDate(ownershipStartDate)
                        .endDate(null)
                        .acquisitionType(ShiftOwnership.AcquisitionType.INITIAL_ASSIGNMENT)
                        .notes(shiftReq.getNotes() != null ? shiftReq.getNotes() : "Initial ownership with cab creation")
                        .build();
                    shiftOwnershipRepository.save(ownership);
                    log.info("Created ownership record for {} shift - start date: {}", shiftType, ownershipStartDate);

                    // Create cab owner history (only once for first shift with owner)
                    if (cab.getOwnerDriver() == null) {
                        cab.setOwnerDriver(owner);
                        cabRepository.save(cab);
                        createOwnerHistoryRecord(cab, owner, ownershipStartDate, "Initial owner assignment");
                    }
                }
            }

            log.info("Successfully created {} shifts for cab {}", request.getShifts().size(), cab.getCabNumber());

        } else if (request.getOwnerDriverId() != null) {
            // BACKWARD COMPATIBILITY: Use deprecated fields if shifts[] not provided
            log.warn("Using deprecated cab creation format for cab {} - recommend using shifts[] array", cab.getCabNumber());

            Driver ownerDriver = driverRepository.findById(request.getOwnerDriverId())
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + request.getOwnerDriverId()));

            if (!Boolean.TRUE.equals(ownerDriver.getIsOwner())) {
                throw new RuntimeException("Driver must be marked as owner");
            }

            CabType cabType = request.getCabType() != null ? CabType.valueOf(request.getCabType().toUpperCase()) : null;
            ShareType shareType = request.getShareType() != null && !request.getShareType().isEmpty()
                    ? ShareType.valueOf(request.getShareType().toUpperCase()) : null;

            LocalDate ownershipStartDate = cab.getFleetAddedDate();

            // Create default shifts based on cab shift type
            createDefaultShiftWithOwnership(cab, ShiftType.DAY, ownerDriver, cabType, shareType, request, ownershipStartDate);

            if (cabShiftType == CabShiftType.DOUBLE) {
                createDefaultShiftWithOwnership(cab, ShiftType.NIGHT, ownerDriver, cabType, shareType, request, ownershipStartDate);
            }

            cab.setOwnerDriver(ownerDriver);
            cabRepository.save(cab);
            createOwnerHistoryRecord(cab, ownerDriver, ownershipStartDate, "Initial owner assignment");

        } else {
            // No shifts and no owner - warn about orphaned cab
            log.warn("Cab {} created WITHOUT shifts - user must add shifts manually later", cab.getCabNumber());
        }

        return CabDTO.fromEntity(cab);
    }

    /**
     * Helper method to create shift with ownership (for backward compatibility)
     */
    private void createDefaultShiftWithOwnership(Cab cab, ShiftType shiftType, Driver owner,
                                                 CabType cabType, ShareType shareType,
                                                 CreateCabRequest request, LocalDate ownershipStartDate) {
        String startTime = shiftType == ShiftType.DAY ? "06:00" : "18:00";
        String endTime = shiftType == ShiftType.DAY ? "18:00" : "06:00";

        // Get default profile
        ShiftProfile defaultProfile = shiftProfileRepository.findByIsDefaultTrue().orElse(null);
        if (defaultProfile != null) {
            log.info("Auto-assigning default profile '{}' to {} shift (backward compatibility mode)",
                defaultProfile.getProfileCode(), shiftType);
        }

        CabShift shift = CabShift.builder()
                .cab(cab)
                .shiftType(shiftType)
                .startTime(startTime)
                .endTime(endTime)
                .currentOwner(owner)
                .currentProfile(defaultProfile)
                .status(CabShift.ShiftStatus.ACTIVE)
                .cabType(cabType)
                .shareType(shareType)
                .hasAirportLicense(request.getHasAirportLicense() != null ? request.getHasAirportLicense() : false)
                .airportLicenseNumber(request.getAirportLicenseNumber())
                .airportLicenseExpiry(request.getAirportLicenseExpiry())
                .build();

        CabShift savedShift = cabShiftRepository.save(shift);

        // Increment profile usage
        if (defaultProfile != null) {
            defaultProfile.incrementUsage();
            shiftProfileRepository.save(defaultProfile);
        }

        shiftStatusService.activateShift(savedShift.getId(), ownershipStartDate, "SYSTEM", "Initial creation with cab");

        ShiftOwnership ownership = ShiftOwnership.builder()
                .shift(savedShift)
                .owner(owner)
                .startDate(ownershipStartDate)
                .endDate(null)
                .acquisitionType(ShiftOwnership.AcquisitionType.INITIAL_ASSIGNMENT)
                .notes("Initial ownership with cab creation")
                .build();
        shiftOwnershipRepository.save(ownership);

        log.info("Created {} shift with ownership for cab {}", shiftType, cab.getCabNumber());
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
     * Activate a cab and all its shifts
     */
    @Transactional
    public CabDTO activate(Long id) {
        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + id));

        cab.setStatus(CabStatus.ACTIVE);
        List<CabShift> shifts = cabShiftRepository.findByCabId(cab.getId());
        for (CabShift shift : shifts) {
            shift.setStatus(CabShift.ShiftStatus.ACTIVE);
            cabShiftRepository.save(shift);
        }

        cab = cabRepository.save(cab);
        // Force reload shifts for DTO
        if (cab.getShifts() != null) cab.getShifts().size();
        log.info("Cab {} activated with {} shifts", cab.getCabNumber(), shifts.size());
        return CabDTO.fromEntity(cab);
    }

    /**
     * Reactivate a deactivated cab with full audit trail
     *
     * Supports two modes:
     * 1. SAME OWNERS (keepSameOwners=true): Quick reactivation with same owners
     *    - Reactivates shifts with existing owners
     *    - Creates new ownership records starting from reactivation date
     *
     * 2. NEW OWNERS (keepSameOwners=false): Full reconfiguration like new cab
     *    - Allows changing owners, profiles, attributes per shift
     *    - Creates new ownership records with new owners
     *
     * @param cabId ID of cab to reactivate
     * @param request ReactivateCabRequest with mode and configuration
     * @return Updated CabDTO
     */
    @Transactional
    public CabDTO reactivateCab(Long cabId, ReactivateCabRequest request) {

        LocalDate reactivationDate = request.getReactivationDate();
        String reason = request.getReason();
        String reactivatedBy = request.getReactivatedBy();
        Boolean keepSameOwners = request.getKeepSameOwners() != null ? request.getKeepSameOwners() : true;

        System.out.println("=== CAB REACTIVATION START ===");
        System.out.println("Cab ID: " + cabId);
        System.out.println("Reactivation Date: " + reactivationDate);
        System.out.println("Keep Same Owners: " + keepSameOwners);
        System.out.println("Reason: " + reason);
        System.out.println("Reactivated By: " + reactivatedBy);

        log.info("Reactivating cab {} with effective date: {}, mode: {}",
            cabId, reactivationDate, keepSameOwners ? "SAME_OWNERS" : "NEW_OWNERS");

        // 1. Get cab
        Cab cab = cabRepository.findById(cabId)
            .orElseThrow(() -> new RuntimeException("Cab not found: " + cabId));

        // 2. Validate cab is currently inactive
        if (cab.getStatus() == CabStatus.ACTIVE) {
            throw new RuntimeException("Cab " + cab.getCabNumber() + " is already active");
        }

        // 3. Validate reactivation date
        if (reactivationDate == null) {
            reactivationDate = LocalDate.now();
        }

        if (cab.getDeactivatedDate() != null && reactivationDate.isBefore(cab.getDeactivatedDate())) {
            throw new RuntimeException(
                String.format("Reactivation date (%s) cannot be before deactivation date (%s)",
                    reactivationDate, cab.getDeactivatedDate())
            );
        }

        if (cab.getFleetAddedDate() != null && reactivationDate.isBefore(cab.getFleetAddedDate())) {
            throw new RuntimeException(
                String.format("Reactivation date (%s) cannot be before fleet added date (%s)",
                    reactivationDate, cab.getFleetAddedDate())
            );
        }

        // 4. Update cab status and clear deactivation date
        cab.setStatus(CabStatus.ACTIVE);
        cab.setDeactivatedDate(null);
        cab = cabRepository.save(cab);

        // 5. Get all shifts for this cab
        List<CabShift> shifts = cabShiftRepository.findByCabId(cab.getId());

        int reactivatedCount = 0;
        int ownershipsCreated = 0;

        // 6. Branch based on reactivation mode
        if (keepSameOwners) {
            // MODE 1: SAME OWNERS - Quick reactivation
            log.info("Reactivating with SAME OWNERS mode - using current shift owners");

            Map<Long, Long> shiftOwnerAssignments = request.getShiftOwnerAssignments();

            for (CabShift shift : shifts) {
                // Get owner: use assignment if provided AND non-null, otherwise use current owner from shift
                Long ownerId = null;

                // First try: get from shiftOwnerAssignments map (if provided and not null)
                if (shiftOwnerAssignments != null && shiftOwnerAssignments.containsKey(shift.getId())) {
                    ownerId = shiftOwnerAssignments.get(shift.getId());
                }

                // Second try: if still null, get from shift's current owner
                if (ownerId == null && shift.getCurrentOwner() != null) {
                    ownerId = shift.getCurrentOwner().getId();
                    log.info("Using shift's current owner {} for shift {} (not in assignment map or was null)",
                        shift.getCurrentOwner().getDriverNumber(), shift.getId());
                }

                if (ownerId == null) {
                    throw new RuntimeException(
                        String.format("No owner found for shift %d (cab %s, type %s). Shift has no current owner and no owner assignment provided.",
                            shift.getId(), cab.getCabNumber(), shift.getShiftType())
                    );
                }

                final Long finalOwnerId = ownerId; // Make effectively final for lambda
                Driver owner = driverRepository.findById(finalOwnerId)
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + finalOwnerId));

                if (!Boolean.TRUE.equals(owner.getIsOwner())) {
                    throw new RuntimeException(
                        String.format("Driver %s is not marked as owner", owner.getDriverNumber())
                    );
                }

                // Activate shift
                shiftStatusService.activateShift(
                    shift.getId(),
                    reactivationDate,
                    reactivatedBy != null ? reactivatedBy : "system",
                    reason != null ? reason : "Cab reactivated - same owner"
                );

                // Update current owner
                shift.setCurrentOwner(owner);
                cabShiftRepository.save(shift);

                // Create ownership record
                ShiftOwnership ownership = ShiftOwnership.builder()
                    .shift(shift)
                    .owner(owner)
                    .startDate(reactivationDate)
                    .endDate(null)
                    .acquisitionType(ShiftOwnership.AcquisitionType.TRANSFER)
                    .notes("Cab reactivated (same owner): " + (reason != null ? reason : ""))
                    .build();
                shiftOwnershipRepository.save(ownership);

                reactivatedCount++;
                ownershipsCreated++;
                log.info("Reactivated shift {} with same owner {} ({})",
                    shift.getId(), owner.getDriverNumber(), shift.getShiftType());
            }

        } else {
            // MODE 2: NEW OWNERS - Full reconfiguration
            log.info("Reactivating with NEW OWNERS mode (full configuration)");

            List<ReactivateShiftRequest> shiftConfigs = request.getShifts();
            if (shiftConfigs == null || shifts.size() != shiftConfigs.size()) {
                throw new RuntimeException(
                    String.format("Shift configuration mismatch: cab has %d shifts but %d configs provided",
                        shifts.size(), shiftConfigs != null ? shiftConfigs.size() : 0)
                );
            }

            for (ReactivateShiftRequest shiftConfig : shiftConfigs) {
                CabShift shift = shifts.stream()
                    .filter(s -> s.getId().equals(shiftConfig.getShiftId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Shift not found: " + shiftConfig.getShiftId()));

                // Get new owner
                Driver owner = driverRepository.findById(shiftConfig.getOwnerId())
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + shiftConfig.getOwnerId()));

                if (!Boolean.TRUE.equals(owner.getIsOwner())) {
                    throw new RuntimeException(
                        String.format("Driver %s is not marked as owner", owner.getDriverNumber())
                    );
                }

                // Get or assign profile
                ShiftProfile profile = null;
                if (shiftConfig.getProfileId() != null) {
                    profile = shiftProfileRepository.findById(shiftConfig.getProfileId())
                        .orElseThrow(() -> new RuntimeException("Shift profile not found: " + shiftConfig.getProfileId()));
                } else {
                    profile = shiftProfileRepository.findByIsDefaultTrue().orElse(null);
                    if (profile != null) {
                        log.info("Auto-assigning default profile '{}' to {} shift during reactivation",
                            profile.getProfileCode(), shift.getShiftType());
                    }
                }

                // Activate shift
                shiftStatusService.activateShift(
                    shift.getId(),
                    reactivationDate,
                    reactivatedBy != null ? reactivatedBy : "system",
                    reason != null ? reason : "Cab reactivated - new configuration"
                );

                // Update shift with new configuration
                shift.setCurrentOwner(owner);
                shift.setCurrentProfile(profile);
                shift.setCabType(CabType.valueOf(shiftConfig.getCabType()));
                shift.setShareType(shiftConfig.getShareType() != null ?
                    ShareType.valueOf(shiftConfig.getShareType()) : null);
                shift.setHasAirportLicense(shiftConfig.getHasAirportLicense());
                shift.setAirportLicenseNumber(shiftConfig.getAirportLicenseNumber());
                shift.setAirportLicenseExpiry(shiftConfig.getAirportLicenseExpiry());
                cabShiftRepository.save(shift);

                // Increment profile usage if assigned
                if (profile != null) {
                    profile.setUsageCount(profile.getUsageCount() + 1);
                    shiftProfileRepository.save(profile);
                }

                // Create ownership record
                ShiftOwnership ownership = ShiftOwnership.builder()
                    .shift(shift)
                    .owner(owner)
                    .startDate(reactivationDate)
                    .endDate(null)
                    .acquisitionType(ShiftOwnership.AcquisitionType.TRANSFER)
                    .notes("Cab reactivated (new owner): " + (reason != null ? reason : ""))
                    .build();
                shiftOwnershipRepository.save(ownership);

                reactivatedCount++;
                ownershipsCreated++;
                log.info("Reactivated shift {} with new owner {} ({}), profile: {}",
                    shift.getId(), owner.getDriverNumber(), shift.getShiftType(),
                    profile != null ? profile.getProfileCode() : "none");
            }
        }

        log.info("Cab {} reactivated successfully on {} ({} shifts reactivated, {} ownerships created)",
            cab.getCabNumber(), reactivationDate, reactivatedCount, ownershipsCreated);

        System.out.println("=== CAB REACTIVATION COMPLETE ===");
        System.out.println("Cab Number: " + cab.getCabNumber());
        System.out.println("Status: " + cab.getStatus());
        System.out.println("Mode: " + (keepSameOwners ? "SAME_OWNERS" : "NEW_OWNERS"));
        System.out.println("Shifts Reactivated: " + reactivatedCount);
        System.out.println("Ownerships Created: " + ownershipsCreated);
        System.out.println("==================================");

        if (cab.getShifts() != null) cab.getShifts().size();
        return CabDTO.fromEntity(cab);
    }

    /**
     * Deactivate cab with proper date tracking and shift history
     */
    @Transactional
    public CabDTO deactivateCab(Long cabId, LocalDate deactivationDate, String reason, String deactivatedBy) {
        System.out.println("=== CAB DEACTIVATION START ===");
        System.out.println("Cab ID: " + cabId);
        System.out.println("Deactivation Date: " + deactivationDate);
        System.out.println("Reason: " + reason);
        System.out.println("Deactivated By: " + deactivatedBy);
        log.info("Deactivating cab {} with effective date: {}", cabId, deactivationDate);

        // 1. Get cab
        Cab cab = cabRepository.findById(cabId)
                .orElseThrow(() -> new RuntimeException("Cab not found: " + cabId));

        // 2. Validate already inactive
        if (cab.getStatus() == CabStatus.INACTIVE) {
            throw new RuntimeException("Cab " + cab.getCabNumber() + " is already inactive");
        }

        // 3. Validate deactivation date
        if (deactivationDate == null) {
            deactivationDate = LocalDate.now();
        }

        if (deactivationDate.isAfter(LocalDate.now())) {
            throw new RuntimeException("Deactivation date cannot be in the future");
        }

        if (cab.getFleetAddedDate() != null && deactivationDate.isBefore(cab.getFleetAddedDate())) {
            throw new RuntimeException(
                    String.format("Deactivation date (%s) cannot be before fleet added date (%s)",
                            deactivationDate, cab.getFleetAddedDate())
            );
        }

        // 4. Update cab status and deactivation date
        cab.setStatus(CabStatus.INACTIVE);
        cab.setDeactivatedDate(deactivationDate);
        cab = cabRepository.save(cab);

        // 5. Get all shifts for this cab
        List<CabShift> shifts = cabShiftRepository.findByCabId(cab.getId());

        // 6. Deactivate each shift and close ownership records
        int deactivatedCount = 0;
        int ownershipsClosed = 0;
        for (CabShift shift : shifts) {
            if (shift.getStatus() == CabShift.ShiftStatus.ACTIVE) {
                try {
                    // Use ShiftStatusService to create proper history records
                    shiftStatusService.deactivateShift(
                            shift.getId(),
                            deactivationDate,
                            deactivatedBy != null ? deactivatedBy : "system",
                            reason != null ? reason : "Cab deactivated"
                    );
                    deactivatedCount++;
                    log.info("Deactivated shift {} for cab {}", shift.getId(), cab.getCabNumber());
                } catch (Exception e) {
                    log.error("Failed to deactivate shift {}: {}", shift.getId(), e.getMessage());
                    // Continue with other shifts even if one fails
                }
            }

            // Close any open ownership records (endDate = NULL)
            try {
                Optional<ShiftOwnership> currentOwnership = shiftOwnershipRepository.findCurrentOwnership(shift.getId());
                if (currentOwnership.isPresent()) {
                    ShiftOwnership ownership = currentOwnership.get();
                    // Set end date to deactivation date (last active day)
                    ownership.setEndDate(deactivationDate);
                    shiftOwnershipRepository.save(ownership);
                    ownershipsClosed++;
                    log.info("Closed ownership record {} for shift {} (ended: {})",
                            ownership.getId(), shift.getId(), ownership.getEndDate());
                }
            } catch (Exception e) {
                log.error("Failed to close ownership for shift {}: {}", shift.getId(), e.getMessage());
            }
        }

        log.info("Cab {} deactivated successfully on {} ({} shifts deactivated, {} ownerships closed)",
                cab.getCabNumber(), deactivationDate, deactivatedCount, ownershipsClosed);

        System.out.println("=== CAB DEACTIVATION COMPLETE ===");
        System.out.println("Cab Number: " + cab.getCabNumber());
        System.out.println("Status: " + cab.getStatus());
        System.out.println("Deactivated Date: " + cab.getDeactivatedDate());
        System.out.println("Shifts Deactivated: " + deactivatedCount);
        System.out.println("Ownerships Closed: " + ownershipsClosed);
        System.out.println("==================================");

        // Force reload shifts for DTO
        if (cab.getShifts() != null) cab.getShifts().size();

        return CabDTO.fromEntity(cab);
    }

    /**
     * @deprecated Use deactivateCab() with date and reason instead
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    @Transactional
    public CabDTO deactivate(Long id) {
        return deactivateCab(id, LocalDate.now(), null, "system");
    }

    /**
     * @deprecated Use deactivate() instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    @Transactional
    public CabDTO setMaintenance(Long id) {
        return deactivate(id);
    }

    /**
     * @deprecated Use deactivate() instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    @Transactional
    public CabDTO retire(Long id) {
        return deactivate(id);
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
     * Change cab shift type (SINGLE <-> DOUBLE) with history tracking and validation
     */
    @Transactional
    public CabDTO changeCabShiftType(Long cabId, CabShiftType newShiftType, String reason, String changedBy) {
        log.info("Changing shift type for cab {} to {}", cabId, newShiftType);

        Cab cab = cabRepository.findById(cabId)
            .orElseThrow(() -> new RuntimeException("Cab not found: " + cabId));

        CabShiftType oldShiftType = cab.getShiftType();

        // Auto-manage shifts based on shift type change
        if (newShiftType == CabShiftType.SINGLE) {
            // DOUBLE → SINGLE: Auto-deactivate NIGHT shift
            List<CabShift> activeShifts = cab.getShifts().stream()
                .filter(s -> s.getStatus() == CabShift.ShiftStatus.ACTIVE)
                .collect(Collectors.toList());

            // Auto-deactivate NIGHT shift if exists
            Optional<CabShift> nightShift = activeShifts.stream()
                .filter(s -> s.getShiftType() == ShiftType.NIGHT)
                .findFirst();

            if (nightShift.isPresent()) {
                LocalDate deactivationDate = LocalDate.now();

                // Deactivate with status history
                shiftStatusService.deactivateShift(
                    nightShift.get().getId(),
                    deactivationDate,
                    changedBy != null ? changedBy : "system",
                    "Auto-deactivated due to shift type change to SINGLE"
                );

                // Close ownership
                Optional<ShiftOwnership> currentOwnership =
                    shiftOwnershipRepository.findCurrentOwnership(nightShift.get().getId());
                if (currentOwnership.isPresent()) {
                    ShiftOwnership ownership = currentOwnership.get();
                    ownership.setEndDate(deactivationDate);
                    shiftOwnershipRepository.save(ownership);
                    log.info("Closed ownership for NIGHT shift due to type change");
                }

                log.info("Auto-deactivated NIGHT shift {} for cab {}", nightShift.get().getId(), cab.getCabNumber());
            }

            // Validate only DAY shift remains
            long dayShiftCount = activeShifts.stream()
                .filter(s -> s.getShiftType() == ShiftType.DAY)
                .count();
            if (dayShiftCount != 1) {
                throw new RuntimeException("Single shift type requires exactly one DAY shift");
            }
        } else if (oldShiftType == CabShiftType.SINGLE && newShiftType == CabShiftType.DOUBLE) {
            // SINGLE → DOUBLE: Auto-create or reactivate NIGHT shift
            Optional<CabShift> nightShift = cab.getShifts().stream()
                .filter(s -> s.getShiftType() == ShiftType.NIGHT)
                .findFirst();

            CabShift dayShift = cab.getShifts().stream()
                .filter(s -> s.getShiftType() == ShiftType.DAY)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("DAY shift not found"));

            if (nightShift.isPresent() && nightShift.get().getStatus() == CabShift.ShiftStatus.INACTIVE) {
                // Reactivate existing NIGHT shift
                LocalDate activationDate = LocalDate.now();

                shiftStatusService.activateShift(
                    nightShift.get().getId(),
                    activationDate,
                    changedBy != null ? changedBy : "system",
                    "Reactivated due to shift type change to DOUBLE"
                );

                // Create new ownership (use DAY shift's owner)
                Driver owner = dayShift.getCurrentOwner();
                if (owner != null) {
                    nightShift.get().setCurrentOwner(owner);
                    cabShiftRepository.save(nightShift.get());

                    ShiftOwnership ownership = ShiftOwnership.builder()
                        .shift(nightShift.get())
                        .owner(owner)
                        .startDate(activationDate)
                        .endDate(null)
                        .acquisitionType(ShiftOwnership.AcquisitionType.TRANSFER)
                        .notes("Reactivated with shift type change to DOUBLE")
                        .build();
                    shiftOwnershipRepository.save(ownership);
                    log.info("Reactivated NIGHT shift with new ownership");
                }

                log.info("Reactivated NIGHT shift {} for cab {}", nightShift.get().getId(), cab.getCabNumber());

            } else if (!nightShift.isPresent()) {
                // Create new NIGHT shift
                CabShift newNightShift = CabShift.builder()
                    .cab(cab)
                    .shiftType(ShiftType.NIGHT)
                    .startTime("18:00")
                    .endTime("06:00")
                    .currentOwner(dayShift.getCurrentOwner())
                    .status(CabShift.ShiftStatus.ACTIVE)
                    .cabType(dayShift.getCabType())
                    .shareType(dayShift.getShareType())
                    .hasAirportLicense(dayShift.getHasAirportLicense())
                    .airportLicenseNumber(dayShift.getAirportLicenseNumber())
                    .airportLicenseExpiry(dayShift.getAirportLicenseExpiry())
                    .build();

                CabShift savedNightShift = cabShiftRepository.save(newNightShift);

                LocalDate activationDate = LocalDate.now();
                shiftStatusService.activateShift(
                    savedNightShift.getId(),
                    activationDate,
                    changedBy != null ? changedBy : "system",
                    "Created due to shift type change to DOUBLE"
                );

                if (dayShift.getCurrentOwner() != null) {
                    ShiftOwnership ownership = ShiftOwnership.builder()
                        .shift(savedNightShift)
                        .owner(dayShift.getCurrentOwner())
                        .startDate(activationDate)
                        .endDate(null)
                        .acquisitionType(ShiftOwnership.AcquisitionType.INITIAL_ASSIGNMENT)
                        .notes("Created with shift type change to DOUBLE")
                        .build();
                    shiftOwnershipRepository.save(ownership);
                }

                log.info("Created new NIGHT shift {} for cab {}", savedNightShift.getId(), cab.getCabNumber());
            }
        }

        // Update cab shift type
        cab.setShiftType(newShiftType);
        cab = cabRepository.save(cab);

        // Record history
        CabShiftTypeHistory history = CabShiftTypeHistory.builder()
            .cabId(cabId)
            .oldShiftType(oldShiftType)
            .newShiftType(newShiftType)
            .changedAt(LocalDateTime.now())
            .changedBy(changedBy)
            .reason(reason)
            .build();
        cabShiftTypeHistoryRepository.save(history);

        log.info("Cab {} shift type changed from {} to {}", cabId, oldShiftType, newShiftType);

        return CabDTO.fromEntity(cab);
    }

    /**
     * Get shift type change history for a cab
     */
    @Transactional(readOnly = true)
    public List<CabShiftTypeHistory> getCabShiftTypeHistory(Long cabId) {
        return cabShiftTypeHistoryRepository.findByCabIdOrderByChangedAtDesc(cabId);
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
