package com.waymate.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID rtId;
    //User who owns the token
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private boolean revoked=false;
    private LocalDateTime createdAt;
    private String deviceName;
    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate(){
        createdAt=LocalDateTime.now();
    }

    public RefreshToken(){}
    //Getters and Setters
    public UUID getRtId(){
        return rtId;
    }
    public User getUser(){
        return user;
    }
    public void setUser(User user){
        this.user=user;
    }
    public String getTokenHash(){
        return tokenHash;
    }
    public void setTokenHash(String tokenHash){
        this.tokenHash=tokenHash;
    }
    public LocalDateTime getExpiresAt(){
        return expiresAt;
    }
    public void setExpiresAt(LocalDateTime expiresAt){
        this.expiresAt=expiresAt;
    }
    public LocalDateTime getCreatedAt(){
        return createdAt;
    }
    public LocalDateTime getLastUsedAt(){
        return lastUsedAt;
    }
    public void setLastUsedAt(LocalDateTime lastUsedAt){
        this.lastUsedAt=lastUsedAt;
    }
    public boolean isRevoked(){
        return revoked;
    }
    public void setRevoked(boolean revoked){
        this.revoked=revoked;
    }
    public String getDeviceName(){
        return deviceName;
    }
    public void setDeviceName(String deviceName){
        this.deviceName=deviceName;
    }


}
