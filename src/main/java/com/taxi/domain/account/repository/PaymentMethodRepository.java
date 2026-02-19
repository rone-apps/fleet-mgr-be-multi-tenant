package com.taxi.domain.account.repository;

import com.taxi.domain.account.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    Optional<PaymentMethod> findByMethodCode(String methodCode);

    @Query("SELECT p FROM PaymentMethod p WHERE p.isActive = true ORDER BY p.displayOrder ASC")
    List<PaymentMethod> findAllActive();
}
