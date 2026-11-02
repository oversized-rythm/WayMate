package com.waymate.backend.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_otp")
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID otpId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String otpHash;

    @Column
    private LocalDateTime registrationExpiresAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public EmailOtp() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public UUID getOtpId() {
        return otpId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public LocalDateTime getRegistrationExpiresAt() {
        return registrationExpiresAt;
    }

    public void setRegistrationExpiresAt(LocalDateTime registrationExpiresAt) {
        this.registrationExpiresAt = registrationExpiresAt;
    }
}

