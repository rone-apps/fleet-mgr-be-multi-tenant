package com.taxi.web.controller;

import com.taxi.domain.cab.model.CabAttributeValue;
import com.taxi.domain.cab.service.CabAttributeValueService;
import com.taxi.web.dto.cab.attribute.AssignAttributeRequest;
import com.taxi.web.dto.cab.attribute.CabAttributeValueDTO;
import com.taxi.web.dto.cab.attribute.UpdateAttributeValueRequest;
import com.taxi.web.dto.cab.attribute.EndAttributeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Cab Attribute Value operations
 * Following CabController pattern
 */
@RestController
@RequestMapping("/cabs/{cabId}/attributes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CabAttributeValueController {

    private final CabAttributeValueService attributeValueService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> assignAttribute(
            @PathVariable Long cabId,
            @Valid @RequestBody AssignAttributeRequest request) {
        log.info("POST /api/cabs/{}/attributes - Assign attribute", cabId);
        try {
            CabAttributeValue value = attributeValueService.assignAttribute(
                cabId,
                request.getAttributeTypeId(),
                request.getAttributeValue(),
                request.getStartDate(),
                request.getEndDate(),
                request.getNotes()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Attribute assigned successfully");
            response.put("attributeValue", CabAttributeValueDTO.fromEntity(value));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Failed to assign attribute", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{attributeValueId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateAttributeValue(
            @PathVariable Long cabId,
            @PathVariable Long attributeValueId,
            @Valid @RequestBody UpdateAttributeValueRequest request) {
        log.info("PUT /api/cabs/{}/attributes/{}", cabId, attributeValueId);
        try {
            CabAttributeValue value = attributeValueService.updateAttributeValue(
                attributeValueId,
                request.getAttributeValue(),
                request.getStartDate(),
                request.getEndDate(),
                request.getNotes()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Attribute value updated successfully");
            response.put("attributeValue", CabAttributeValueDTO.fromEntity(value));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to update attribute value", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{attributeValueId}/end")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> endAttribute(
            @PathVariable Long cabId,
            @PathVariable Long attributeValueId,
            @Valid @RequestBody EndAttributeRequest request) {
        log.info("PUT /api/cabs/{}/attributes/{}/end", cabId, attributeValueId);
        try {
            attributeValueService.endAttributeAssignment(attributeValueId, request.getEndDate());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Attribute assignment ended successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to end attribute", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CabAttributeValueDTO>> getCurrentAttributes(
            @PathVariable Long cabId) {
        log.info("GET /api/cabs/{}/attributes/current", cabId);
        List<CabAttributeValueDTO> attributes = attributeValueService.getCurrentAttributes(cabId)
                .stream()
                .map(CabAttributeValueDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(attributes);
    }

    @GetMapping("/on-date")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CabAttributeValueDTO>> getAttributesOnDate(
            @PathVariable Long cabId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("GET /api/cabs/{}/attributes/on-date?date={}", cabId, date);
        List<CabAttributeValueDTO> attributes = attributeValueService.getAttributesOnDate(cabId, date)
                .stream()
                .map(CabAttributeValueDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(attributes);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CabAttributeValueDTO>> getAttributeHistory(
            @PathVariable Long cabId) {
        log.info("GET /api/cabs/{}/attributes/history", cabId);
        List<CabAttributeValueDTO> history = attributeValueService.getAttributeHistory(cabId)
                .stream()
                .map(CabAttributeValueDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{attributeValueId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAttribute(
            @PathVariable Long cabId,
            @PathVariable Long attributeValueId) {
        log.info("DELETE /api/cabs/{}/attributes/{}", cabId, attributeValueId);
        try {
            attributeValueService.deleteAttributeValue(attributeValueId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Attribute deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to delete attribute", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
