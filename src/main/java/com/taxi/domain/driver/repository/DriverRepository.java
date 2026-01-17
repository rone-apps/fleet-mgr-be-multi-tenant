package com.taxi.domain.driver.repository;

import com.taxi.domain.driver.model.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Driver entity
 */
@Repository
public interface DriverRepository extends JpaRepository<Driver, Long>, JpaSpecificationExecutor<Driver> {

    Optional<Driver> findByDriverNumber(String driverNumber);

    Optional<Driver> findByUsername(String username);

    List<Driver> findByStatus(Driver.DriverStatus status);

    @Query("SELECT d FROM Driver d WHERE d.status = 'ACTIVE' ORDER BY d.lastName, d.firstName")
    List<Driver> findAllActiveDrivers();

    @Query("SELECT d FROM Driver d WHERE LOWER(d.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(d.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Driver> searchByName(@Param("name") String name);

    boolean existsByDriverNumber(String driverNumber);

    boolean existsByUsername(String username);

    /**
     * Find driver_number by username (for TaxiCaller import)
     */
    @Query("SELECT d.driverNumber FROM Driver d WHERE d.username = :username")
    Optional<String> findDriverNumberByUsername(@Param("username") String username);

    /**
     * Find driver_number by first name and last name (for TaxiCaller import)
     */
    @Query("SELECT d.driverNumber FROM Driver d WHERE LOWER(d.firstName) = LOWER(:firstName) AND LOWER(d.lastName) = LOWER(:lastName)")
    Optional<String> findDriverNumberByName(@Param("firstName") String firstName, @Param("lastName") String lastName);

   // Optional<Driver> findByDriverNumber(String driverNumber);
}
