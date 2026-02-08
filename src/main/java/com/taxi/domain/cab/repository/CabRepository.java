package com.taxi.domain.cab.repository;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.driver.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Cab entity
 */
@Repository
public interface CabRepository extends JpaRepository<Cab, Long> {

    Optional<Cab> findByCabNumber(String cabNumber);

    Optional<Cab> findByRegistrationNumber(String registrationNumber);

    // Note: cabType, status, hasAirportLicense are now at shift level, not cab level
    // Use CabShiftRepository to query by these attributes

    // Original method - might have lazy loading issues
    List<Cab> findByOwnerDriver(Driver ownerDriver);

    // RECOMMENDED: Query by owner driver ID (fixes lazy loading issues)
    @Query("SELECT c FROM Cab c WHERE c.ownerDriver.id = :ownerId")
    List<Cab> findByOwnerDriverId(@Param("ownerId") Long ownerId);

    // ALTERNATIVE: Query by driver number directly (most efficient)
    @Query("SELECT c FROM Cab c WHERE c.ownerDriver.driverNumber = :driverNumber")
    List<Cab> findByOwnerDriverNumber(@Param("driverNumber") String driverNumber);

    // Get all cabs (filtering by shift status should be done via CabShiftRepository)
    @Query("SELECT DISTINCT c FROM Cab c ORDER BY c.cabNumber")
    List<Cab> findAllOrderByNumber();

    boolean existsByCabNumber(String cabNumber);

    boolean existsByRegistrationNumber(String registrationNumber);
    
}