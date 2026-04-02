package com.taxi.domain.eft.repository;

import com.taxi.domain.eft.model.EftFileGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EftFileGenerationRepository extends JpaRepository<EftFileGeneration, Long> {
    List<EftFileGeneration> findByBatchId(Long batchId);
}
