package com.taxi.domain.shift.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.ShiftOwnership;
import com.taxi.domain.shift.model.ShiftType;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.domain.shift.repository.ShiftOwnershipRepository;
import com.taxi.web.dto.shift.CabShiftDTO;
import com.taxi.web.dto.shift.CreateShiftRequest;
import com.taxi.web.dto.shift.ShiftOwnershipDTO;
import com.taxi.web.dto.shift.TransferShiftOwnershipRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing cab shifts and shift ownership
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftService {

    private final CabShiftRepository cabShiftRepository;
    private final ShiftOwnershipRepository shiftOwnershipRepository;
    private final CabRepository cabRepository;
    private final DriverRepository driverRepository;

    /**
     * Get all shifts
     */
    @Transactional(readOnly = true)
    public List<CabShiftDTO> getAllShifts() {
        log.info("Getting all shifts");
        return cabShiftRepository.findAll().stream()
                .map(CabShiftDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all shifts for a cab
     */
    @Transactional(readOnly = true)
    public List<CabShiftDTO> getShiftsByCab(Long cabId) {
        log.info("Getting shifts for cab ID: {}", cabId);
        return cabShiftRepository.findByCabId(cabId).stream()
                .map(CabShiftDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all shifts owned by a driver
     */
    @Transactional(readOnly = true)
    public List<CabShiftDTO> getShiftsByOwner(Long ownerId) {
        log.info("Getting shifts owned by driver ID: {}", ownerId);
        return cabShiftRepository.findByCurrentOwnerId(ownerId).stream()
                .map(CabShiftDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get ownership history for a shift
     */
    @Transactional(readOnly = true)
    public List<ShiftOwnershipDTO> getOwnershipHistory(Long shiftId) {
        log.info("Getting ownership history for shift ID: {}", shiftId);
        return shiftOwnershipRepository.findByShiftIdOrderByStartDateDesc(shiftId).stream()
                .map(ShiftOwnershipDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all ownership history for a driver
     */
    @Transactional(readOnly = true)
    public List<ShiftOwnershipDTO> getOwnershipHistoryByDriver(Long driverId) {
        log.info("Getting ownership history for driver ID: {}", driverId);
        return shiftOwnershipRepository.findByOwnerIdOrderByStartDateDesc(driverId).stream()
                .map(ShiftOwnershipDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Create a new shift for a cab
     */
    @Transactional
    public CabShiftDTO createShift(CreateShiftRequest request) {
        log.info("Creating shift for cab ID: {}", request.getCabId());

        // Get cab
        Cab cab = cabRepository.findById(request.getCabId())
                .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + request.getCabId()));

        // Validate shift type
        ShiftType shiftType;
        try {
            shiftType = ShiftType.valueOf(request.getShiftType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid shift type: " + request.getShiftType());
        }

        // Check if shift already exists for this cab
        if (cabShiftRepository.existsByCabAndShiftType(cab, shiftType)) {
            throw new RuntimeException("Shift " + shiftType + " already exists for cab " + cab.getCabNumber());
        }

        // Get owner
        Driver owner = driverRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + request.getOwnerId()));

        // Validate owner is marked as owner
        if (!Boolean.TRUE.equals(owner.getIsOwner())) {
            throw new RuntimeException("Driver must be marked as owner to own a shift");
        }

        // Create shift
        CabShift shift = CabShift.builder()
                .cab(cab)
                .shiftType(shiftType)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .currentOwner(owner)
                .status(CabShift.ShiftStatus.ACTIVE)
                .notes(request.getNotes())
                .build();

        shift = cabShiftRepository.save(shift);
        log.info("Shift created: {} for cab {}", shiftType, cab.getCabNumber());

        // Create initial ownership record
        ShiftOwnership.AcquisitionType acquisitionType = ShiftOwnership.AcquisitionType.INITIAL_ASSIGNMENT;
        if (request.getAcquisitionType() != null) {
            try {
                acquisitionType = ShiftOwnership.AcquisitionType.valueOf(request.getAcquisitionType().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid acquisition type: {}, using INITIAL_ASSIGNMENT", request.getAcquisitionType());
            }
        }

        ShiftOwnership ownership = ShiftOwnership.builder()
                .shift(shift)
                .owner(owner)
                .startDate(LocalDate.now())
                .endDate(null)  // Current owner
                .acquisitionType(acquisitionType)
                .acquisitionPrice(request.getAcquisitionPrice())
                .notes(request.getNotes())
                .build();

        shiftOwnershipRepository.save(ownership);
        log.info("Initial ownership record created for shift");

        return CabShiftDTO.fromEntity(shift);
    }

    /**
     * Transfer shift ownership to a new owner
     */
    @Transactional
    public CabShiftDTO transferOwnership(Long shiftId, TransferShiftOwnershipRequest request) {
        log.info("Transferring ownership of shift ID: {} to driver ID: {}", shiftId, request.getNewOwnerId());

        // Get shift
        CabShift shift = cabShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found with ID: " + shiftId));

        // Get new owner
        Driver newOwner = driverRepository.findById(request.getNewOwnerId())
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + request.getNewOwnerId()));

        // Validate new owner is marked as owner
        if (!Boolean.TRUE.equals(newOwner.getIsOwner())) {
            throw new RuntimeException("Driver must be marked as owner to own a shift");
        }

        // Get current owner
        Driver currentOwner = shift.getCurrentOwner();

        // Don't transfer if already owned by this driver
        if (currentOwner != null && currentOwner.getId().equals(newOwner.getId())) {
            throw new RuntimeException("Shift is already owned by this driver");
        }

        // Determine transfer date
        LocalDate transferDate = request.getTransferDate() != null ? 
                request.getTransferDate() : LocalDate.now();

        // Close current ownership
        ShiftOwnership currentOwnership = shiftOwnershipRepository.findCurrentOwnership(shiftId)
                .orElseThrow(() -> new RuntimeException("No current ownership found for shift"));

        currentOwnership.close(transferDate, newOwner, request.getSalePrice());
        shiftOwnershipRepository.save(currentOwnership);
        log.info("Closed ownership from driver {}", currentOwner.getDriverNumber());

        // Update shift current owner
        shift.transferOwnership(newOwner);
        shift = cabShiftRepository.save(shift);

        // Create new ownership record
        ShiftOwnership.AcquisitionType acquisitionType = ShiftOwnership.AcquisitionType.TRANSFER;
        if (request.getAcquisitionType() != null) {
            try {
                acquisitionType = ShiftOwnership.AcquisitionType.valueOf(request.getAcquisitionType().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid acquisition type: {}, using TRANSFER", request.getAcquisitionType());
            }
        }

        ShiftOwnership newOwnership = ShiftOwnership.builder()
                .shift(shift)
                .owner(newOwner)
                .startDate(transferDate)
                .endDate(null)  // Current owner
                .acquisitionType(acquisitionType)
                .acquisitionPrice(request.getAcquisitionPrice())
                .notes(request.getNotes())
                .build();

        shiftOwnershipRepository.save(newOwnership);
        log.info("New ownership created for driver {}", newOwner.getDriverNumber());

        return CabShiftDTO.fromEntity(shift);
    }

    /**
     * Get shift by ID
     */
    @Transactional(readOnly = true)
    public CabShiftDTO getShiftById(Long id) {
        log.info("Getting shift by ID: {}", id);
        CabShift shift = cabShiftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shift not found with ID: " + id));
        return CabShiftDTO.fromEntity(shift);
    }

    /**
     * Activate shift
     */
    @Transactional
    public CabShiftDTO activateShift(Long id) {
        log.info("Activating shift ID: {}", id);
        CabShift shift = cabShiftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shift not found with ID: " + id));
        
        shift.activate();
        shift = cabShiftRepository.save(shift);
        log.info("Shift activated");
        
        return CabShiftDTO.fromEntity(shift);
    }

    /**
     * Deactivate shift
     */
    @Transactional
    public CabShiftDTO deactivateShift(Long id) {
        log.info("Deactivating shift ID: {}", id);
        CabShift shift = cabShiftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shift not found with ID: " + id));
        
        shift.deactivate();
        shift = cabShiftRepository.save(shift);
        log.info("Shift deactivated");
        
        return CabShiftDTO.fromEntity(shift);
    }
}
