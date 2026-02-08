package com.taxi.domain.expense.service;

import com.taxi.domain.expense.model.ExpenseCategory;
import com.taxi.domain.expense.repository.ExpenseCategoryRepository;
import com.taxi.domain.profile.model.ShiftProfile;
import com.taxi.domain.profile.service.ShiftProfileService;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.repository.CabShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ProfileBasedExpenseService - Integration between shift profiles and expense categories
 *
 * Enables:
 * - Linking expense categories to profiles for automated expense application
 * - Finding all shifts applicable for a category's profile
 * - Finding all expense categories applicable to a shift's profile
 * - Preview of impact before linking categories to profiles
 *
 * This simplifies expense matching from complex per-category matching rules
 * to simple profile-based matching where all shifts in a profile auto-match
 * all expense categories linked to that profile.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProfileBasedExpenseService {

    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final CabShiftRepository cabShiftRepository;
    private final ShiftProfileService shiftProfileService;

    // ============================================================================
    // Profile-Expense Linking Operations
    // ============================================================================

    /**
     * Link an expense category to a shift profile
     * All shifts assigned to this profile will match this category
     */
    public ExpenseCategory linkExpenseCategoryToProfile(Long categoryId, Long profileId, String currentUser) {
        log.info("Linking expense category {} to profile {}", categoryId, profileId);

        ExpenseCategory category = expenseCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        ShiftProfile profile = shiftProfileService.getProfileById(profileId);

        category.setShiftProfileId(profileId);
        ExpenseCategory updated = expenseCategoryRepository.save(category);

        log.info("Linked category {} (affects {} shifts) to profile {}",
                categoryId, getCabShiftsForExpenseCategory(categoryId).size(), profileId);
        return updated;
    }

    /**
     * Unlink an expense category from its profile
     * Category will no longer auto-match based on profile
     */
    public ExpenseCategory unlinkExpenseCategoryFromProfile(Long categoryId, String currentUser) {
        log.info("Unlinking expense category {} from profile", categoryId);

        ExpenseCategory category = expenseCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        if (category.getShiftProfileId() == null) {
            log.debug("Category {} is not linked to any profile", categoryId);
            return category;
        }

        category.setShiftProfileId(null);
        ExpenseCategory updated = expenseCategoryRepository.save(category);

        log.info("Unlinked category {} from profile", categoryId);
        return updated;
    }

    // ============================================================================
    // Query Operations - Finding Applicable Entities
    // ============================================================================

    /**
     * Get all shifts applicable for an expense category
     * Returns shifts whose profiles match the category's linked profile
     */
    public List<CabShift> getCabShiftsForExpenseCategory(Long categoryId) {
        log.debug("Finding shifts applicable for expense category {}", categoryId);

        ExpenseCategory category = expenseCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        if (category.getShiftProfileId() == null) {
            log.debug("Category {} has no profile link, returning empty list", categoryId);
            return List.of();
        }

        List<CabShift> shifts = cabShiftRepository.findByCurrentProfileId(category.getShiftProfileId());
        log.debug("Found {} shifts for category {}", shifts.size(), categoryId);
        return shifts;
    }

    /**
     * Get all active shifts applicable for an expense category
     */
    public List<CabShift> getActiveCabShiftsForExpenseCategory(Long categoryId) {
        log.debug("Finding active shifts applicable for expense category {}", categoryId);

        ExpenseCategory category = expenseCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        if (category.getShiftProfileId() == null) {
            return List.of();
        }

        List<CabShift> shifts = cabShiftRepository.findActiveByCurrentProfileId(category.getShiftProfileId());
        log.debug("Found {} active shifts for category {}", shifts.size(), categoryId);
        return shifts;
    }

    /**
     * Get all expense categories applicable to a shift
     * Returns categories whose profiles match the shift's assigned profile
     */
    public List<ExpenseCategory> getExpenseCategoriesForShift(Long shiftId) {
        log.debug("Finding expense categories applicable for shift {}", shiftId);

        CabShift shift = cabShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

        if (shift.getCurrentProfile() == null) {
            log.debug("Shift {} has no profile assigned, returning empty list", shiftId);
            return List.of();
        }

        List<ExpenseCategory> categories = expenseCategoryRepository
                .findByShiftProfileId(shift.getCurrentProfile().getId());
        log.debug("Found {} expense categories for shift {}", categories.size(), shiftId);
        return categories;
    }

    /**
     * Get all active expense categories applicable to a shift
     */
    public List<ExpenseCategory> getActiveExpenseCategoriesForShift(Long shiftId) {
        log.debug("Finding active expense categories applicable for shift {}", shiftId);

        CabShift shift = cabShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

        if (shift.getCurrentProfile() == null) {
            return List.of();
        }

        List<ExpenseCategory> categories = expenseCategoryRepository
                .findActiveByShiftProfileId(shift.getCurrentProfile().getId());
        log.debug("Found {} active expense categories for shift {}", categories.size(), shiftId);
        return categories;
    }

    // ============================================================================
    // Preview & Impact Analysis
    // ============================================================================

    /**
     * Preview the impact of linking an expense category to a profile
     * Shows which shifts would be affected before confirming the link
     */
    public ProfileLinkingPreview previewProfileExpenseLink(Long categoryId, Long profileId) {
        log.info("Previewing link of category {} to profile {}", categoryId, profileId);

        ExpenseCategory category = expenseCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        ShiftProfile profile = shiftProfileService.getProfileById(profileId);

        // Get currently affected shifts (if already linked)
        List<CabShift> currentlyAffected = category.getShiftProfileId() != null
                ? getCabShiftsForExpenseCategory(categoryId)
                : List.of();

        // Get shifts that would be affected by new link
        List<CabShift> wouldBeAffected = cabShiftRepository.findByCurrentProfileId(profileId);

        // Calculate the difference
        List<CabShift> newlyAffected = wouldBeAffected.stream()
                .filter(shift -> !currentlyAffected.contains(shift))
                .toList();

        List<CabShift> noLongerAffected = currentlyAffected.stream()
                .filter(shift -> !wouldBeAffected.contains(shift))
                .toList();

        ProfileLinkingPreview preview = new ProfileLinkingPreview(
                categoryId,
                category.getCategoryName(),
                profileId,
                profile.getProfileName(),
                wouldBeAffected.size(),
                newlyAffected.size(),
                noLongerAffected.size(),
                currentlyAffected.size()
        );

        log.info("Preview: Linking {} would affect {} total shifts " +
                "(adding {}, removing {})",
                category.getCategoryCode(), wouldBeAffected.size(),
                newlyAffected.size(), noLongerAffected.size());

        return preview;
    }

    /**
     * Check if a shift would match a category based on profile
     */
    public boolean doesShiftMatchCategory(Long shiftId, Long categoryId) {
        log.debug("Checking if shift {} matches category {}", shiftId, categoryId);

        CabShift shift = cabShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

        ExpenseCategory category = expenseCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        // If category has no profile link, no profile-based match
        if (category.getShiftProfileId() == null) {
            return false;
        }

        // If shift has no profile, cannot match
        if (shift.getCurrentProfile() == null) {
            return false;
        }

        // Check if profiles match
        return shift.getCurrentProfile().getId().equals(category.getShiftProfileId());
    }

    // ============================================================================
    // Impact Analysis for Profile Changes
    // ============================================================================

    /**
     * Get all expense categories linked to a profile
     * Useful when deactivating or deleting a profile
     */
    public List<ExpenseCategory> getExpenseCategoriesForProfile(Long profileId) {
        log.debug("Finding expense categories for profile {}", profileId);
        List<ExpenseCategory> categories = expenseCategoryRepository.findByShiftProfileId(profileId);
        log.debug("Found {} categories for profile {}", categories.size(), profileId);
        return categories;
    }

    /**
     * Get count of expense categories using a profile
     */
    public long countExpenseCategoriesForProfile(Long profileId) {
        return getExpenseCategoriesForProfile(profileId).size();
    }

    // ============================================================================
    // DTOs
    // ============================================================================

    /**
     * Preview information for profile-category linking
     */
    public static class ProfileLinkingPreview {
        public final Long categoryId;
        public final String categoryName;
        public final Long profileId;
        public final String profileName;
        public final int totalShiftsAffected;  // Total shifts in new profile
        public final int newlyAffectedCount;   // Shifts newly affected (not in old profile)
        public final int noLongerAffectedCount; // Shifts no longer affected (in old but not new)
        public final int previouslyAffectedCount; // Shifts in old profile

        public ProfileLinkingPreview(
                Long categoryId, String categoryName,
                Long profileId, String profileName,
                int totalShiftsAffected, int newlyAffectedCount,
                int noLongerAffectedCount, int previouslyAffectedCount) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.profileId = profileId;
            this.profileName = profileName;
            this.totalShiftsAffected = totalShiftsAffected;
            this.newlyAffectedCount = newlyAffectedCount;
            this.noLongerAffectedCount = noLongerAffectedCount;
            this.previouslyAffectedCount = previouslyAffectedCount;
        }
    }
}
