package com.taxi.domain.moneris;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonerisConfigRepository extends JpaRepository<MonerisConfig, Long> {

    List<MonerisConfig> findByCabNumber(String cabNumber);

    Optional<MonerisConfig> findByCabNumberAndShift(String cabNumber, String shift);

    Optional<MonerisConfig> findByMerchantNumber(String merchantNumber);

    Optional<MonerisConfig> findByMonerisStoreId(String monerisStoreId);

    List<MonerisConfig> findByMonerisEnvironment(String environment);

    boolean existsByCabNumberAndShiftAndMerchantNumber(String cabNumber, String shift, String merchantNumber);
}
