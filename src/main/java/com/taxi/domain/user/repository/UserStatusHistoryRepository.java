package com.taxi.domain.user.repository;

import com.taxi.domain.user.entity.UserStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserStatusHistoryRepository extends JpaRepository<UserStatusHistory, Long> {

    /**
     * Find all status changes for a user, ordered by most recent first
     */
    List<UserStatusHistory> findByUserIdOrderByChangedAtDesc(Long userId);

    /**
     * Find all status changes made by a specific admin
     */
    List<UserStatusHistory> findByChangedByIdOrderByChangedAtDesc(Long changedById);

    /**
     * Get the most recent status change for a user
     */
    @Query("SELECT h FROM UserStatusHistory h WHERE h.user.id = :userId ORDER BY h.changedAt DESC LIMIT 1")
    UserStatusHistory findMostRecentByUserId(@Param("userId") Long userId);
}
