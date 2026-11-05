package com.waymate.backend.repository;

import com.waymate.backend.entity.PasswordResetToken;
import com.waymate.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    //Database update operation
    @Modifying

    @Transactional
    //Update all unused reset token belongings to the given user
    @Query("""
        UPDATE PasswordResetToken p
        SET p.usedAt= :currentTime
        WHERE p.user= :user
        AND p.usedAt IS NULL
""")

    // Mark previous unused tokens as invalid!
    void invalidatePreviousTokens(
            @Param("user") User user,
            @Param("currentTime") LocalDateTime currentTime
    );

    // Scheduled cleanup job after password reset for db table
    @Modifying
    @Transactional
    @Query("""
    DELETE FROM PasswordResetToken p
    WHERE p.expiresAt <= :currentTime
       OR p.usedAt IS NOT NULL
""")
    void deleteExpiredAndUsedTokens(
            @Param("currentTime") LocalDateTime currentTime
    );

}
