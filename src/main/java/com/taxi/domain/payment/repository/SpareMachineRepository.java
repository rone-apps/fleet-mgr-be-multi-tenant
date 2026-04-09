package com.taxi.domain.payment.repository;

import com.taxi.domain.payment.model.SpareMachine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpareMachineRepository extends JpaRepository<SpareMachine, Long> {

    Optional<SpareMachine> findByVirtualCabId(Integer virtualCabId);

    Optional<SpareMachine> findByMerchantNumber(String merchantNumber);

    Optional<SpareMachine> findByMachineName(String machineName);
}
