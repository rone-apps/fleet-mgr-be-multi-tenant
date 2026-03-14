package com.taxi.domain.tax.repository;

import com.taxi.domain.tax.model.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TaxTypeRepository extends JpaRepository<TaxType, Long> {

    List<TaxType> findByIsActiveTrueOrderByName();

    List<TaxType> findAllByOrderByName();

    Optional<TaxType> findByCode(String code);

    @Query("SELECT t FROM TaxType t LEFT JOIN FETCH t.rates WHERE t.id = :id")
    Optional<TaxType> findByIdWithRates(Long id);
}
