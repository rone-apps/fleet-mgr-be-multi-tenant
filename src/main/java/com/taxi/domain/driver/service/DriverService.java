package com.taxi.domain.driver.service;

import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.user.model.User;
import com.taxi.domain.user.repository.UserRepository;
import com.taxi.web.dto.driver.CreateDriverRequest;
import com.taxi.web.dto.driver.DriverDTO;
import com.taxi.web.dto.driver.UpdateDriverRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxi.infrastructure.multitenancy.TenantContext;
import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing drivers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get all drivers
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "drivers_all", key = "T(com.taxi.infrastructure.multitenancy.TenantContext).getCurrentTenant()")
    public List<DriverDTO> getAllDrivers() {
        log.info("Getting all drivers");
        List<Driver> drivers = driverRepository.findAll();

        Map<Long, User> userByDriverId = userRepository.findAllDriverUsers().stream()
                .filter(u -> u.getDriver() != null && u.getDriver().getId() != null)
                .collect(Collectors.toMap(u -> u.getDriver().getId(), Function.identity(), (a, b) -> a));

        return drivers.stream()
                .map(driver -> toDriverDTO(driver, userByDriverId.get(driver.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Get active drivers only
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "drivers_active", key = "T(com.taxi.infrastructure.multitenancy.TenantContext).getCurrentTenant()")
    public List<DriverDTO> getActiveDrivers() {
        log.info("Getting active drivers");
        List<Driver> drivers = driverRepository.findAllActiveDrivers();

        Map<Long, User> userByDriverId = userRepository.findAllDriverUsers().stream()
                .filter(u -> u.getDriver() != null && u.getDriver().getId() != null)
                .collect(Collectors.toMap(u -> u.getDriver().getId(), Function.identity(), (a, b) -> a));

        return drivers.stream()
                .map(driver -> toDriverDTO(driver, userByDriverId.get(driver.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Get drivers by status
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "drivers_status", key = "T(com.taxi.infrastructure.multitenancy.TenantContext).getCurrentTenant() + '_' + #status")
    public List<DriverDTO> getDriversByStatus(Driver.DriverStatus status) {
        log.info("Getting drivers with status: {}", status);
        List<Driver> drivers = driverRepository.findByStatus(status);

        Map<Long, User> userByDriverId = userRepository.findAllDriverUsers().stream()
                .filter(u -> u.getDriver() != null && u.getDriver().getId() != null)
                .collect(Collectors.toMap(u -> u.getDriver().getId(), Function.identity(), (a, b) -> a));

        return drivers.stream()
                .map(driver -> toDriverDTO(driver, userByDriverId.get(driver.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Search drivers by name
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "drivers_search", key = "T(com.taxi.infrastructure.multitenancy.TenantContext).getCurrentTenant() + '_' + #name")
    public List<DriverDTO> searchDriversByName(String name) {
        log.info("Searching drivers by name: {}", name);
        List<Driver> drivers = driverRepository.searchByName(name);

        Map<Long, User> userByDriverId = userRepository.findAllDriverUsers().stream()
                .filter(u -> u.getDriver() != null && u.getDriver().getId() != null)
                .collect(Collectors.toMap(u -> u.getDriver().getId(), Function.identity(), (a, b) -> a));

        return drivers.stream()
                .map(driver -> toDriverDTO(driver, userByDriverId.get(driver.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Get driver by ID
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "driver_by_id", key = "T(com.taxi.infrastructure.multitenancy.TenantContext).getCurrentTenant() + '_' + #id")
    public DriverDTO getDriverById(Long id) {
        log.info("Getting driver by ID: {}", id);
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + id));

        User user = userRepository.findByDriver(driver).orElse(null);
        return toDriverDTO(driver, user);
    }

    /**
     * Get driver by driver number
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "driver_by_number", key = "T(com.taxi.infrastructure.multitenancy.TenantContext).getCurrentTenant() + '_' + #driverNumber")
    public DriverDTO getDriverByDriverNumber(String driverNumber) {
        log.info("Getting driver by driver number: {}", driverNumber);
        Driver driver = driverRepository.findByDriverNumber(driverNumber)
                .orElseThrow(() -> new RuntimeException("Driver not found with number: " + driverNumber));

        User user = userRepository.findByDriver(driver).orElse(null);
        return toDriverDTO(driver, user);
    }

    /**
     * Create a new driver
     */
    @Transactional
    @CacheEvict(cacheNames = {
            "drivers_all",
            "drivers_active",
            "drivers_status",
            "drivers_search",
            "driver_by_id",
            "driver_by_number"
    }, allEntries = true)
    public DriverDTO createDriver(CreateDriverRequest request) {
        log.info("Creating new driver: {} {}", request.getFirstName(), request.getLastName());

        // Use provided driver number or generate one
        String driverNumber = (request.getDriverNumber() != null && !request.getDriverNumber().isBlank())
                ? request.getDriverNumber()
                : generateDriverNumber();

        // Create driver entity
        Driver driver = Driver.builder()
                .driverNumber(driverNumber)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .licenseNumber(request.getLicenseNumber())
                .licenseExpiry(request.getLicenseExpiry())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .joinedDate(request.getJoinedDate())
                .notes(request.getNotes())
                .isOwner(Boolean.TRUE.equals(request.getIsOwner()))
                .sin(request.getSin())
                .gstNumber(request.getGstNumber())
                .depositAmount(request.getDepositAmount())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .emergencyContactRelationship(request.getEmergencyContactRelationship())
                .securityDepositDate(request.getSecurityDepositDate())
                .refundDate(request.getRefundDate())
                .picDate(request.getPicDate())
                .ibcRecordsDate(request.getIbcRecordsDate())
                .status(Driver.DriverStatus.ACTIVE)
                .build();

        driver = driverRepository.save(driver);
        log.info("Driver created with ID: {} and number: {}", driver.getId(), driver.getDriverNumber());

        // Create user account if requested
        if (Boolean.TRUE.equals(request.getCreateUser()) && 
            request.getUsername() != null && 
            request.getPassword() != null) {
            
            createUserForDriver(driver, request.getUsername(), request.getPassword());
        }

        return getDriverById(driver.getId());
    }

    /**
     * Update an existing driver
     */
    @Transactional
    @CacheEvict(cacheNames = {
            "drivers_all",
            "drivers_active",
            "drivers_status",
            "drivers_search",
            "driver_by_id",
            "driver_by_number"
    }, allEntries = true)
    public DriverDTO updateDriver(Long id, UpdateDriverRequest request) {
        log.info("Updating driver with ID: {}", id);
        
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + id));

        // Update fields if provided
        if (request.getFirstName() != null) {
            driver.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            driver.setLastName(request.getLastName());
        }
        if (request.getLicenseNumber() != null) {
            driver.setLicenseNumber(request.getLicenseNumber());
        }
        if (request.getLicenseExpiry() != null) {
            driver.setLicenseExpiry(request.getLicenseExpiry());
        }
        if (request.getPhone() != null) {
            driver.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            driver.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            driver.setAddress(request.getAddress());
        }
        if (request.getJoinedDate() != null) {
            driver.setJoinedDate(request.getJoinedDate());
        }
        if (request.getNotes() != null) {
            driver.setNotes(request.getNotes());
        }
        if (request.getIsOwner() != null) {
            driver.setIsOwner(request.getIsOwner());
        }
        if (request.getSin() != null) {
            driver.setSin(request.getSin());
        }
        if (request.getGstNumber() != null) {
            driver.setGstNumber(request.getGstNumber());
        }
        if (request.getDepositAmount() != null) {
            driver.setDepositAmount(request.getDepositAmount());
        }
        if (request.getEmergencyContactName() != null) {
            driver.setEmergencyContactName(request.getEmergencyContactName());
        }
        if (request.getEmergencyContactPhone() != null) {
            driver.setEmergencyContactPhone(request.getEmergencyContactPhone());
        }
        if (request.getEmergencyContactRelationship() != null) {
            driver.setEmergencyContactRelationship(request.getEmergencyContactRelationship());
        }
        if (request.getSecurityDepositDate() != null) {
            driver.setSecurityDepositDate(request.getSecurityDepositDate());
        }
        if (request.getRefundDate() != null) {
            driver.setRefundDate(request.getRefundDate());
        }
        if (request.getPicDate() != null) {
            driver.setPicDate(request.getPicDate());
        }
        if (request.getIbcRecordsDate() != null) {
            driver.setIbcRecordsDate(request.getIbcRecordsDate());
        }
        if (request.getStatus() != null) {
            try {
                driver.setStatus(Driver.DriverStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status: " + request.getStatus());
            }
        }

        driver = driverRepository.save(driver);
        log.info("Driver updated: {}", driver.getDriverNumber());

        return getDriverById(driver.getId());
    }

    /**
     * Delete a driver (soft delete by setting status to TERMINATED)
     */
    @Transactional
    @CacheEvict(cacheNames = {
            "drivers_all",
            "drivers_active",
            "drivers_status",
            "drivers_search",
            "driver_by_id",
            "driver_by_number"
    }, allEntries = true)
    public void deleteDriver(Long id) {
        log.info("Deleting driver with ID: {}", id);
        
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + id));

        // Soft delete: set status to TERMINATED
        driver.setStatus(Driver.DriverStatus.TERMINATED);
        driverRepository.save(driver);
        
        log.info("Driver terminated: {}", driver.getDriverNumber());
    }

    /**
     * Activate a driver
     */
    @Transactional
    @CacheEvict(cacheNames = {
            "drivers_all",
            "drivers_active",
            "drivers_status",
            "drivers_search",
            "driver_by_id",
            "driver_by_number"
    }, allEntries = true)
    public DriverDTO activateDriver(Long id) {
        log.info("Activating driver with ID: {}", id);
        
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + id));

        driver.activate();
        driverRepository.save(driver);
        
        log.info("Driver activated: {}", driver.getDriverNumber());
        return getDriverById(driver.getId());
    }

    /**
     * Suspend a driver
     */
    @Transactional
    @CacheEvict(cacheNames = {
            "drivers_all",
            "drivers_active",
            "drivers_status",
            "drivers_search",
            "driver_by_id",
            "driver_by_number"
    }, allEntries = true)
    public DriverDTO suspendDriver(Long id) {
        log.info("Suspending driver with ID: {}", id);
        
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + id));

        driver.suspend();
        driverRepository.save(driver);
        
        log.info("Driver suspended: {}", driver.getDriverNumber());
        return getDriverById(driver.getId());
    }

    /**
     * Generate a unique driver number
     */
    private String generateDriverNumber() {
        long count = driverRepository.count();
        String driverNumber;
        int counter = (int) count + 1;
        
        do {
            driverNumber = String.format("DRV-%03d", counter);
            counter++;
        } while (driverRepository.existsByDriverNumber(driverNumber));
        
        return driverNumber;
    }

    /**
     * Create a user account for a driver
     */
    private void createUserForDriver(Driver driver, String username, String password) {
        log.info("Creating user account for driver: {}", driver.getDriverNumber());

        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }

        // Create user
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(driver.getEmail())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .phone(driver.getPhone())
                .role(User.UserRole.DRIVER)
                .isActive(true)
                .driver(driver)
                .build();

        userRepository.save(user);
        log.info("User account created for driver: {}", driver.getDriverNumber());
    }

    private DriverDTO toDriverDTO(Driver driver, User user) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        if (user != null) {
            return DriverDTO.fromEntityWithUser(driver, user.getId(), user.getUsername());
        }
        return DriverDTO.fromEntity(driver);
    }
}
