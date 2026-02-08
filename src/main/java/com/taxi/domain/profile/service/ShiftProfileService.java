package com.taxi.domain.profile.service;

import com.taxi.domain.cab.model.CabAttributeType;
import com.taxi.domain.cab.model.CabAttributeValue;
import com.taxi.domain.cab.repository.CabAttributeTypeRepository;
import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.cab.model.ShareType;
import com.taxi.web.dto.profile.CreateShiftProfileRequest;
import com.taxi.web.dto.profile.UpdateShiftProfileRequest;
import com.taxi.domain.cab.repository.CabAttributeValueRepository;
import com.taxi.domain.profile.model.ShiftProfile;
import com.taxi.domain.profile.model.ShiftProfileAttribute;
import com.taxi.domain.profile.model.ShiftProfileAssignment;
import com.taxi.domain.profile.repository.ShiftProfileAttributeRepository;
import com.taxi.domain.profile.repository.ShiftProfileRepository;
import com.taxi.domain.profile.repository.ShiftProfileAssignmentRepository;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.ShiftType;
import com.taxi.domain.shift.repository.CabShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ShiftProfileService - Business logic for shift profiles
 *
 * Manages:
 * - CRUD operations on shift profiles
 * - Profile assignment to shifts with usage tracking
 * - Profile matching based on shift attributes
 * - Validation and constraint enforcement
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShiftProfileService {

    private final ShiftProfileRepository shiftProfileRepository;
    private final ShiftProfileAttributeRepository profileAttributeRepository;
    private final ShiftProfileAssignmentRepository assignmentRepository;
    private final CabShiftRepository cabShiftRepository;
    private final CabAttributeValueRepository cabAttributeValueRepository;
    private final CabAttributeTypeRepository cabAttributeTypeRepository;

    // ============================================================================
    // CRUD Operations
    // ============================================================================

    /**
     * Get all active profiles ordered by display order
     */
    public List<ShiftProfile> getAllActiveProfiles() {
        log.debug("Fetching all active shift profiles");
        return shiftProfileRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    /**
     * Get all profiles (including inactive)
     */
    public List<ShiftProfile> getAllProfiles() {
        log.debug("Fetching all shift profiles");
        return shiftProfileRepository.findAll();
    }

    /**
     * Get profile by ID
     */
    public ShiftProfile getProfileById(Long profileId) {
        log.debug("Fetching shift profile by ID: {}", profileId);
        return shiftProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));
    }

    /**
     * Get profile by code
     */
    public ShiftProfile getProfileByCode(String profileCode) {
        log.debug("Fetching shift profile by code: {}", profileCode);
        return shiftProfileRepository.findByProfileCode(profileCode)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileCode));
    }

    /**
     * Create a new shift profile with static and dynamic attributes
     */
    public ShiftProfile createProfile(com.taxi.web.dto.profile.CreateShiftProfileRequest request, String currentUser) {
        log.info("Creating new shift profile: {}", request.getProfileCode());

        // Validate unique profile code
        if (shiftProfileRepository.findByProfileCode(request.getProfileCode()).isPresent()) {
            throw new IllegalArgumentException("Profile code already exists: " + request.getProfileCode());
        }

        ShiftProfile profile = ShiftProfile.builder()
                .profileCode(request.getProfileCode())
                .profileName(request.getProfileName())
                .description(request.getDescription())
                .cabType(request.getCabType())
                .shareType(request.getShareType())
                .hasAirportLicense(request.getHasAirportLicense())
                .shiftType(request.getShiftType())
                .category(request.getCategory())
                .colorCode(request.getColorCode())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(true)
                .isSystemProfile(false)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        ShiftProfile savedProfile = shiftProfileRepository.save(profile);

        // Add dynamic attributes if provided
        if (request.getDynamicAttributes() != null && !request.getDynamicAttributes().isEmpty()) {
            for (CreateShiftProfileRequest.DynamicAttributeRequest attrReq : request.getDynamicAttributes()) {
                addAttributeToProfile(savedProfile.getId(), attrReq.getAttributeTypeId(),
                        attrReq.getIsRequired(), attrReq.getExpectedValue(), currentUser);
            }
        }

        log.info("Created shift profile successfully: {} (ID: {})", request.getProfileCode(), savedProfile.getId());
        return savedProfile;
    }

    /**
     * Update an existing shift profile
     * System profiles cannot be deleted but can be modified
     */
    public ShiftProfile updateProfile(Long profileId, com.taxi.web.dto.profile.UpdateShiftProfileRequest request, String currentUser) {
        log.info("Updating shift profile: {}", profileId);

        ShiftProfile profile = getProfileById(profileId);

        // System profiles cannot be deleted but can be updated
        profile.setProfileName(request.getProfileName());
        profile.setDescription(request.getDescription());
        profile.setCabType(request.getCabType());
        profile.setShareType(request.getShareType());
        profile.setHasAirportLicense(request.getHasAirportLicense());
        profile.setShiftType(request.getShiftType());
        profile.setCategory(request.getCategory());
        profile.setColorCode(request.getColorCode());
        profile.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        profile.setUpdatedBy(currentUser);

        // Handle dynamic attributes
        if (request.getDynamicAttributes() != null) {
            // Clear existing attributes
            profileAttributeRepository.deleteByProfileId(profileId);

            // Add new attributes
            for (UpdateShiftProfileRequest.DynamicAttributeRequest attrReq : request.getDynamicAttributes()) {
                addAttributeToProfile(profileId, attrReq.getAttributeTypeId(),
                        attrReq.getIsRequired(), attrReq.getExpectedValue(), currentUser);
            }
        }

        ShiftProfile updated = shiftProfileRepository.save(profile);
        log.info("Updated shift profile: {}", profileId);
        return updated;
    }

    /**
     * Delete a profile
     * Can only delete custom profiles with no shifts assigned
     */
    public void deleteProfile(Long profileId) {
        log.info("Deleting shift profile: {}", profileId);

        ShiftProfile profile = getProfileById(profileId);

        // System profiles cannot be deleted
        if (Boolean.TRUE.equals(profile.getIsSystemProfile())) {
            throw new IllegalArgumentException("Cannot delete system profile: " + profile.getProfileCode());
        }

        // Check if profile is in use
        long shiftCount = shiftProfileRepository.countShiftsUsingProfile(profileId);
        if (shiftCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete profile in use by " + shiftCount + " shifts");
        }

        shiftProfileRepository.deleteById(profileId);
        log.info("Deleted shift profile: {}", profileId);
    }

    /**
     * Activate a profile for assignment
     */
    public ShiftProfile activateProfile(Long profileId, String currentUser) {
        log.info("Activating shift profile: {}", profileId);
        ShiftProfile profile = getProfileById(profileId);
        profile.activate();
        profile.setUpdatedBy(currentUser);
        ShiftProfile updated = shiftProfileRepository.save(profile);
        log.info("Activated shift profile: {}", profileId);
        return updated;
    }

    /**
     * Deactivate a profile (prevents new assignments)
     */
    public ShiftProfile deactivateProfile(Long profileId, String currentUser) {
        log.info("Deactivating shift profile: {}", profileId);
        ShiftProfile profile = getProfileById(profileId);
        profile.deactivate();
        profile.setUpdatedBy(currentUser);
        ShiftProfile updated = shiftProfileRepository.save(profile);
        log.info("Deactivated shift profile: {}", profileId);
        return updated;
    }

    // ============================================================================
    // Profile Assignment to Shifts (with History)
    // ============================================================================

    /**
     * Assign a profile to a shift with history tracking
     * Creates a new assignment record and ends any previous active assignment
     * @param shiftId ID of the shift
     * @param profileId ID of the profile to assign
     * @param reason Reason for the assignment
     * @param startDate Date when assignment becomes effective
     * @param currentUser User making the assignment
     * @return The new assignment record
     */
    public ShiftProfileAssignment assignProfileToShift(Long shiftId, Long profileId,
                                                       String reason, LocalDate startDate, String currentUser) {
        log.info("Assigning profile {} to shift {} starting {}", profileId, shiftId, startDate);

        CabShift shift = cabShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

        ShiftProfile profile = getProfileById(profileId);

        // Validate shift matches profile criteria
        if (!doesShiftMatchProfile(shiftId, profileId)) {
            log.warn("Shift {} does not match profile {}", shiftId, profileId);
            throw new IllegalArgumentException(
                    "Shift does not match profile criteria for static attributes");
        }

        // End any previous active assignment
        var previousAssignment = assignmentRepository.findActiveAssignmentByShiftId(shiftId);
        if (previousAssignment.isPresent()) {
            ShiftProfileAssignment prev = previousAssignment.get();
            prev.endAssignment(startDate.minusDays(1));  // End the day before new assignment
            assignmentRepository.save(prev);

            // Decrement usage count of old profile
            prev.getProfile().decrementUsage();
            shiftProfileRepository.save(prev.getProfile());
        }

        // Create new assignment record
        ShiftProfileAssignment newAssignment = ShiftProfileAssignment.builder()
                .shift(shift)
                .profile(profile)
                .startDate(startDate)
                .endDate(null)  // NULL = currently active
                .reason(reason)
                .assignedBy(currentUser)
                .build();

        ShiftProfileAssignment saved = assignmentRepository.save(newAssignment);

        // Update shift's current profile (denormalized field)
        shift.setCurrentProfile(profile);
        cabShiftRepository.save(shift);

        // Increment usage count
        profile.incrementUsage();
        shiftProfileRepository.save(profile);

        log.info("Assigned profile {} to shift {} starting {} (profile now has {} shifts in use)",
                profileId, shiftId, startDate, profile.getUsageCount());
        return saved;
    }

    /**
     * End a profile assignment for a shift (without assigning a new one)
     * @param shiftId ID of the shift
     * @param endDate Date when assignment ends
     * @param currentUser User making the change
     * @return The ended assignment record
     */
    public ShiftProfileAssignment endProfileAssignment(Long shiftId, LocalDate endDate, String currentUser) {
        log.info("Ending profile assignment for shift: {}", shiftId);

        var activeAssignment = assignmentRepository.findActiveAssignmentByShiftId(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("No active assignment found for shift: " + shiftId));

        activeAssignment.endAssignment(endDate);
        ShiftProfileAssignment updated = assignmentRepository.save(activeAssignment);

        // Update shift's current profile
        CabShift shift = activeAssignment.getShift();
        shift.setCurrentProfile(null);
        cabShiftRepository.save(shift);

        // Decrement usage count
        activeAssignment.getProfile().decrementUsage();
        shiftProfileRepository.save(activeAssignment.getProfile());

        log.info("Ended profile assignment for shift {} (profile now has {} shifts in use)",
                shiftId, activeAssignment.getProfile().getUsageCount());
        return updated;
    }

    /**
     * Get the current active profile assignment for a shift
     */
    public ShiftProfileAssignment getCurrentAssignment(Long shiftId) {
        return assignmentRepository.findActiveAssignmentByShiftId(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("No active assignment for shift: " + shiftId));
    }

    /**
     * Get all profile assignment history for a shift
     */
    public List<ShiftProfileAssignment> getAssignmentHistory(Long shiftId) {
        return assignmentRepository.findByShiftIdOrderByStartDateDesc(shiftId);
    }

    // ============================================================================
    // Profile Matching Logic
    // ============================================================================

    /**
     * Check if shift matches profile's static attribute requirements
     */
    public boolean doesShiftMatchProfile(Long shiftId, Long profileId) {
        log.debug("Checking if shift {} matches profile {}", shiftId, profileId);

        CabShift shift = cabShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

        ShiftProfile profile = getProfileById(profileId);

        return profile.matchesShift(
                shift.getCabType(),
                shift.getShareType(),
                shift.getHasAirportLicense(),
                shift.getShiftType()
        );
    }

    /**
     * Comprehensive check if shift matches profile (static + dynamic attributes)
     */
    public boolean shiftMatchesProfile(Long shiftId, Long profileId, LocalDate date) {
        log.debug("Comprehensive match check - shift {} against profile {} on date {}",
                shiftId, profileId, date);

        // First check static attributes
        if (!doesShiftMatchProfile(shiftId, profileId)) {
            return false;
        }

        CabShift shift = cabShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

        ShiftProfile profile = getProfileById(profileId);

        // Get shift's current attributes
        List<CabAttributeValue> shiftAttributes = cabAttributeValueRepository
                .findCurrentAttributesByShiftId(shiftId);

        Map<Long, CabAttributeValue> attrMap = shiftAttributes.stream()
                .collect(Collectors.toMap(
                        attr -> attr.getAttributeType().getId(),
                        attr -> attr
                ));

        // Check dynamic attribute requirements
        List<ShiftProfileAttribute> profileAttrs = profileAttributeRepository.findByProfileId(profileId);
        for (ShiftProfileAttribute profileAttr : profileAttrs) {
            CabAttributeValue shiftAttr = attrMap.get(profileAttr.getAttributeType().getId());
            String attrValue = shiftAttr != null ? shiftAttr.getAttributeValue() : null;

            if (!profileAttr.matches(attrValue)) {
                log.debug("Dynamic attribute mismatch: {} for shift {}",
                        profileAttr.getAttributeType().getAttributeCode(), shiftId);
                return false;
            }
        }

        return true;
    }

    /**
     * Auto-suggest profiles for a shift based on its attributes
     * Returns profiles that match the shift's static attributes
     */
    public List<ShiftProfile> findSuggestedProfiles(Long shiftId) {
        log.debug("Finding suggested profiles for shift: {}", shiftId);

        CabShift shift = cabShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

        List<ShiftProfile> suggestions = shiftProfileRepository.findMatchingProfiles(
                shift.getCabType(),
                shift.getShareType(),
                shift.getHasAirportLicense(),
                shift.getShiftType()
        );

        log.debug("Found {} suggested profiles for shift {}", suggestions.size(), shiftId);
        return suggestions;
    }

    // ============================================================================
    // Dynamic Attribute Management
    // ============================================================================

    /**
     * Add a dynamic attribute requirement to a profile
     */
    public ShiftProfileAttribute addAttributeToProfile(Long profileId, Long attributeTypeId,
                                                        Boolean isRequired, String expectedValue,
                                                        String currentUser) {
        log.debug("Adding attribute {} to profile {}", attributeTypeId, profileId);

        ShiftProfile profile = getProfileById(profileId);

        // Check if attribute already exists
        Optional<ShiftProfileAttribute> existing = profileAttributeRepository
                .findByProfileIdAndAttributeTypeId(profileId, attributeTypeId);

        if (existing.isPresent()) {
            log.warn("Attribute already associated with profile {}", profileId);
            throw new IllegalArgumentException("Attribute already associated with this profile");
        }

        // Fetch the attribute type from repository
        CabAttributeType attrType = cabAttributeTypeRepository.findById(attributeTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Attribute type not found: " + attributeTypeId));

        ShiftProfileAttribute profileAttr = ShiftProfileAttribute.builder()
                .profile(profile)
                .attributeType(attrType)
                .isRequired(isRequired != null ? isRequired : true)
                .expectedValue(expectedValue)
                .build();

        ShiftProfileAttribute saved = profileAttributeRepository.save(profileAttr);
        log.debug("Added attribute {} to profile {}", attributeTypeId, profileId);
        return saved;
    }

    /**
     * Remove a dynamic attribute from a profile
     */
    public void removeAttributeFromProfile(Long profileId, Long attributeTypeId) {
        log.debug("Removing attribute {} from profile {}", attributeTypeId, profileId);

        ShiftProfileAttribute attr = profileAttributeRepository
                .findByProfileIdAndAttributeTypeId(profileId, attributeTypeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Attribute not associated with this profile"));

        profileAttributeRepository.delete(attr);
        log.debug("Removed attribute {} from profile {}", attributeTypeId, profileId);
    }

}
