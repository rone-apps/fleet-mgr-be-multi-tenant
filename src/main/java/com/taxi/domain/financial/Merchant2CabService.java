package com.taxi.domain.financial;

import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.driver.model.Driver;
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
public class Merchant2CabService {
    
    private final Merchant2CabRepository merchant2CabRepository;
    private final CabRepository cabRepository;
    
    @Transactional(readOnly = true)
    public List<Merchant2CabDTO> getAllActiveMappings() {
        return merchant2CabRepository.findAllActiveMappings()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<Merchant2CabDTO> getAllMappings() {
        return merchant2CabRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .sorted((a, b) -> {
                    // Sort by active status first, then by cab number
                    if (a.isActive() != b.isActive()) {
                        return a.isActive() ? -1 : 1;
                    }
                    return a.getCabNumber().compareTo(b.getCabNumber());
                })
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<Merchant2CabDTO> getMappingsByCabNumber(String cabNumber) {
        return merchant2CabRepository.findByCabNumberOrderByStartDateDesc(cabNumber)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Merchant2CabDTO getCurrentMappingByCabNumber(String cabNumber) {
        return merchant2CabRepository.findCurrentMappingByCabNumber(cabNumber)
                .map(this::convertToDTO)
                .orElse(null);
    }
    
    @Transactional
    public Merchant2CabDTO createMapping(Merchant2CabDTO dto, String username) {
        // Validate cab exists
        cabRepository.findByCabNumber(dto.getCabNumber())
                .orElseThrow(() -> new IllegalArgumentException("Cab not found: " + dto.getCabNumber()));
        
        // Validate dates
        if (dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        
        // Check for overlapping mappings
        LocalDate endDate = dto.getEndDate() != null ? dto.getEndDate() : LocalDate.of(9999, 12, 31);
        if (merchant2CabRepository.hasOverlappingMapping(dto.getCabNumber(), dto.getStartDate(), endDate, 0L)) {
            throw new IllegalArgumentException("Overlapping mapping exists for this cab in the given date range");
        }
        
        // Auto-end current mapping if creating a new one starting today
        merchant2CabRepository.findCurrentMappingByCabNumber(dto.getCabNumber())
                .ifPresent(current -> {
                    if (dto.getStartDate().equals(LocalDate.now()) || 
                        dto.getStartDate().isBefore(LocalDate.now().plusDays(7))) {
                        LocalDate previousEndDate = dto.getStartDate().minusDays(1);
                        current.setEndDate(previousEndDate);
                        current.setUpdatedBy(username);
                        merchant2CabRepository.save(current);
                        log.info("Auto-ended previous mapping {} for cab {} on {}", 
                                current.getId(), dto.getCabNumber(), previousEndDate);
                    }
                });
        
        Merchant2Cab entity = convertToEntity(dto);
        entity.setCreatedBy(username);
        entity.setUpdatedBy(username);
        
        Merchant2Cab saved = merchant2CabRepository.save(entity);
        log.info("Created merchant2cab mapping: {} for cab {} to merchant {}", 
                 saved.getId(), saved.getCabNumber(), saved.getMerchantNumber());
        
        return convertToDTO(saved);
    }
    
    @Transactional
    public Merchant2CabDTO updateMapping(Long id, Merchant2CabDTO dto, String username) {
        Merchant2Cab existing = merchant2CabRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mapping not found: " + id));
        
        // Don't allow changing cab number on existing mapping
        if (!existing.getCabNumber().equals(dto.getCabNumber())) {
            throw new IllegalArgumentException("Cannot change cab number on existing mapping. Delete and create new instead.");
        }
        
        // Validate dates
        if (dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        
        // Check for overlapping mappings (excluding current)
        LocalDate endDate = dto.getEndDate() != null ? dto.getEndDate() : LocalDate.of(9999, 12, 31);
        if (merchant2CabRepository.hasOverlappingMapping(dto.getCabNumber(), dto.getStartDate(), endDate, id)) {
            throw new IllegalArgumentException("Overlapping mapping exists for this cab in the given date range");
        }
        
        existing.setMerchantNumber(dto.getMerchantNumber());
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        existing.setNotes(dto.getNotes());
        existing.setUpdatedBy(username);
        
        Merchant2Cab updated = merchant2CabRepository.save(existing);
        log.info("Updated merchant2cab mapping: {}", id);
        
        return convertToDTO(updated);
    }
    
    @Transactional
    public void endMapping(Long id, LocalDate endDate, String username) {
        Merchant2Cab mapping = merchant2CabRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mapping not found: " + id));
        
        if (endDate.isBefore(mapping.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        
        mapping.setEndDate(endDate);
        mapping.setUpdatedBy(username);
        merchant2CabRepository.save(mapping);
        
        log.info("Ended merchant2cab mapping: {} on {}", id, endDate);
    }
    
    @Transactional
    public void deleteMapping(Long id) {
        if (!merchant2CabRepository.existsById(id)) {
            throw new IllegalArgumentException("Mapping not found: " + id);
        }
        
        merchant2CabRepository.deleteById(id);
        log.info("Deleted merchant2cab mapping: {}", id);
    }
    
    private Merchant2CabDTO convertToDTO(Merchant2Cab entity) {
        Merchant2CabDTO dto = new Merchant2CabDTO();
        dto.setId(entity.getId());
        dto.setCabNumber(entity.getCabNumber());
        dto.setMerchantNumber(entity.getMerchantNumber());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setNotes(entity.getNotes());
        dto.setActive(entity.isActive());
        
        // Populate cab details
        cabRepository.findByCabNumber(entity.getCabNumber()).ifPresent(cab -> {
            dto.setMake(cab.getMake());
            dto.setModel(cab.getModel());
            dto.setYear(cab.getYear());
            dto.setColor(cab.getColor());
            dto.setRegistrationNumber(cab.getRegistrationNumber());
            dto.setCabType(cab.getCabType() != null ? cab.getCabType().toString() : null);
            dto.setCabStatus(cab.getStatus() != null ? cab.getStatus().toString() : null);
            
            // Populate owner driver details
            Driver owner = cab.getOwnerDriver();
            if (owner != null) {
                dto.setOwnerDriverId(owner.getId());
                dto.setOwnerDriverName(owner.getFirstName() + " " + owner.getLastName());
                dto.setOwnerDriverLicenseNumber(owner.getLicenseNumber());
            }
        });
        
        return dto;
    }
    
    private Merchant2Cab convertToEntity(Merchant2CabDTO dto) {
        Merchant2Cab entity = new Merchant2Cab();
        entity.setId(dto.getId());
        entity.setCabNumber(dto.getCabNumber());
        entity.setMerchantNumber(dto.getMerchantNumber());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setNotes(dto.getNotes());
        return entity;
    }
}
