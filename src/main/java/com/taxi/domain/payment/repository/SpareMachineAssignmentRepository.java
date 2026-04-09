package com.taxi.domain.payment.repository;

import com.taxi.domain.payment.model.SpareMachineAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpareMachineAssignmentRepository extends JpaRepository<SpareMachineAssignment, Long> {

    Optional<SpareMachineAssignment> findBySpareMachineIdAndReturnedAtIsNull(Long spareMachineId);

    List<SpareMachineAssignment> findByRealCabNumberAndAssignedAtBetween(
        Integer realCabNumber, LocalDateTime from, LocalDateTime to);

    List<SpareMachineAssignment> findByReturnedAtIsNullOrderByAssignedAtDesc();

    List<SpareMachineAssignment> findBySpareMachineIdOrderByAssignedAtDesc(Long spareMachineId);
}
