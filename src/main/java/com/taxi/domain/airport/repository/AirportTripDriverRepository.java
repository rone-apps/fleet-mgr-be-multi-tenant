package com.taxi.domain.airport.repository;

import com.taxi.domain.airport.model.AirportTripDriver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AirportTripDriverRepository extends JpaRepository<AirportTripDriver, Long> {

    /**
     * Get all driver assignments for a specific airport trip
     */
    List<AirportTripDriver> findByAirportTripIdOrderByHour(Long airportTripId);

    /**
     * Get driver's trip assignments for a date range (for reports)
     */
    List<AirportTripDriver> findByDriverNumberAndTripDateBetweenOrderByTripDateAscHourAsc(
        String driverNumber, LocalDate startDate, LocalDate endDate);

    /**
     * Get total trips per driver for a date range (for summary reports)
     */
    @Query("SELECT atd.driverNumber, SUM(atd.tripCount) FROM AirportTripDriver atd " +
           "WHERE atd.tripDate BETWEEN :startDate AND :endDate " +
           "GROUP BY atd.driverNumber ORDER BY SUM(atd.tripCount) DESC")
    List<Object[]> getTripSummaryByDriver(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    /**
     * Get total trips for a specific driver in a date range
     */
    @Query("SELECT COALESCE(SUM(atd.tripCount), 0) FROM AirportTripDriver atd " +
           "WHERE atd.driverNumber = :driverNumber " +
           "AND atd.tripDate BETWEEN :startDate AND :endDate")
    int getTotalTripsForDriver(
        @Param("driverNumber") String driverNumber,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    /**
     * Get daily trip breakdown for a driver (for detailed reports)
     */
    @Query("SELECT atd.tripDate, SUM(atd.tripCount) FROM AirportTripDriver atd " +
           "WHERE atd.driverNumber = :driverNumber " +
           "AND atd.tripDate BETWEEN :startDate AND :endDate " +
           "GROUP BY atd.tripDate ORDER BY atd.tripDate")
    List<Object[]> getDailyTripsForDriver(
        @Param("driverNumber") String driverNumber,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    /**
     * Get total trips for a cab in a date range, grouped by driver
     */
    @Query("SELECT atd.driverNumber, SUM(atd.tripCount) FROM AirportTripDriver atd " +
           "WHERE atd.cabNumber = :cabNumber " +
           "AND atd.tripDate BETWEEN :startDate AND :endDate " +
           "GROUP BY atd.driverNumber")
    List<Object[]> getTripsByDriverForCab(
        @Param("cabNumber") String cabNumber,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    /**
     * Delete all assignments for an airport trip (for re-import/overwrite)
     */
    @Modifying
    @Query("DELETE FROM AirportTripDriver atd WHERE atd.airportTrip.id = :airportTripId")
    void deleteByAirportTripId(@Param("airportTripId") Long airportTripId);

    /**
     * Check if assignments exist for an airport trip
     */
    boolean existsByAirportTripId(Long airportTripId);

    // ==================== Paginated queries for data view ====================

    @Query("SELECT atd FROM AirportTripDriver atd WHERE atd.tripDate BETWEEN :startDate AND :endDate ORDER BY atd.tripDate DESC, atd.cabNumber, atd.hour")
    Page<AirportTripDriver> findByTripDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable);

    @Query("SELECT atd FROM AirportTripDriver atd WHERE atd.tripDate BETWEEN :startDate AND :endDate AND atd.cabNumber = :cabNumber ORDER BY atd.tripDate DESC, atd.hour")
    Page<AirportTripDriver> findByTripDateBetweenAndCabNumber(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("cabNumber") String cabNumber,
        Pageable pageable);

    @Query("SELECT atd FROM AirportTripDriver atd WHERE atd.tripDate BETWEEN :startDate AND :endDate AND atd.driverNumber = :driverNumber ORDER BY atd.tripDate DESC, atd.hour")
    Page<AirportTripDriver> findByTripDateBetweenAndDriverNumber(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("driverNumber") String driverNumber,
        Pageable pageable);

    @Query("SELECT atd FROM AirportTripDriver atd WHERE atd.tripDate BETWEEN :startDate AND :endDate AND atd.cabNumber = :cabNumber AND atd.driverNumber = :driverNumber ORDER BY atd.tripDate DESC, atd.hour")
    Page<AirportTripDriver> findByTripDateBetweenAndCabNumberAndDriverNumber(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("cabNumber") String cabNumber,
        @Param("driverNumber") String driverNumber,
        Pageable pageable);
}
