package com.carboncredit.repository;

import com.carboncredit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByRole(User.UserRole role);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByFullName(String fullName);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.createdAt >= CURRENT_TIMESTAMP - 30 DAY")
    List<User> findRecentUsersByRole(User.UserRole role);

    @Query("SELECT u FROM User u WHERE u.email = :input OR u.phone = :input")
    Optional<User> findByEmailOrPhone(String input);

     // ✅ NEW: Email Verification methods
    Optional<User> findByEmailVerificationToken(String token);
    
    @Query("SELECT u FROM User u WHERE u.isEmailVerified = false AND u.createdAt < :before")
    List<User> findUnverifiedUsersOlderThan(LocalDateTime before);

    // ✅ NEW: Password Reset methods
    Optional<User> findByPasswordResetToken(String token);
    
    @Query("SELECT u FROM User u WHERE u.passwordResetExpiresAt < CURRENT_TIMESTAMP AND u.passwordResetToken IS NOT NULL")
    List<User> findExpiredPasswordResetTokens();

}
