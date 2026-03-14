package com.taxi.domain.tax.repository;

import com.taxi.domain.tax.model.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {

    List<TaxRate> findByTaxTypeIdOrderByEffectiveFromDesc(Long taxTypeId);

    List<TaxRate> findByTaxTypeIdAndIsActiveTrueOrderByEffectiveFromDesc(Long taxTypeId);

    @Query("SELECT r FROM TaxRate r WHERE r.taxType.id = :taxTypeId AND r.isActive = true " +
           "AND r.effectiveFrom <= :date AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date)")
    Optional<TaxRate> findActiveRateOnDate(@Param("taxTypeId") Long taxTypeId, @Param("date") LocalDate date);
}
