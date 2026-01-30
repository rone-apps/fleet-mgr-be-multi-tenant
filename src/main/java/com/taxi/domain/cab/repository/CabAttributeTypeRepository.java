package com.taxi.domain.cab.repository;

import com.taxi.domain.cab.model.CabAttributeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CabAttributeTypeRepository extends JpaRepository<CabAttributeType, Long> {

    Optional<CabAttributeType> findByAttributeCode(String attributeCode);

    List<CabAttributeType> findByIsActiveTrue();

    List<CabAttributeType> findByCategory(CabAttributeType.AttributeCategory category);

    @Query("SELECT t FROM CabAttributeType t WHERE t.isActive = true AND t.category = :category ORDER BY t.attributeName ASC")
    List<CabAttributeType> findActiveByCategoryOrderByAttributeName(
        CabAttributeType.AttributeCategory category);

    boolean existsByAttributeCode(String attributeCode);

    List<CabAttributeType> findAllByOrderByAttributeNameAsc();
}
