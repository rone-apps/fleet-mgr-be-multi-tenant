package com.taxi.domain.cab.service;

import com.taxi.domain.cab.model.CabAttributeType;
import com.taxi.domain.cab.repository.CabAttributeTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Service for managing cab attribute types
 * Following ExpenseCategoryService pattern
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CabAttributeTypeService {

    private final CabAttributeTypeRepository attributeTypeRepository;

    public CabAttributeType createAttributeType(CabAttributeType attributeType) {
        log.info("Creating cab attribute type: {}", attributeType.getAttributeName());

        // Validate unique code
        if (attributeTypeRepository.existsByAttributeCode(attributeType.getAttributeCode())) {
            throw new RuntimeException("Attribute type with code already exists: " +
                attributeType.getAttributeCode());
        }

        return attributeTypeRepository.save(attributeType);
    }

    public CabAttributeType updateAttributeType(Long id, CabAttributeType updates) {
        log.info("Updating cab attribute type ID: {}", id);

        CabAttributeType attributeType = attributeTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + id));

        // Update fields (cannot change code)
        if (updates.getAttributeName() != null) {
            attributeType.setAttributeName(updates.getAttributeName());
        }
        if (updates.getDescription() != null) {
            attributeType.setDescription(updates.getDescription());
        }
        if (updates.getCategory() != null) {
            attributeType.setCategory(updates.getCategory());
        }
        if (updates.getValidationPattern() != null) {
            attributeType.setValidationPattern(updates.getValidationPattern());
        }
        if (updates.getHelpText() != null) {
            attributeType.setHelpText(updates.getHelpText());
        }

        return attributeTypeRepository.save(attributeType);
    }

    public void deleteAttributeType(Long id) {
        log.info("Deleting cab attribute type ID: {}", id);
        attributeTypeRepository.deleteById(id);
    }

    public void activateAttributeType(Long id) {
        CabAttributeType attributeType = attributeTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + id));
        attributeType.activate();
        attributeTypeRepository.save(attributeType);
    }

    public void deactivateAttributeType(Long id) {
        CabAttributeType attributeType = attributeTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + id));
        attributeType.deactivate();
        attributeTypeRepository.save(attributeType);
    }

    @Transactional(readOnly = true)
    public CabAttributeType getAttributeType(Long id) {
        return attributeTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + id));
    }

    @Transactional(readOnly = true)
    public CabAttributeType getAttributeTypeByCode(String code) {
        return attributeTypeRepository.findByAttributeCode(code)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + code));
    }

    @Transactional(readOnly = true)
    public List<CabAttributeType> getAllAttributeTypes() {
        return attributeTypeRepository.findAllByOrderByAttributeNameAsc();
    }

    @Transactional(readOnly = true)
    public List<CabAttributeType> getActiveAttributeTypes() {
        return attributeTypeRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<CabAttributeType> getAttributeTypesByCategory(
            CabAttributeType.AttributeCategory category) {
        return attributeTypeRepository.findByCategory(category);
    }
}
