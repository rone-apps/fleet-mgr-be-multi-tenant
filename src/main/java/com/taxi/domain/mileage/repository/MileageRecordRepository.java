package com.taxi.domain.mileage.repository;

import com.taxi.domain.mileage.model.MileageRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MileageRecordRepository extends JpaRepository<MileageRecord, Long> {

    List<MileageRecord> findByCabNumberOrderByLogonTimeDesc(String cabNumber);

    List<MileageRecord> findByDriverNumberOrderByLogonTimeDesc(String driverNumber);

    @Query("SELECT m FROM MileageRecord m WHERE DATE(m.logonTime) BETWEEN :startDate AND :endDate ORDER BY m.logonTime DESC")
    List<MileageRecord> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    /**
     * Paginated queries for data view (using logonTime for date filtering)
     */
    @Query("SELECT m FROM MileageRecord m WHERE DATE(m.logonTime) BETWEEN :startDate AND :endDate")
    Page<MileageRecord> findByLogonDateBetween(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate, 
        Pageable pageable);
    
    @Query("SELECT m FROM MileageRecord m WHERE DATE(m.logonTime) BETWEEN :startDate AND :endDate AND m.cabNumber = :cabNumber")
    Page<MileageRecord> findByLogonDateBetweenAndCabNumber(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate, 
        @Param("cabNumber") String cabNumber, 
        Pageable pageable);
    
    @Query("SELECT m FROM MileageRecord m WHERE DATE(m.logonTime) BETWEEN :startDate AND :endDate AND m.driverNumber = :driverNumber")
    Page<MileageRecord> findByLogonDateBetweenAndDriverNumber(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate, 
        @Param("driverNumber") String driverNumber, 
        Pageable pageable);
    
    @Query("SELECT m FROM MileageRecord m WHERE DATE(m.logonTime) BETWEEN :startDate AND :endDate " +
           "AND m.cabNumber = :cabNumber AND m.driverNumber = :driverNumber")
    Page<MileageRecord> findByLogonDateBetweenAndCabNumberAndDriverNumber(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate, 
        @Param("cabNumber") String cabNumber, 
        @Param("driverNumber") String driverNumber, 
        Pageable pageable);

    @Query("SELECT m FROM MileageRecord m WHERE m.cabNumber = :cabNumber " +
           "AND DATE(m.logonTime) BETWEEN :startDate AND :endDate ORDER BY m.logonTime DESC")
    List<MileageRecord> findByCabNumberAndDateRange(
        @Param("cabNumber") String cabNumber,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT m FROM MileageRecord m WHERE m.driverNumber = :driverNumber " +
           "AND DATE(m.logonTime) BETWEEN :startDate AND :endDate ORDER BY m.logonTime DESC")
    List<MileageRecord> findByDriverNumberAndDateRange(
        @Param("driverNumber") String driverNumber,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    boolean existsByCabNumberAndDriverNumberAndLogonTime(
        String cabNumber, String driverNumber, LocalDateTime logonTime);

    List<MileageRecord> findByUploadBatchIdOrderByLogonTimeDesc(String uploadBatchId);

    @Query("SELECT m.cabNumber, SUM(m.mileageA), SUM(m.mileageB), SUM(m.mileageC), " +
           "SUM(m.totalMileage), COUNT(m), SUM(m.shiftHours) " +
           "FROM MileageRecord m WHERE DATE(m.logonTime) BETWEEN :startDate AND :endDate " +
           "GROUP BY m.cabNumber ORDER BY m.cabNumber")
    List<Object[]> getMileageSummaryByCab(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT m.driverNumber, SUM(m.mileageA), SUM(m.mileageB), SUM(m.mileageC), " +
           "SUM(m.totalMileage), COUNT(m), SUM(m.shiftHours) " +
           "FROM MileageRecord m WHERE DATE(m.logonTime) BETWEEN :startDate AND :endDate " +
           "GROUP BY m.driverNumber ORDER BY m.driverNumber")
    List<Object[]> getMileageSummaryByDriver(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    /**
     * Find all mileage records for a specific cab where a given datetime falls within the shift.
     * Used to determine which driver was driving at a specific hour.
     */
    @Query("SELECT m FROM MileageRecord m WHERE UPPER(m.cabNumber) = UPPER(:cabNumber) " +
           "AND m.logonTime <= :dateTime AND (m.logoffTime IS NULL OR m.logoffTime >= :dateTime) " +
           "ORDER BY m.logonTime DESC")
    List<MileageRecord> findByCabNumberAndTimeWithinShift(
        @Param("cabNumber") String cabNumber,
        @Param("dateTime") LocalDateTime dateTime);

    /**
     * Find all shifts for a cab on a given date, ordered by logon time.
     */
    @Query("SELECT m FROM MileageRecord m WHERE UPPER(m.cabNumber) = UPPER(:cabNumber) " +
           "AND (DATE(m.logonTime) = :date OR DATE(m.logoffTime) = :date) " +
           "ORDER BY m.logonTime ASC")
    List<MileageRecord> findShiftsForCabOnDate(
        @Param("cabNumber") String cabNumber,
        @Param("date") LocalDate date);

    /**
     * Find mileage records that fall within the shift's time window.
     * Captures all mileage records where the mileage activity window overlaps with the shift time window.
     * Allows 15-minute tolerance on both ends to handle timing discrepancies.
     *
     * A mileage record is captured if:
     * - Driver number matches
     * - Mileage record's logon time is before shift's logoff time (+ 15 min tolerance)
     * - Mileage record's logoff time is after shift's logon time (- 15 min tolerance)
     */
    @Query(value = "SELECT m.* FROM mileage_records m " +
           "WHERE m.driver_number = :driverNumber " +
           "AND m.logon_time <= DATE_ADD(:logoffTime, INTERVAL 15 MINUTE) " +
           "AND (m.logoff_time IS NULL OR m.logoff_time >= DATE_SUB(:logonTime, INTERVAL 15 MINUTE)) " +
           "ORDER BY m.logon_time ASC",
           nativeQuery = true)
    List<MileageRecord> findByDriverNumberAndShiftTimes(
        @Param("driverNumber") String driverNumber,
        @Param("logonTime") LocalDateTime logonTime,
        @Param("logoffTime") LocalDateTime logoffTime);
}
