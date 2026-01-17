package com.taxi.domain.airport.repository;

import com.taxi.domain.airport.model.AirportTrip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AirportTripRepository extends JpaRepository<AirportTrip, Long> {

    List<AirportTrip> findByCabNumberOrderByTripDateDesc(String cabNumber);

    List<AirportTrip> findByShiftOrderByTripDateDesc(String shift);

    List<AirportTrip> findByTripDateBetweenOrderByTripDateDesc(LocalDate startDate, LocalDate endDate);

    /**
     * Paginated queries for data view
     */
    Page<AirportTrip> findByTripDateBetweenOrderByTripDateDesc(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    @Query("SELECT a FROM AirportTrip a WHERE a.tripDate BETWEEN :startDate AND :endDate AND a.cabNumber = :cabNumber ORDER BY a.tripDate DESC")
    Page<AirportTrip> findByTripDateBetweenAndCabNumber(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate, 
        @Param("cabNumber") String cabNumber, 
        Pageable pageable);
    
    @Query("SELECT a FROM AirportTrip a WHERE a.tripDate BETWEEN :startDate AND :endDate AND a.shift = :shift ORDER BY a.tripDate DESC")
    Page<AirportTrip> findByTripDateBetweenAndShift(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate, 
        @Param("shift") String shift, 
        Pageable pageable);
    
    @Query("SELECT a FROM AirportTrip a WHERE a.tripDate BETWEEN :startDate AND :endDate " +
           "AND a.cabNumber = :cabNumber AND a.shift = :shift ORDER BY a.tripDate DESC")
    Page<AirportTrip> findByTripDateBetweenAndCabNumberAndShift(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate, 
        @Param("cabNumber") String cabNumber, 
        @Param("shift") String shift, 
        Pageable pageable);

    List<AirportTrip> findByCabNumberAndTripDateBetweenOrderByTripDateDesc(
        String cabNumber, LocalDate startDate, LocalDate endDate);

    Optional<AirportTrip> findByCabNumberAndTripDate(String cabNumber, LocalDate tripDate);

    Optional<AirportTrip> findByCabNumberAndShiftAndTripDate(String cabNumber, String shift, LocalDate tripDate);

    boolean existsByCabNumberAndTripDate(String cabNumber, LocalDate tripDate);

    boolean existsByCabNumberAndShiftAndTripDate(String cabNumber, String shift, LocalDate tripDate);

    List<AirportTrip> findByUploadBatchIdOrderByTripDateDesc(String uploadBatchId);

    @Query("SELECT a.cabNumber, SUM(a.grandTotal), COUNT(a) FROM AirportTrip a " +
           "WHERE a.tripDate BETWEEN :startDate AND :endDate " +
           "GROUP BY a.cabNumber ORDER BY SUM(a.grandTotal) DESC")
    List<Object[]> getTripSummaryByCab(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT a.shift, SUM(a.grandTotal), COUNT(a) FROM AirportTrip a " +
           "WHERE a.tripDate BETWEEN :startDate AND :endDate " +
           "GROUP BY a.shift ORDER BY a.shift")
    List<Object[]> getTripSummaryByShift(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT a.cabNumber, a.shift, SUM(a.grandTotal), COUNT(a) FROM AirportTrip a " +
           "WHERE a.tripDate BETWEEN :startDate AND :endDate " +
           "GROUP BY a.cabNumber, a.shift ORDER BY a.cabNumber, a.shift")
    List<Object[]> getTripSummaryByCabAndShift(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(a.grandTotal) FROM AirportTrip a WHERE a.tripDate BETWEEN :startDate AND :endDate")
    Long getTotalTripsForDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
