package com.taxi.domain.eft.repository;

import com.taxi.domain.eft.model.EftConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EftConfigRepository extends JpaRepository<EftConfig, Long> {
    Optional<EftConfig> findByIsActiveTrue();
}
