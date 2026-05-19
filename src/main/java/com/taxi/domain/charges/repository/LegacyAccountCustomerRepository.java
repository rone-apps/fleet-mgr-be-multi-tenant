package com.taxi.domain.charges.repository;

import com.taxi.domain.charges.model.LegacyAccountCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegacyAccountCustomerRepository extends JpaRepository<LegacyAccountCustomer, Long> {

    /**
     * Find customer by db_id (original database ID from legacy system)
     */
    Optional<LegacyAccountCustomer> findByDbId(Long dbId);

    /**
     * Find customer by customer ID (legacy identifier)
     */
    Optional<LegacyAccountCustomer> findByCustomerId(String customerId);

    /**
     * Find customers by city
     */
    List<LegacyAccountCustomer> findByCity(String city);

    /**
     * Find customers by name (case-insensitive contains)
     */
    @Query("SELECT lac FROM LegacyAccountCustomer lac " +
           "WHERE LOWER(lac.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<LegacyAccountCustomer> findByNameContaining(@Param("name") String name);
}
