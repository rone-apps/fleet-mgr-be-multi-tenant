package com.taxi.web.controller;

import com.taxi.domain.expense.service.ItemRateService;
import com.taxi.web.dto.expense.CreateItemRateOverrideRequest;
import com.taxi.web.dto.expense.CreateItemRateRequest;
import com.taxi.web.dto.expense.ItemRateDTO;
import com.taxi.web.dto.expense.ItemRateOverrideDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/item-rates")
@RequiredArgsConstructor
@Slf4j
public class ItemRateController {

    private final ItemRateService itemRateService;

    // ===== ITEM RATE ENDPOINTS =====

    /**
     * GET /api/item-rates
     * Get all active item rates
     */
    @GetMapping
    public ResponseEntity<List<ItemRateDTO>> getAllRates() {
        log.info("GET all active item rates");
        List<ItemRateDTO> rates = itemRateService.getAllActiveRates();
        return ResponseEntity.ok(rates);
    }

    /**
     * POST /api/item-rates
     * Create a new item rate
     */
    @PostMapping
    public ResponseEntity<ItemRateDTO> createRate(@RequestBody CreateItemRateRequest request) {
        log.info("POST create item rate: {}", request.getName());
        ItemRateDTO createdRate = itemRateService.createRate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRate);
    }

    /**
     * PUT /api/item-rates/{rateId}
     * Update an item rate (creates new version)
     */
    @PutMapping("/{rateId}")
    public ResponseEntity<ItemRateDTO> updateRate(
            @PathVariable Long rateId,
            @RequestBody CreateItemRateRequest request) {
        log.info("PUT update item rate {}", rateId);
        ItemRateDTO updatedRate = itemRateService.updateRate(rateId, request);
        return ResponseEntity.ok(updatedRate);
    }

    /**
     * DELETE /api/item-rates/{rateId}
     * Deactivate an item rate
     */
    @DeleteMapping("/{rateId}")
    public ResponseEntity<Void> deactivateRate(@PathVariable Long rateId) {
        log.info("DELETE deactivate item rate {}", rateId);
        itemRateService.deactivateRate(rateId);
        return ResponseEntity.noContent().build();
    }

    // ===== ITEM RATE OVERRIDE ENDPOINTS =====

    /**
     * GET /api/item-rates/overrides/{ownerDriverNumber}
     * Get all overrides for an owner
     */
    @GetMapping("/overrides/{ownerDriverNumber}")
    public ResponseEntity<List<ItemRateOverrideDTO>> getOwnerOverrides(
            @PathVariable String ownerDriverNumber) {
        log.info("GET overrides for owner {}", ownerDriverNumber);
        List<ItemRateOverrideDTO> overrides = itemRateService.getOwnerOverrides(ownerDriverNumber);
        return ResponseEntity.ok(overrides);
    }

    /**
     * POST /api/item-rates/overrides
     * Create a new override
     */
    @PostMapping("/overrides")
    public ResponseEntity<ItemRateOverrideDTO> createOverride(
            @RequestBody CreateItemRateOverrideRequest request) {
        log.info("POST create override for owner {}", request.getOwnerDriverNumber());
        ItemRateOverrideDTO createdOverride = itemRateService.createOverride(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOverride);
    }

    /**
     * PUT /api/item-rates/overrides/{overrideId}
     * Update an override
     */
    @PutMapping("/overrides/{overrideId}")
    public ResponseEntity<ItemRateOverrideDTO> updateOverride(
            @PathVariable Long overrideId,
            @RequestBody CreateItemRateOverrideRequest request) {
        log.info("PUT update override {}", overrideId);
        ItemRateOverrideDTO updatedOverride = itemRateService.updateOverride(overrideId, request);
        return ResponseEntity.ok(updatedOverride);
    }

    /**
     * DELETE /api/item-rates/overrides/{overrideId}
     * Deactivate an override
     */
    @DeleteMapping("/overrides/{overrideId}")
    public ResponseEntity<Void> deactivateOverride(@PathVariable Long overrideId) {
        log.info("DELETE deactivate override {}", overrideId);
        itemRateService.deactivateOverride(overrideId);
        return ResponseEntity.noContent().build();
    }
}
