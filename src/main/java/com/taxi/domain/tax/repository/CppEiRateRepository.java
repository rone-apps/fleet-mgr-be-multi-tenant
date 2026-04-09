package com.taxi.domain.tax.repository;

import com.taxi.domain.tax.model.CppEiRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CppEiRateRepository extends JpaRepository<CppEiRate, Long> {

    Optional<CppEiRate> findByTaxYear(Integer taxYear);
}
