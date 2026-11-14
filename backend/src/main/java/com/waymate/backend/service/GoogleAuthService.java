package com.waymate.backend.service;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.waymate.backend.dto.LoginResponse;
import com.waymate.backend.entity.User;
import com.waymate.backend.exception.ApiException;
import com.waymate.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Locale;

@Service

public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    //Google's library to download Google's public certificates, parse JWT headers, and write RSA signature-checking
    private final GoogleIdTokenVerifier verifier;


    @Autowired
    public GoogleAuthService(UserRepository userRepository, JwtService jwtService, RefreshTokenService refreshTokenService,@Value("${google.client.id}")
    String googleClientId ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;

        //Configures the verifier to only accept ID tokens meant for WayMate google client ID

        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

    }
    //Constructor used by JUnit tests to inject a mock verifier
    public GoogleAuthService(
            UserRepository userRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            GoogleIdTokenVerifier verifier) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.verifier = verifier;
    }

    //verify and extract user data with Google
    @Transactional
    public LoginResponse authenticateWithGoogle(String idTokenString){

        //Ask Google's verifier to validate the token signature, expiration, and Client ID
        GoogleIdToken idToken;
        try{
            idToken= verifier.verify(idTokenString);
        }catch(Exception e){
            throw new ApiException(HttpStatus.UNAUTHORIZED,"Failed to verify Google ID Token");
        }
        if(idToken==null){
            throw new ApiException(HttpStatus.UNAUTHORIZED,"Invalid or Expired Google ID Token");
        }
//        Extract the user's identity claims from the verified Google payload
        GoogleIdToken.Payload payload=idToken.getPayload();
        Boolean emailVerified=payload.getEmailVerified();
        if(emailVerified==null || !emailVerified){
            throw  new ApiException(HttpStatus.FORBIDDEN, "Google account is not verified");
        }
        String googleId= payload.getSubject();
        String email=payload.getEmail().trim().toLowerCase(Locale.ROOT);
        String name=(String)payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        //Look up by googleId first; if not found, check by email (links existing local accounts)
        User user= userRepository.findByGoogleId(googleId)
                .orElseGet(()->userRepository.findByEmail(email).orElse(null));

        //Create a new account for new users
        if(user==null){
            user=new User();
            user.setFullName((name!=null && !name.isBlank())?name.trim():email.split("@")[0]);
            user.setEmail(email);
            user.setGoogleId(googleId);
            user.setAuthProvider("GOOGLE");
            user.setAvatarUrl(pictureUrl);
            user.setAccountStatus("ACTIVE");
            user.setEmailVerified(true);
            user= userRepository.save(user);

        }else{
            if(user.getGoogleId()==null){
                user.setGoogleId(googleId);
            }
            if((user.getAvatarUrl()==null || user.getAvatarUrl().isBlank()) && pictureUrl!=null){
                user.setAvatarUrl(pictureUrl);
            }
            if(!user.isEmailVerified()){
                user.setEmailVerified(true);
            }
            user= userRepository.save(user);
        }
        //Block deleted or suspended accounts
        if(!"ACTIVE".equals(user.getAccountStatus())){
            throw new ApiException(HttpStatus.FORBIDDEN,"Your account is inactive. Please contact support");

        }
        //issue jwt and refresh token
        String accessToken= jwtService.generateToken(user.getUserId());
        String refreshToken = refreshTokenService.createRefreshToken(user,"Google SSO");
        return new LoginResponse(accessToken,"Bearer", refreshToken);

    }

}
