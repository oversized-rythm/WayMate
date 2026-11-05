package com.waymate.backend.repository;

import com.waymate.backend.entity.RefreshToken;
import com.waymate.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("""
        UPDATE RefreshToken r
        SET r.revoked = true,
            r.lastUsedAt = :revokedAt
        WHERE r.user = :user
          AND r.revoked = false
    """)
    void revokeAllByUser(
            @Param("user") User user,
            @Param("revokedAt") LocalDateTime revokedAt
    );

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM RefreshToken r
        WHERE r.revoked = true
          AND r.lastUsedAt <= :cutoff
    """)
    void deleteOldRevokedTokens(@Param("cutoff") LocalDateTime cutoff);
}