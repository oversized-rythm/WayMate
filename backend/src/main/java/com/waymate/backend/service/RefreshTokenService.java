package com.waymate.backend.service;

import com.waymate.backend.dto.RefreshTokenResponse;
import com.waymate.backend.entity.RefreshToken;
import com.waymate.backend.entity.User;
import com.waymate.backend.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {
    //Refresh token repo injection
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService){
        this.refreshTokenRepository=refreshTokenRepository;
        this.jwtService=jwtService;
    }

    //token generation method
    private String generateSecureToken(){
        byte[]randomBytes= new byte[32];

        SecureRandom secureRandom=new SecureRandom();
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    //SHA-256 hashing method
    private String hashToken(String token){
        try{
            MessageDigest digest= MessageDigest.getInstance("SHA-256");
            byte[] hashBytes=digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getUrlEncoder()
                    .encodeToString(hashBytes);
        }catch (NoSuchAlgorithmException e){
            throw new RuntimeException("Error Hashing Refresh Token", e);
        }
    }

    //Generate and save Refresh Token Method

    public String createRefreshToken(User user,String deviceName){
        //Generate the raw token
        String rawToken=generateSecureToken();

        //Create a database record
        RefreshToken refreshToken=new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(10));
        refreshToken.setDeviceName(deviceName);

        //save the hashed token
        refreshTokenRepository.save(refreshToken);

        //return the raw token to the caller
        return rawToken;
    }
    //Refresh Token Validation Method
    public RefreshToken validateRefreshToken(String rawToken){
        //hash the incoming token
        String tokenHash=hashToken(rawToken);

        //find the token in the database
        RefreshToken refreshToken=refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(()->new RuntimeException("Invalid Refresh Token!"));

        //checking for token revokeation
        if(refreshToken.isRevoked()){
            throw new RuntimeException("Refresh Token has been Revoked!");
        }
        //checking for token expiration
        if(!refreshToken.getExpiresAt().isAfter(LocalDateTime.now())){
            throw new RuntimeException("Refresh Token has Expired!");
        }
        return refreshToken;
    }

    //Refresh Token Rotation METHOD
//    @Transactional
//    public String rotateRefreshToken(String rawToken){
//        //validate existing token
//        RefreshToken oldToken= validateRefreshToken(rawToken);
//
//        //revoke the old token
//        oldToken.setRevoked(true);
//        oldToken.setLastUsedAt(LocalDateTime.now());
//        refreshTokenRepository.save(oldToken);
//
//        //Generate the new token for same user and device
//        return createRefreshToken(
//                oldToken.getUser(),
//                oldToken.getDeviceName()
//        );
//    }

    //Revoke refresh token during logout method

    @Transactional
    public void revokeRefreshToken(String rawToken, UUID userId){
        String tokenHash=hashToken(rawToken);
        //find the token in the database
        RefreshToken refreshToken= refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(()->new RuntimeException("Invalid Refresh Token!"));

        //verify token ownership
        if(!refreshToken.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized Token Access!");
        }
        //Revoke the token only if not already revoked
        if(!refreshToken.isRevoked()){
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        }


    }
    //Generating new Access tokens using a valid Refresh Token
//    public  String generateNewAccessToken(String rawToken){
//        RefreshToken refreshToken=validateRefreshToken(rawToken);
//        UUID userId=refreshToken.getUser().getUserId();
//        return jwtService.generateToken(userId);
//    }

    //Rotate refresh tokens and generate new access tokens both in one transaction
    @Transactional
    public RefreshTokenResponse refreshTokens(String rawToken){
        RefreshToken oldToken=validateRefreshToken(rawToken);

        //Generate new access token
        String newAccessToken= jwtService.generateToken(
                oldToken.getUser().getUserId()
        );

        //Revoke the old refresh token
        oldToken.setRevoked(true);
        oldToken.setLastUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(oldToken);

        //Create a new refresh token
        String newRefreshToken= createRefreshToken(
                oldToken.getUser(),
                oldToken.getDeviceName()
        );
        return new RefreshTokenResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer"
        );

    }
}
