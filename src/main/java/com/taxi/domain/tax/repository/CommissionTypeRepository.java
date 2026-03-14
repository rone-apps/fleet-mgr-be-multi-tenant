package com.taxi.domain.tax.repository;

import com.taxi.domain.tax.model.CommissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommissionTypeRepository extends JpaRepository<CommissionType, Long> {

    List<CommissionType> findByIsActiveTrueOrderByName();

    List<CommissionType> findAllByOrderByName();

    Optional<CommissionType> findByCode(String code);

    @Query("SELECT c FROM CommissionType c LEFT JOIN FETCH c.rates WHERE c.id = :id")
    Optional<CommissionType> findByIdWithRates(Long id);
}
