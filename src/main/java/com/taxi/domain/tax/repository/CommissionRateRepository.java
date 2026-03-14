package com.taxi.domain.tax.repository;

import com.taxi.domain.tax.model.CommissionRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CommissionRateRepository extends JpaRepository<CommissionRate, Long> {

    List<CommissionRate> findByCommissionTypeIdOrderByEffectiveFromDesc(Long commissionTypeId);

    List<CommissionRate> findByCommissionTypeIdAndIsActiveTrueOrderByEffectiveFromDesc(Long commissionTypeId);

    @Query("SELECT r FROM CommissionRate r WHERE r.commissionType.id = :typeId AND r.isActive = true " +
           "AND r.effectiveFrom <= :date AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date)")
    Optional<CommissionRate> findActiveRateOnDate(@Param("typeId") Long typeId, @Param("date") LocalDate date);
}
