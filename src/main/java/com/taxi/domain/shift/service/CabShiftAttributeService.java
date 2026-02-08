package com.taxi.domain.shift.service;

import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.cab.model.ShareType;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.web.dto.shift.ShiftAttributesDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Domain service for managing shift-level attributes
 *
 * Purpose: Manage attributes at the shift level rather than cab level.
 * This allows each shift (DAY/NIGHT) of the same cab to have different attributes.
 *
 * Key Responsibilities:
 * - Update shift attributes (cab type, share type, airport license)
 * - Retrieve shift attribute information
 * - Validate attribute values
 *
 * Example Use Case:
 * - Cab 123 DAY shift: SEDAN with VOTING_SHARE
 * - Cab 123 NIGHT shift: HANDICAP_VAN with NON_VOTING_SHARE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CabShiftAttributeService {

    private final CabShiftRepository cabShiftRepository;

    /**
     * Update all attributes for a shift at once
     *
     * @param shiftId The shift ID
     * @param cabType The cab type (SEDAN, HANDICAP_VAN)
     * @param shareType The share type (VOTING_SHARE, NON_VOTING_SHARE, or null)
     * @param hasAirportLicense Whether shift has airport license
     * @param airportLicenseNumber The airport license number (optional)
     * @param airportLicenseExpiry The airport license expiry date (optional)
     * @throws IllegalArgumentException if shift not found
     */
    @Transactional
    public void updateShiftAttributes(
            Long shiftId,
            CabType cabType,
            ShareType shareType,
            Boolean hasAirportLicense,
            String airportLicenseNumber,
            LocalDate airportLicenseExpiry) {

        log.info("Updating attributes for shift {} - cabType: {}, shareType: {}, airportLicense: {}",
            shiftId, cabType, shareType, hasAirportLicense);

        CabShift shift = cabShiftRepository.findById(shiftId)
            .orElseThrow(() -> {
                log.error("Shift not found: {}", shiftId);
                return new IllegalArgumentException("Shift not found: " + shiftId);
            });

        // Update attributes
        shift.setCabType(cabType);
        shift.setShareType(shareType);
        shift.setHasAirportLicense(hasAirportLicense != null ? hasAirportLicense : false);
        shift.setAirportLicenseNumber(airportLicenseNumber);
        shift.setAirportLicenseExpiry(airportLicenseExpiry);

        cabShiftRepository.save(shift);
        log.info("Shift {} attributes updated successfully", shiftId);
    }

    /**
     * Update only the cab type for a shift
     *
     * @param shiftId The shift ID
     * @param cabType The cab type (SEDAN, HANDICAP_VAN)
     * @throws IllegalArgumentException if shift not found
     */
    @Transactional
    public void updateCabType(Long shiftId, CabType cabType) {
        log.info("Updating cab type for shift {} to {}", shiftId, cabType);

        CabShift shift = cabShiftRepository.findById(shiftId)
            .orElseThrow(() -> {
                log.error("Shift not found: {}", shiftId);
                return new IllegalArgumentException("Shift not found: " + shiftId);
            });

        shift.setCabType(cabType);
        cabShiftRepository.save(shift);
        log.info("Shift {} cab type updated to {}", shiftId, cabType);
    }

    /**
     * Update only the share type for a shift
     *
     * @param shiftId The shift ID
     * @param shareType The share type (VOTING_SHARE, NON_VOTING_SHARE, or null)
     * @throws IllegalArgumentException if shift not found
     */
    @Transactional
    public void updateShareType(Long shiftId, ShareType shareType) {
        log.info("Updating share type for shift {} to {}", shiftId, shareType);

        CabShift shift = cabShiftRepository.findById(shiftId)
            .orElseThrow(() -> {
                log.error("Shift not found: {}", shiftId);
                return new IllegalArgumentException("Shift not found: " + shiftId);
            });

        shift.setShareType(shareType);
        cabShiftRepository.save(shift);
        log.info("Shift {} share type updated to {}", shiftId, shareType);
    }

    /**
     * Update airport license information for a shift
     *
     * @param shiftId The shift ID
     * @param hasLicense Whether shift has airport license
     * @param licenseNumber The license number (optional)
     * @param expiryDate The license expiry date (optional)
     * @throws IllegalArgumentException if shift not found
     * @throws IllegalArgumentException if license number provided but has license is false
     */
    @Transactional
    public void updateAirportLicense(
            Long shiftId,
            Boolean hasLicense,
            String licenseNumber,
            LocalDate expiryDate) {

        log.info("Updating airport license for shift {} - hasLicense: {}, number: {}, expiry: {}",
            shiftId, hasLicense, licenseNumber, expiryDate);

        // Validation: license number requires has license to be true
        if (Boolean.FALSE.equals(hasLicense) && licenseNumber != null && !licenseNumber.isEmpty()) {
            throw new IllegalArgumentException("Cannot set license number when hasAirportLicense is false");
        }

        CabShift shift = cabShiftRepository.findById(shiftId)
            .orElseThrow(() -> {
                log.error("Shift not found: {}", shiftId);
                return new IllegalArgumentException("Shift not found: " + shiftId);
            });

        shift.setHasAirportLicense(hasLicense != null ? hasLicense : false);
        shift.setAirportLicenseNumber(licenseNumber);
        shift.setAirportLicenseExpiry(expiryDate);

        cabShiftRepository.save(shift);
        log.info("Shift {} airport license updated", shiftId);
    }

    /**
     * Retrieve all attributes for a shift as a DTO
     *
     * @param shiftId The shift ID
     * @return ShiftAttributesDTO with all current attribute values
     * @throws IllegalArgumentException if shift not found
     */
    @Transactional(readOnly = true)
    public ShiftAttributesDTO getShiftAttributes(Long shiftId) {
        log.debug("Retrieving attributes for shift {}", shiftId);

        CabShift shift = cabShiftRepository.findById(shiftId)
            .orElseThrow(() -> {
                log.error("Shift not found: {}", shiftId);
                return new IllegalArgumentException("Shift not found: " + shiftId);
            });

        return ShiftAttributesDTO.fromEntity(shift);
    }

    /**
     * Retrieve all attributes for a shift (read-only)
     *
     * @param shiftId The shift ID
     * @return The shift with all current attribute values
     * @throws IllegalArgumentException if shift not found
     */
    @Transactional(readOnly = true)
    public CabShift getShiftWithAttributes(Long shiftId) {
        log.debug("Retrieving attributes for shift {}", shiftId);

        return cabShiftRepository.findById(shiftId)
            .orElseThrow(() -> {
                log.error("Shift not found: {}", shiftId);
                return new IllegalArgumentException("Shift not found: " + shiftId);
            });
    }

    /**
     * Check if shift has airport license
     *
     * @param shiftId The shift ID
     * @return true if shift has airport license, false otherwise
     * @throws IllegalArgumentException if shift not found
     */
    @Transactional(readOnly = true)
    public boolean hasAirportLicense(Long shiftId) {
        return cabShiftRepository.findById(shiftId)
            .map(s -> Boolean.TRUE.equals(s.getHasAirportLicense()))
            .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));
    }

    /**
     * Check if shift's airport license is expired
     *
     * @param shiftId The shift ID
     * @return true if has license and it's expired, false otherwise
     * @throws IllegalArgumentException if shift not found
     */
    @Transactional(readOnly = true)
    public boolean isAirportLicenseExpired(Long shiftId) {
        CabShift shift = cabShiftRepository.findById(shiftId)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

        return shift.isAirportLicenseExpired();
    }

    /**
     * Get the cab type for a shift
     *
     * @param shiftId The shift ID
     * @return The cab type, or null if not set
     * @throws IllegalArgumentException if shift not found
     */
    @Transactional(readOnly = true)
    public CabType getCabType(Long shiftId) {
        return cabShiftRepository.findById(shiftId)
            .map(CabShift::getCabType)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));
    }

    /**
     * Get the share type for a shift
     *
     * @param shiftId The shift ID
     * @return The share type, or null if not set
     * @throws IllegalArgumentException if shift not found
     */
    @Transactional(readOnly = true)
    public ShareType getShareType(Long shiftId) {
        return cabShiftRepository.findById(shiftId)
            .map(CabShift::getShareType)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));
    }

    /**
     * Get airport license number for a shift
     *
     * @param shiftId The shift ID
     * @return The license number, or null if not set
     * @throws IllegalArgumentException if shift not found
     */
    @Transactional(readOnly = true)
    public String getAirportLicenseNumber(Long shiftId) {
        return cabShiftRepository.findById(shiftId)
            .map(CabShift::getAirportLicenseNumber)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));
    }

    /**
     * Get airport license expiry date for a shift
     *
     * @param shiftId The shift ID
     * @return The expiry date, or null if not set
     * @throws IllegalArgumentException if shift not found
     */
    @Transactional(readOnly = true)
    public LocalDate getAirportLicenseExpiry(Long shiftId) {
        return cabShiftRepository.findById(shiftId)
            .map(CabShift::getAirportLicenseExpiry)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));
    }
}
