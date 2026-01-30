package com.taxi.web.controller;

import com.taxi.domain.cab.model.CabAttributeType;
import com.taxi.domain.cab.service.CabAttributeTypeService;
import com.taxi.web.dto.cab.attribute.CabAttributeTypeDTO;
import com.taxi.web.dto.cab.attribute.CreateCabAttributeTypeRequest;
import com.taxi.web.dto.cab.attribute.UpdateCabAttributeTypeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Cab Attribute Type operations
 * Following ExpenseCategoryController pattern
 */
@RestController
@RequestMapping("/cab-attribute-types")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CabAttributeTypeController {

    private final CabAttributeTypeService attributeTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createAttributeType(
            @Valid @RequestBody CreateCabAttributeTypeRequest request) {
        log.info("POST /api/cab-attribute-types - Create attribute type: {}",
            request.getAttributeName());
        try {
            CabAttributeType attributeType = request.toEntity();
            CabAttributeType created = attributeTypeService.createAttributeType(attributeType);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Attribute type created successfully");
            response.put("attributeType", CabAttributeTypeDTO.fromEntity(created));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Failed to create attribute type", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateAttributeType(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCabAttributeTypeRequest request) {
        log.info("PUT /api/cab-attribute-types/{} - Update attribute type", id);
        try {
            CabAttributeType updates = request.toEntity();
            CabAttributeType updated = attributeTypeService.updateAttributeType(id, updates);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Attribute type updated successfully");
            response.put("attributeType", CabAttributeTypeDTO.fromEntity(updated));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to update attribute type", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAttributeType(@PathVariable Long id) {
        log.info("DELETE /api/cab-attribute-types/{} - Delete attribute type", id);
        try {
            attributeTypeService.deleteAttributeType(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Attribute type deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to delete attribute type", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateAttributeType(@PathVariable Long id) {
        log.info("PUT /api/cab-attribute-types/{}/activate", id);
        try {
            attributeTypeService.activateAttributeType(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Attribute type activated successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to activate attribute type", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateAttributeType(@PathVariable Long id) {
        log.info("PUT /api/cab-attribute-types/{}/deactivate", id);
        try {
            attributeTypeService.deactivateAttributeType(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Attribute type deactivated successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to deactivate attribute type", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> getAttributeType(@PathVariable Long id) {
        log.info("GET /api/cab-attribute-types/{}", id);
        try {
            CabAttributeType attributeType = attributeTypeService.getAttributeType(id);
            return ResponseEntity.ok(CabAttributeTypeDTO.fromEntity(attributeType));
        } catch (RuntimeException e) {
            log.error("Attribute type not found: {}", id);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CabAttributeTypeDTO>> getAllAttributeTypes() {
        log.info("GET /api/cab-attribute-types - Get all attribute types");
        List<CabAttributeTypeDTO> attributeTypes = attributeTypeService.getAllAttributeTypes()
                .stream()
                .map(CabAttributeTypeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(attributeTypes);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CabAttributeTypeDTO>> getActiveAttributeTypes() {
        log.info("GET /api/cab-attribute-types/active");
        List<CabAttributeTypeDTO> attributeTypes = attributeTypeService.getActiveAttributeTypes()
                .stream()
                .map(CabAttributeTypeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(attributeTypes);
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> getAttributeTypesByCategory(@PathVariable String category) {
        log.info("GET /api/cab-attribute-types/category/{}", category);
        try {
            CabAttributeType.AttributeCategory cat =
                CabAttributeType.AttributeCategory.valueOf(category.toUpperCase());
            List<CabAttributeTypeDTO> attributeTypes =
                attributeTypeService.getAttributeTypesByCategory(cat)
                    .stream()
                    .map(CabAttributeTypeDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(attributeTypes);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid category: " + category);
            return ResponseEntity.badRequest().body(error);
        }
    }
}
