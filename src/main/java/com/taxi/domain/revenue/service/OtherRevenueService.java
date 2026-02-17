package com.taxi.domain.revenue.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.expense.model.ApplicationType;
import com.taxi.domain.revenue.entity.OtherRevenue;
import com.taxi.domain.revenue.entity.RevenueCategory;
import com.taxi.domain.revenue.repository.OtherRevenueRepository;
import com.taxi.domain.revenue.repository.RevenueCategoryRepository;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.web.dto.revenue.OtherRevenueDTO;
import com.taxi.web.dto.revenue.OtherRevenueRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtherRevenueService {
    
    private final OtherRevenueRepository revenueRepository;
    private final RevenueCategoryRepository categoryRepository;
    private final CabRepository cabRepository;
    private final DriverRepository driverRepository;
    private final CabShiftRepository cabShiftRepository;
    
    // Create revenue
    @Transactional
    public OtherRevenueDTO createRevenue(OtherRevenueRequest request) {
        OtherRevenue revenue = new OtherRevenue();
        mapRequestToEntity(request, revenue);
        validateRevenue(revenue);
        setEntityReferences(revenue);
        OtherRevenue saved = revenueRepository.save(revenue);
        log.info("Created other revenue with id: {}", saved.getId());
        return OtherRevenueDTO.fromEntity(saved);
    }
    
    // Update revenue
    @Transactional
    public OtherRevenueDTO updateRevenue(Long id, OtherRevenueRequest request) {
        OtherRevenue existing = revenueRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Revenue not found with id: " + id));
        
        mapRequestToEntity(request, existing);
        validateRevenue(existing);
        setEntityReferences(existing);
        
        OtherRevenue saved = revenueRepository.save(existing);
        log.info("Updated other revenue with id: {}", saved.getId());
        return OtherRevenueDTO.fromEntity(saved);
    }
    
    // Get revenue by ID
    @Transactional(readOnly = true)
    public OtherRevenueDTO getRevenueById(Long id) {
        OtherRevenue revenue = revenueRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Revenue not found with id: " + id));
        return OtherRevenueDTO.fromEntity(revenue);
    }
    
    // Get all revenues
    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getAllRevenues() {
        return revenueRepository.findAll().stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    // Get revenues between dates
    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getRevenuesBetweenDates(LocalDate startDate, LocalDate endDate) {
        return revenueRepository.findByRevenueDateBetween(startDate, endDate).stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getRevenuesForDriverBetweenDates(String driverNumber, LocalDate startDate, LocalDate endDate) {
        Driver driver = driverRepository.findByDriverNumber(driverNumber)
            .orElseThrow(() -> new RuntimeException("Driver not found with driverNumber: " + driverNumber));

        Long driverId = driver.getId();
        
        log.info("🔍 === OtherRevenue Search Debug ===");
        log.info("🔍 Input driverNumber: {}", driverNumber);
        log.info("🔍 Resolved driver.id: {}", driverId);
        log.info("🔍 Date range: {} to {}", startDate, endDate);
        
        // First, let's see ALL revenues in the date range (no driver filter)
        List<OtherRevenue> allInDateRange = revenueRepository.findByRevenueDateBetween(startDate, endDate);
        log.info("🔍 Total OtherRevenue records in date range (no driver filter): {}", allInDateRange.size());
        
        // Log what entity types and IDs exist
        for (OtherRevenue rev : allInDateRange) {
            log.info("🔍   -> id={}, entityType={}, entityId={}, driverId={}, ownerId={}, amount={}", 
                     rev.getId(), 
                     rev.getEntityType(), 
                     rev.getEntityId(),
                     rev.getDriver() != null ? rev.getDriver().getId() : "null",
                     rev.getOwner() != null ? rev.getOwner().getId() : "null",
                     rev.getAmount());
        }
        
        // Now try the filtered query
        List<OtherRevenue> results = revenueRepository.findForDriverBetweenDates(driverId, startDate, endDate);
        log.info("🔍 Filtered results for driverId={}: {} records", driverId, results.size());
        log.info("🔍 === End Debug ===");
        
        return results.stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    // Get revenues by category
    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getRevenuesByCategory(Long categoryId) {
        return revenueRepository.findByCategory_Id(categoryId).stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    // Get revenues by entity
    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getRevenuesByEntity(OtherRevenue.EntityType entityType, Long entityId) {
        return revenueRepository.findByEntityTypeAndEntityId(entityType, entityId).stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    // Get revenues by payment status
    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getRevenuesByPaymentStatus(OtherRevenue.PaymentStatus paymentStatus) {
        return revenueRepository.findByPaymentStatus(paymentStatus).stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    // Get revenues with filters
    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getRevenuesWithFilters(
            LocalDate startDate, 
            LocalDate endDate,
            Long categoryId,
            OtherRevenue.EntityType entityType,
            OtherRevenue.PaymentStatus paymentStatus) {
        return revenueRepository.findWithFilters(startDate, endDate, categoryId, entityType, paymentStatus).stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    // Mark revenue as paid
    @Transactional
    public OtherRevenueDTO markAsPaid(Long id, LocalDate paymentDate) {
        OtherRevenue revenue = revenueRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Revenue not found with id: " + id));
        
        revenue.setPaymentStatus(OtherRevenue.PaymentStatus.PAID);
        revenue.setPaymentDate(paymentDate);
        
        OtherRevenue saved = revenueRepository.save(revenue);
        log.info("Marked other revenue {} as paid", id);
        return OtherRevenueDTO.fromEntity(saved);
    }
    
    // Cancel revenue
    @Transactional
    public OtherRevenueDTO cancelRevenue(Long id) {
        OtherRevenue revenue = revenueRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Revenue not found with id: " + id));
        
        revenue.setPaymentStatus(OtherRevenue.PaymentStatus.CANCELLED);
        
        OtherRevenue saved = revenueRepository.save(revenue);
        log.info("Cancelled other revenue {}", id);
        return OtherRevenueDTO.fromEntity(saved);
    }
    
    // ✅ NEW: Application Type Query Methods

    // Get revenues by application type
    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getRevenuesByApplicationType(ApplicationType applicationType) {
        return revenueRepository.findByApplicationType(applicationType).stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }

    // Get revenues by application type and date range
    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getRevenuesByApplicationTypeBetweenDates(
            ApplicationType applicationType,
            LocalDate startDate,
            LocalDate endDate) {
        return revenueRepository.findByApplicationTypeAndRevenueDateBetween(applicationType, startDate, endDate).stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }

    // Get revenues for a specific shift profile
    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getRevenuesByShiftProfileBetweenDates(
            Long shiftProfileId,
            LocalDate startDate,
            LocalDate endDate) {
        return revenueRepository.findByShiftProfileIdAndRevenueDateBetween(shiftProfileId, startDate, endDate).stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }

    // Get revenues for a specific shift
    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getRevenuesBySpecificShiftBetweenDates(
            Long shiftId,
            LocalDate startDate,
            LocalDate endDate) {
        return revenueRepository.findBySpecificShiftIdAndRevenueDateBetween(shiftId, startDate, endDate).stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }

    // Get revenues for a specific person (driver/owner)
    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getRevenuesBySpecificPersonBetweenDates(
            Long personId,
            LocalDate startDate,
            LocalDate endDate) {
        return revenueRepository.findBySpecificPersonIdAndRevenueDateBetween(personId, startDate, endDate).stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }

    // Get revenues for shifts with a specific attribute
    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getRevenuesByAttributeTypeBetweenDates(
            Long attributeTypeId,
            LocalDate startDate,
            LocalDate endDate) {
        return revenueRepository.findByAttributeTypeIdAndRevenueDateBetween(attributeTypeId, startDate, endDate).stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }

    // Get all revenues applicable to a specific person between dates
    // Includes: SPECIFIC_PERSON, ALL_DRIVERS, ALL_OWNERS, ALL_ACTIVE_SHIFTS
    @Transactional(readOnly = true)
    public List<OtherRevenueDTO> getApplicableRevenuesBetweenDates(
            Long personId,
            LocalDate startDate,
            LocalDate endDate) {
        return revenueRepository.findApplicableRevenuesBetween(personId, startDate, endDate).stream()
            .map(OtherRevenueDTO::fromEntity)
            .collect(Collectors.toList());
    }

    // Get total revenue by application type and date range
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenueByApplicationType(
            ApplicationType applicationType,
            LocalDate startDate,
            LocalDate endDate) {
        BigDecimal total = revenueRepository.getTotalByApplicationTypeAndDateRange(applicationType, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    // Get total revenue for date range
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue(LocalDate startDate, LocalDate endDate) {
        BigDecimal total = revenueRepository.getTotalRevenueBetweenDates(startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    // Get total revenue by category
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenueByCategory(Long categoryId, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = revenueRepository.getTotalRevenueByCategoryAndDateRange(categoryId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    // Get total revenue by entity
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenueByEntity(
            OtherRevenue.EntityType entityType, 
            Long entityId,
            LocalDate startDate, 
            LocalDate endDate) {
        BigDecimal total = revenueRepository.getTotalRevenueByEntityTypeAndEntityIdAndRevenueDateBetween(entityType, entityId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    // Helper methods
    private void mapRequestToEntity(OtherRevenueRequest request, OtherRevenue revenue) {
        revenue.setAmount(request.getAmount());
        revenue.setRevenueDate(request.getRevenueDate());
        revenue.setDescription(request.getDescription());
        revenue.setReferenceNumber(request.getReferenceNumber());
        revenue.setPaymentMethod(request.getPaymentMethod());
        revenue.setPaymentDate(request.getPaymentDate());
        revenue.setNotes(request.getNotes());

        // Set entity type
        if (request.getEntityType() != null) {
            revenue.setEntityType(OtherRevenue.EntityType.valueOf(request.getEntityType().toUpperCase()));
        }
        revenue.setEntityId(request.getEntityId());

        // Set revenue type
        if (request.getRevenueType() != null) {
            revenue.setRevenueType(OtherRevenue.RevenueType.valueOf(request.getRevenueType().toUpperCase()));
        }

        // Set payment status
        if (request.getPaymentStatus() != null && !request.getPaymentStatus().isEmpty()) {
            revenue.setPaymentStatus(OtherRevenue.PaymentStatus.valueOf(request.getPaymentStatus().toUpperCase()));
        } else if (revenue.getPaymentStatus() == null) {
            revenue.setPaymentStatus(OtherRevenue.PaymentStatus.PENDING);
        }

        // Set category
        if (request.getCategoryId() != null) {
            RevenueCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Revenue category not found with id: " + request.getCategoryId()));
            revenue.setCategory(category);
        }

        // Set application type fields (new system)
        if (request.getApplicationType() != null) {
            revenue.setApplicationType(request.getApplicationType());
            revenue.setShiftProfileId(request.getShiftProfileId());
            revenue.setSpecificShiftId(request.getSpecificShiftId());
            revenue.setSpecificPersonId(request.getSpecificPersonId());
            revenue.setAttributeTypeId(request.getAttributeTypeId());
        }
    }
    
    private void validateRevenue(OtherRevenue revenue) {
        if (revenue.getAmount() == null || revenue.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Revenue amount must be positive");
        }

        if (revenue.getRevenueDate() == null) {
            throw new IllegalArgumentException("Revenue date is required");
        }

        // Entity type is required only if application type is not set
        if (revenue.getApplicationType() == null && revenue.getEntityType() == null) {
            throw new IllegalArgumentException("Either entity type (legacy) or application type is required");
        }

        // Entity ID is required only if application type is not set
        if (revenue.getApplicationType() == null && revenue.getEntityId() == null) {
            throw new IllegalArgumentException("Entity ID is required when using legacy entity type");
        }

        if (revenue.getRevenueType() == null) {
            throw new IllegalArgumentException("Revenue type is required");
        }

        // ✅ Category is optional when using ApplicationType system
        // But if provided, validate it exists and is active
        if (revenue.getCategory() != null && revenue.getCategory().getId() != null) {
            RevenueCategory category = categoryRepository.findById(revenue.getCategory().getId())
                .orElseThrow(() -> new RuntimeException("Revenue category not found"));

            if (!category.isActive()) {
                throw new IllegalArgumentException("Cannot create revenue with inactive category");
            }

            revenue.setCategory(category);
        }

        // Validate application type specific requirements (if application type is set)
        if (revenue.getApplicationType() != null) {
            switch (revenue.getApplicationType()) {
                case SHIFT_PROFILE:
                    if (revenue.getShiftProfileId() == null) {
                        throw new IllegalArgumentException("Shift Profile ID is required for SHIFT_PROFILE application type");
                    }
                    break;
                case SPECIFIC_SHIFT:
                    if (revenue.getSpecificShiftId() == null) {
                        throw new IllegalArgumentException("Specific Shift ID is required for SPECIFIC_SHIFT application type");
                    }
                    break;
                case SPECIFIC_PERSON:
                    if (revenue.getSpecificPersonId() == null) {
                        throw new IllegalArgumentException("Specific Person ID is required for SPECIFIC_PERSON application type");
                    }
                    break;
                case SHIFTS_WITH_ATTRIBUTE:
                    if (revenue.getAttributeTypeId() == null) {
                        throw new IllegalArgumentException("Attribute Type ID is required for SHIFTS_WITH_ATTRIBUTE application type");
                    }
                    break;
                case ALL_OWNERS:
                case ALL_DRIVERS:
                case ALL_ACTIVE_SHIFTS:
                    // No additional validation needed - these apply globally
                    break;
            }
        }
    }
    
    private void setEntityReferences(OtherRevenue revenue) {
        // Clear all entity references first
        revenue.setCab(null);
        revenue.setDriver(null);
        revenue.setOwner(null);
        revenue.setShift(null);

        // ✅ Only set entity references if using legacy entity_type system
        // New ApplicationType system doesn't require these references
        if (revenue.getEntityType() == null) {
            return;
        }

        // Set the appropriate reference based on entity type (legacy system)
        switch (revenue.getEntityType()) {
            case CAB:
                Cab cab = cabRepository.findById(revenue.getEntityId())
                    .orElseThrow(() -> new RuntimeException("Cab not found with id: " + revenue.getEntityId()));
                revenue.setCab(cab);
                break;

            case DRIVER:
                Driver driver = driverRepository.findById(revenue.getEntityId())
                    .orElseThrow(() -> new RuntimeException("Driver not found with id: " + revenue.getEntityId()));
                revenue.setDriver(driver);
                break;

            case OWNER:
                Driver owner = driverRepository.findById(revenue.getEntityId())
                    .orElseThrow(() -> new RuntimeException("Owner not found with id: " + revenue.getEntityId()));
                revenue.setOwner(owner);
                break;

            case SHIFT:
                CabShift shift = cabShiftRepository.findById(revenue.getEntityId())
                    .orElseThrow(() -> new RuntimeException("Shift not found with id: " + revenue.getEntityId()));
                revenue.setShift(shift);
                break;

            case COMPANY:
                // No specific entity reference needed for company
                break;
        }
    }
}