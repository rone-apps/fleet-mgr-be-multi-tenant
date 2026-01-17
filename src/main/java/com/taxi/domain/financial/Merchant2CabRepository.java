package com.taxi.domain.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface Merchant2CabRepository extends JpaRepository<Merchant2Cab, Long> {
    
    // Find active mapping for a cab
    @Query("SELECT m FROM Merchant2Cab m WHERE m.cabNumber = :cabNumber " +
           "AND (m.endDate IS NULL OR m.endDate >= :date) " +
           "ORDER BY m.startDate DESC")
    List<Merchant2Cab> findActiveMappingsByCabNumber(@Param("cabNumber") String cabNumber, 
                                                       @Param("date") LocalDate date);
    
    // Find current active mapping for a cab
    @Query("SELECT m FROM Merchant2Cab m WHERE m.cabNumber = :cabNumber " +
           "AND m.endDate IS NULL")
    Optional<Merchant2Cab> findCurrentMappingByCabNumber(@Param("cabNumber") String cabNumber);
    
    // Find all mappings for a cab (including historical)
    List<Merchant2Cab> findByCabNumberOrderByStartDateDesc(String cabNumber);
    
    // Find all cabs for a merchant number
    @Query("SELECT m FROM Merchant2Cab m WHERE m.merchantNumber = :merchantNumber " +
           "AND (m.endDate IS NULL OR m.endDate >= :date)")
    List<Merchant2Cab> findActiveMappingsByMerchantNumber(@Param("merchantNumber") String merchantNumber,
                                                            @Param("date") LocalDate date);
    
    // Find all active mappings with cab details
    @Query("SELECT m FROM Merchant2Cab m WHERE m.endDate IS NULL " +
           "ORDER BY m.cabNumber")
    List<Merchant2Cab> findAllActiveMappings();
    
    // Check for overlapping mappings
    @Query("SELECT COUNT(m) > 0 FROM Merchant2Cab m WHERE m.cabNumber = :cabNumber " +
           "AND m.id != :excludeId " +
           "AND m.startDate <= :endDate " +
           "AND (m.endDate IS NULL OR m.endDate >= :startDate)")
    boolean hasOverlappingMapping(@Param("cabNumber") String cabNumber,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate,
                                   @Param("excludeId") Long excludeId);

    @Query("SELECT m FROM Merchant2Cab m WHERE m.merchantNumber = :merchantNumber " +
       "AND m.startDate <= :date " +
       "AND (m.endDate IS NULL OR m.endDate >= :date)")
    List<Merchant2Cab> findByMerchantNumberAndActiveDateRange(
                                @Param("merchantNumber") String merchantNumber,
                                @Param("date") LocalDate date);

    

                                
}