package com.taxi.domain.user.repository;

import com.taxi.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.taxi.domain.driver.model.Driver;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find all active users
     */
    List<User> findByIsActiveTrue();

    /**
     * Find users by role
     */
    List<User> findByRole(User.UserRole role);

    /**
     * Find active users by role
     */
    List<User> findByRoleAndIsActiveTrue(User.UserRole role);

    /**
     * Find user by driver ID
     */
    @Query("SELECT u FROM User u WHERE u.driver.id = :driverId")
    Optional<User> findByDriverId(Long driverId);

    /**
     * Find user by driver entity
     */
    Optional<User> findByDriver(Driver driver);

    /**
     * Find all users who are drivers (have driver link)
     */
    @Query("SELECT u FROM User u WHERE u.driver IS NOT NULL")
    List<User> findAllDriverUsers();

    /**
     * Find all non-driver users (office staff)
     */
    @Query("SELECT u FROM User u WHERE u.driver IS NULL")
    List<User> findAllOfficeStaff();

    /**
     * Find all users excluding super admins (for regular admin view)
     */
    @Query("SELECT u FROM User u WHERE u.role != com.taxi.domain.user.model.User$UserRole.SUPER_ADMIN")
    List<User> findAllExcludingSuperAdmins();

    // ✅ NEW METHOD: Check if driver is already linked to a user
    boolean existsByDriver(Driver driver);
    
    // ✅ OPTIONAL: Find user by driver
    // (already declared above)
}
