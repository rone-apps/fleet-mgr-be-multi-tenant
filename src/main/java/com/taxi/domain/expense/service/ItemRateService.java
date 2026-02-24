package com.taxi.domain.expense.service;

import com.taxi.domain.expense.model.ItemRate;
import com.taxi.domain.expense.model.ItemRateOverride;
import com.taxi.domain.expense.repository.ItemRateRepository;
import com.taxi.domain.expense.repository.ItemRateOverrideRepository;
import com.taxi.web.dto.expense.ItemRateDTO;
import com.taxi.web.dto.expense.ItemRateOverrideDTO;
import com.taxi.web.dto.expense.CreateItemRateRequest;
import com.taxi.web.dto.expense.CreateItemRateOverrideRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ItemRateService {

    private final ItemRateRepository itemRateRepository;
    private final ItemRateOverrideRepository itemRateOverrideRepository;

    // ===== ITEM RATE OPERATIONS =====

    /**
     * Get all active item rates
     */
    public List<ItemRateDTO> getAllActiveRates() {
        log.info("Fetching all active item rates");
        return itemRateRepository.findByIsActiveTrueOrderByName()
                .stream()
                .map(this::mapItemRateToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new item rate
     */
    @Transactional
    public ItemRateDTO createRate(CreateItemRateRequest request) {
        log.info("Creating new item rate: name={}, unitType={}, rate={}",
                request.getName(), request.getUnitType(), request.getRate());

        ItemRate rate = ItemRate.builder()
                .name(request.getName())
                .unitType(request.getUnitType())
                .rate(request.getRate())
                .chargedTo(request.getChargedTo())
                .effectiveFrom(request.getEffectiveFrom())
                .isActive(true)
                .notes(request.getNotes())
                .build();

        rate = itemRateRepository.save(rate);
        log.info("Created item rate ID {} for {}", rate.getId(), request.getName());

        return mapItemRateToDTO(rate);
    }

    /**
     * Update an item rate (creates new version)
     */
    @Transactional
    public ItemRateDTO updateRate(Long rateId, CreateItemRateRequest request) {
        log.info("Updating item rate {}: new rate={}, effectiveFrom={}",
                rateId, request.getRate(), request.getEffectiveFrom());

        ItemRate oldRate = itemRateRepository.findById(rateId)
                .orElseThrow(() -> new RuntimeException("Item rate not found: " + rateId));

        // Close off the old version
        oldRate.setEffectiveTo(request.getEffectiveFrom().minusDays(1));
        oldRate.setIsActive(false);
        itemRateRepository.save(oldRate);

        // Create new version
        ItemRate newRate = ItemRate.builder()
                .name(oldRate.getName())
                .unitType(oldRate.getUnitType())
                .rate(request.getRate())
                .chargedTo(request.getChargedTo())
                .effectiveFrom(request.getEffectiveFrom())
                .isActive(true)
                .notes(request.getNotes())
                .build();

        newRate = itemRateRepository.save(newRate);
        log.info("Created new version of rate (ID {}), closed old version effective to {}",
                newRate.getId(), oldRate.getEffectiveTo());

        return mapItemRateToDTO(newRate);
    }

    /**
     * Deactivate an item rate
     */
    @Transactional
    public void deactivateRate(Long rateId) {
        log.info("Deactivating item rate {}", rateId);

        ItemRate rate = itemRateRepository.findById(rateId)
                .orElseThrow(() -> new RuntimeException("Item rate not found: " + rateId));

        rate.setIsActive(false);
        rate.setEffectiveTo(LocalDate.now().minusDays(1));
        itemRateRepository.save(rate);

        log.info("Deactivated item rate {}", rateId);
    }

    // ===== ITEM RATE OVERRIDE OPERATIONS =====

    /**
     * Get all active overrides for an owner
     */
    public List<ItemRateOverrideDTO> getOwnerOverrides(String ownerDriverNumber) {
        log.info("Fetching active overrides for owner {}", ownerDriverNumber);
        return itemRateOverrideRepository.findByOwnerDriverNumberAndIsActiveTrueOrderByPriorityDesc(ownerDriverNumber)
                .stream()
                .map(this::mapOverrideToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new override
     */
    @Transactional
    public ItemRateOverrideDTO createOverride(CreateItemRateOverrideRequest request) {
        log.info("Creating override for owner {} on item rate {}",
                request.getOwnerDriverNumber(), request.getItemRateId());

        ItemRate itemRate = itemRateRepository.findById(request.getItemRateId())
                .orElseThrow(() -> new RuntimeException("Item rate not found: " + request.getItemRateId()));

        ItemRateOverride override = ItemRateOverride.builder()
                .itemRate(itemRate)
                .ownerDriverNumber(request.getOwnerDriverNumber())
                .cabNumber(request.getCabNumber())
                .shiftType(request.getShiftType())
                .dayOfWeek(request.getDayOfWeek())
                .overrideRate(request.getOverrideRate())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .notes(request.getNotes())
                .isActive(true)
                .build();

        // Auto-calculate priority based on specificity
        override.calculatePriority();

        override = itemRateOverrideRepository.save(override);
        log.info("Created override ID {} for owner {}", override.getId(), request.getOwnerDriverNumber());

        return mapOverrideToDTO(override);
    }

    /**
     * Update an override
     */
    @Transactional
    public ItemRateOverrideDTO updateOverride(Long overrideId, CreateItemRateOverrideRequest request) {
        log.info("Updating override {}", overrideId);

        ItemRateOverride override = itemRateOverrideRepository.findById(overrideId)
                .orElseThrow(() -> new RuntimeException("Override not found: " + overrideId));

        override.setOverrideRate(request.getOverrideRate());
        override.setCabNumber(request.getCabNumber());
        override.setShiftType(request.getShiftType());
        override.setDayOfWeek(request.getDayOfWeek());
        override.setStartDate(request.getStartDate());
        override.setEndDate(request.getEndDate());
        override.setNotes(request.getNotes());

        // Recalculate priority
        override.calculatePriority();

        override = itemRateOverrideRepository.save(override);
        log.info("Updated override {}", overrideId);

        return mapOverrideToDTO(override);
    }

    /**
     * Deactivate an override
     */
    @Transactional
    public void deactivateOverride(Long overrideId) {
        log.info("Deactivating override {}", overrideId);

        ItemRateOverride override = itemRateOverrideRepository.findById(overrideId)
                .orElseThrow(() -> new RuntimeException("Override not found: " + overrideId));

        override.setIsActive(false);
        itemRateOverrideRepository.save(override);

        log.info("Deactivated override {}", overrideId);
    }

    // ===== MAPPERS =====

    private ItemRateDTO mapItemRateToDTO(ItemRate rate) {
        return ItemRateDTO.builder()
                .id(rate.getId())
                .name(rate.getName())
                .unitType(rate.getUnitType())
                .unitTypeDisplay(rate.getUnitType() != null ? rate.getUnitType().getDisplayName() : "")
                .rate(rate.getRate())
                .chargedTo(rate.getChargedTo())
                .chargedToDisplay(rate.getChargedTo() != null ? rate.getChargedTo().getDisplayName() : "")
                .effectiveFrom(rate.getEffectiveFrom())
                .effectiveTo(rate.getEffectiveTo())
                .isActive(rate.getIsActive())
                .notes(rate.getNotes())
                .build();
    }

    private ItemRateOverrideDTO mapOverrideToDTO(ItemRateOverride override) {
        return ItemRateOverrideDTO.builder()
                .id(override.getId())
                .itemRateId(override.getItemRate().getId())
                .itemRateName(override.getItemRate().getName())
                .ownerDriverNumber(override.getOwnerDriverNumber())
                .cabNumber(override.getCabNumber())
                .shiftType(override.getShiftType())
                .dayOfWeek(override.getDayOfWeek())
                .overrideRate(override.getOverrideRate())
                .priority(override.getPriority())
                .startDate(override.getStartDate())
                .endDate(override.getEndDate())
                .isActive(override.getIsActive())
                .notes(override.getNotes())
                .build();
    }
}
