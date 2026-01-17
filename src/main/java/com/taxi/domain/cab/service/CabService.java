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
import com.taxi.web.dto.cab.CabDTO;
import com.taxi.web.dto.cab.CreateCabRequest;
import com.taxi.web.dto.cab.UpdateCabRequest;
import com.taxi.web.dto.cab.CabOwnerHistoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
     * Get active cabs only
     */
    @Transactional(readOnly = true)
    public List<CabDTO> getActiveCabs() {
        log.info("Getting active cabs");
        return cabRepository.findAllActiveCabs().stream()
                .map(CabDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get cabs by status
     */
    @Transactional(readOnly = true)
    public List<CabDTO> getCabsByStatus(Cab.CabStatus status) {
        log.info("Getting cabs with status: {}", status);
        return cabRepository.findByStatus(status).stream()
                .map(CabDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get cabs by type
     */
    @Transactional(readOnly = true)
    public List<CabDTO> getCabsByType(CabType type) {
        log.info("Getting cabs of type: {}", type);
        return cabRepository.findByCabType(type).stream()
                .map(CabDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get cabs with airport license
     */
    @Transactional(readOnly = true)
    public List<CabDTO> getCabsWithAirportLicense() {
        log.info("Getting cabs with airport license");
        return cabRepository.findCabsWithAirportLicense().stream()
                .map(CabDTO::fromEntity)
                .collect(Collectors.toList());
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
     */
    @Transactional
    public CabDTO createCab(CreateCabRequest request) {
        log.info("Creating new cab with registration: {}", request.getRegistrationNumber());

        // Validate registration number doesn't exist
        if (cabRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new RuntimeException("Registration number already exists: " + request.getRegistrationNumber());
        }

        // Validate cab type
        CabType cabType;
        try {
            cabType = CabType.valueOf(request.getCabType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid cab type: " + request.getCabType());
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

        // Validate cab shift type if provided
        CabShiftType cabShiftType = null;
        if (request.getCabShiftType() != null && !request.getCabShiftType().isEmpty()) {
            try {
                cabShiftType = CabShiftType.valueOf(request.getCabShiftType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid cab shift type: " + request.getCabShiftType());
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

        // Create cab
        Cab cab = Cab.builder()
                .cabNumber(cabNumber)
                .registrationNumber(request.getRegistrationNumber())
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .color(request.getColor())
                .cabType(cabType)
                .shareType(shareType)
                .cabShiftType(cabShiftType)
                .hasAirportLicense(request.getHasAirportLicense() != null ? request.getHasAirportLicense() : false)
                .airportLicenseNumber(request.getAirportLicenseNumber())
                .airportLicenseExpiry(request.getAirportLicenseExpiry())
                .status(Cab.CabStatus.ACTIVE)
                .ownerDriver(ownerDriver)
                .notes(request.getNotes())
                .build();

        cab = cabRepository.save(cab);
        log.info("Cab created with number: {}", cab.getCabNumber());

        // Create owner history record if owner assigned
        if (ownerDriver != null) {
            createOwnerHistoryRecord(cab, ownerDriver, LocalDate.now(), "Initial owner assignment");
        }

        return CabDTO.fromEntity(cab);
    }

    /**
     * Update an existing cab
     */
    @Transactional
    public CabDTO updateCab(Long id, UpdateCabRequest request) {
        log.info("Updating cab with ID: {}", id);

        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + id));

        // Update fields if provided
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

        if (request.getCabType() != null) {
            try {
                cab.setCabType(CabType.valueOf(request.getCabType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid cab type: " + request.getCabType());
            }
        }

        if (request.getShareType() != null) {
            if (request.getShareType().isEmpty()) {
                cab.setShareType(null);
            } else {
                try {
                    cab.setShareType(ShareType.valueOf(request.getShareType().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid share type: " + request.getShareType());
                }
            }
        }

        if (request.getCabShiftType() != null) {
            if (request.getCabShiftType().isEmpty()) {
                cab.setCabShiftType(null);
            } else {
                try {
                    cab.setCabShiftType(CabShiftType.valueOf(request.getCabShiftType().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid cab shift type: " + request.getCabShiftType());
                }
            }
        }

        if (request.getHasAirportLicense() != null) {
            cab.setHasAirportLicense(request.getHasAirportLicense());
        }

        if (request.getAirportLicenseNumber() != null) {
            cab.setAirportLicenseNumber(request.getAirportLicenseNumber());
        }

        if (request.getAirportLicenseExpiry() != null) {
            cab.setAirportLicenseExpiry(request.getAirportLicenseExpiry());
        }

        if (request.getStatus() != null) {
            try {
                cab.setStatus(Cab.CabStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status: " + request.getStatus());
            }
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
     * Set cab to maintenance
     */
    @Transactional
    public CabDTO setMaintenance(Long id) {
        log.info("Setting cab to maintenance: {}", id);

        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + id));

        cab.setMaintenance();
        cab = cabRepository.save(cab);
        log.info("Cab {} set to maintenance", cab.getCabNumber());

        return CabDTO.fromEntity(cab);
    }

    /**
     * Activate cab
     */
    @Transactional
    public CabDTO activate(Long id) {
        log.info("Activating cab: {}", id);

        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + id));

        cab.activate();
        cab = cabRepository.save(cab);
        log.info("Cab {} activated", cab.getCabNumber());

        return CabDTO.fromEntity(cab);
    }

    /**
     * Retire cab
     */
    @Transactional
    public CabDTO retire(Long id) {
        log.info("Retiring cab: {}", id);

        Cab cab = cabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + id));

        cab.retire();
        cab = cabRepository.save(cab);
        log.info("Cab {} retired", cab.getCabNumber());

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
