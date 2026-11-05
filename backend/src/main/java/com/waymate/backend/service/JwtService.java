package com.waymate.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    //Convert the Base64 secret into a signing key
    private SecretKey getSigningKey(){
        byte[]keyBytes= Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    //Generating a Jwt for logged-in users
    public String generateToken(UUID userId){
        long expirationTime=1000*60*60; //1 hr

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }
    //Extract the user's UUID from the token

    public String extractUserId(String token){
        return extractAllClaims(token).getSubject();
    }

    //Checks the token signature and expiration
    public boolean validateToken(String token){
        try{
            extractAllClaims(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    //Parse the token and verify it
    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
