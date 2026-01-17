package com.taxi.domain.shift.repository;

import com.taxi.domain.shift.model.DriverShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverShiftRepository extends JpaRepository<DriverShift, Long> {

    /**
     * Find active shift for a specific driver
     */
    Optional<DriverShift> findByDriverNumberAndStatus(String driverNumber, String status);

    /**
     * Find all active shifts
     */
    List<DriverShift> findByStatus(String status);

    /**
     * Find all shifts for a driver
     */
    List<DriverShift> findByDriverNumberOrderByLogonTimeDesc(String driverNumber);

    /**
     * Find shifts for a driver in a date range
     */
    @Query("SELECT ds FROM DriverShift ds WHERE ds.driverNumber = :driverNumber " +
           "AND DATE(ds.logonTime) BETWEEN :startDate AND :endDate " +
           "ORDER BY ds.logonTime DESC")
    List<DriverShift> findByDriverNumberAndDateRange(
        @Param("driverNumber") String driverNumber,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all shifts in a date range
     */
    @Query("SELECT ds FROM DriverShift ds WHERE DATE(ds.logonTime) BETWEEN :startDate AND :endDate " +
           "ORDER BY ds.logonTime DESC")
    List<DriverShift> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find completed shifts for a driver
     */
    List<DriverShift> findByDriverNumberAndStatusOrderByLogonTimeDesc(String driverNumber, String status);

    /**
     * Check if driver has an active shift
     */
    boolean existsByDriverNumberAndStatus(String driverNumber, String status);

    /**
     * Get shift summary for a driver in date range
     */
    @Query("SELECT " +
           "SUM(ds.totalHours) as totalHours, " +
           "SUM(CASE WHEN ds.primaryShiftType = 'DAY' THEN ds.primaryShiftCount ELSE 0 END + " +
           "    CASE WHEN ds.secondaryShiftType = 'DAY' THEN ds.secondaryShiftCount ELSE 0 END) as dayShifts, " +
           "SUM(CASE WHEN ds.primaryShiftType = 'NIGHT' THEN ds.primaryShiftCount ELSE 0 END + " +
           "    CASE WHEN ds.secondaryShiftType = 'NIGHT' THEN ds.secondaryShiftCount ELSE 0 END) as nightShifts, " +
           "SUM(ds.totalTrips) as totalTrips, " +
           "SUM(ds.totalRevenue) as totalRevenue, " +
           "SUM(ds.totalDistance) as totalDistance " +
           "FROM DriverShift ds " +
           "WHERE ds.driverNumber = :driverNumber " +
           "AND ds.status = 'COMPLETED' " +
           "AND DATE(ds.logonTime) BETWEEN :startDate AND :endDate")
    Object[] getDriverShiftSummary(
        @Param("driverNumber") String driverNumber,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find shifts by cab_number in date range
     */
    @Query("SELECT ds FROM DriverShift ds WHERE ds.cabNumber = :cabNumber " +
           "AND DATE(ds.logonTime) BETWEEN :startDate AND :endDate " +
           "ORDER BY ds.logonTime DESC")
    List<DriverShift> findByCabNumberAndDateRange(
        @Param("cabNumber") String cabNumber,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Count active shifts
     */
    @Query("SELECT COUNT(ds) FROM DriverShift ds WHERE ds.status = 'ACTIVE'")
    long countActiveShifts();

    /**
     * Get all driver IDs with active shifts
     */
    @Query("SELECT DISTINCT ds.driverNumber FROM DriverShift ds WHERE ds.status = 'ACTIVE'")
    List<Long> findActiveDriverIds();
    
    /**
     * Check if shift exists by driver username and logon time (for TaxiCaller import)
     */
    boolean existsByDriverUsernameAndLogonTime(String driverUsername, LocalDateTime logonTime);
    
    /**
     * Find shifts by cab number and logon time range (for lease revenue reports)
     */
    @Query("SELECT ds FROM DriverShift ds WHERE ds.cabNumber = :cabNumber " +
           "AND ds.logonTime BETWEEN :startDateTime AND :endDateTime " +
           "ORDER BY ds.logonTime ASC")
    List<DriverShift> findByCabNumberAndLogonTimeBetween(
        @Param("cabNumber") String cabNumber,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );
    
    /**
     * Find shifts by driver number and logon time range (for lease expense reports)
     */
    @Query("SELECT ds FROM DriverShift ds WHERE ds.driverNumber = :driverNumber " +
           "AND ds.logonTime BETWEEN :startDateTime AND :endDateTime " +
           "ORDER BY ds.logonTime ASC")
    List<DriverShift> findByDriverNumberAndLogonTimeBetween(
        @Param("driverNumber") String driverNumber,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("SELECT ds FROM DriverShift ds WHERE ds.cabNumber = :cabNumber " +
           "AND DATE(ds.logonTime) = :shiftDate " +
           "ORDER BY ds.logonTime DESC")
    List<DriverShift> findByCabNumberAndShiftDate(
        @Param("cabNumber") String cabNumber,
        @Param("shiftDate") LocalDate shiftDate
    );
    
    /**
     * Find all shifts in a datetime range (for bulk reporting)
     * This is optimized for driver summary reports
     */
    @Query("SELECT ds FROM DriverShift ds WHERE ds.logonTime BETWEEN :startDateTime AND :endDateTime " +
           "ORDER BY ds.driverNumber, ds.logonTime ASC")
    List<DriverShift> findByLogonTimeBetween(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );
    
    /**
     * Find shifts for specific drivers in a datetime range (for paginated reporting)
     */
    @Query("SELECT ds FROM DriverShift ds WHERE ds.driverNumber IN :driverNumbers " +
           "AND ds.logonTime BETWEEN :startDateTime AND :endDateTime " +
           "ORDER BY ds.driverNumber, ds.logonTime ASC")
    List<DriverShift> findByDriverNumberInAndLogonTimeBetween(
        @Param("driverNumbers") List<String> driverNumbers,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );
}