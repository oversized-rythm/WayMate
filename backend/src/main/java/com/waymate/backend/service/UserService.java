package com.waymate.backend.service;

import com.waymate.backend.dto.LoginRequest;
import com.waymate.backend.dto.LoginResponse;
import com.waymate.backend.dto.RegisterRequest;
import com.waymate.backend.entity.EmailOtp;
import com.waymate.backend.entity.OtpPurpose;
import com.waymate.backend.entity.User;
import com.waymate.backend.exception.ApiException;
import com.waymate.backend.repository.EmailOtpRepository;
import com.waymate.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailOtpRepository emailOtpRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,JwtService jwtService,RefreshTokenService refreshTokenService, EmailOtpRepository emailOtpRepository){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.jwtService=jwtService;
        this.refreshTokenService=refreshTokenService;
        this.emailOtpRepository=emailOtpRepository;
    }
    public User getUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(()->
                        new RuntimeException("User not Found"));
    }
    //register a new user
    @Transactional
    public User registerUser(RegisterRequest request){
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        // Prevent registration with an email that already has an account
        if(userRepository.existsByEmail(email)){
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Email is already registered."
            );
        }
        // Ensure an OTP record exists for this email.
        EmailOtp emailOtp=emailOtpRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.REGISTRATION)
                .orElseThrow(()->new ApiException(
                        HttpStatus.FORBIDDEN,
                        "Please verify your email first."));

        // Only allow registration after successful email verification.
        if(!emailOtp.isVerified()){
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Please verify your email first."
            );
        }
        // Prevent registration if the email verification session has expired.
        if(emailOtp.getRegistrationExpiresAt()==null || !emailOtp.getRegistrationExpiresAt().isAfter(LocalDateTime.now())){
            throw new ApiException(
                    HttpStatus.GONE,
                    "Registration session expired. Please verify your email again."
            );
        }
        //Creating a new user entity
        User user= new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);

        //Hash the password
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAccountStatus("ACTIVE");
        user.setEmailVerified(true);

        //Save the user
        User savedUser = userRepository.save(user);
        //Remove the used OTP record
        emailOtpRepository.delete(emailOtp);

        return savedUser;

    }

    //login an existing user
    public LoginResponse loginUser(LoginRequest request){
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        // Return a generic error so we don't reveal whether an email is registered.
        User user=userRepository.findByEmail(email)
                .orElseThrow(()->new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password."));


        // Block password login if this account was created via Google SSO and has no password set
        if (user.getPasswordHash() == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "This account uses Google Sign-In. Please continue with Google."
            );
        }
        // Reject login when the supplied password doesn't match.
        boolean isMatch= passwordEncoder.matches(request.getPassword(),user.getPasswordHash());
        if(!isMatch){
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password."
            );
        }
        // Block login if the account's email hasn't been verified.
        if (!user.isEmailVerified()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Please verify your email before logging in."
            );
        }
        // Prevent inactive accounts from logging in.
        if(!"ACTIVE".equals(user.getAccountStatus())){
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Your account is inactive. Please contact support."
            );
        }


        //Generate JWT after a successful Login
        String accessToken = jwtService.generateToken(user.getUserId());
        String refreshToken=refreshTokenService.createRefreshToken(user, "Unknown device");
        return new LoginResponse(accessToken, "Bearer",refreshToken);
    }

    // Account deletion method
    @Transactional
    public void deleteAccount(UUID userId){
        User user=userRepository.findById(userId)
                .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"User Not Found!"));
        if(user.getDeletedAt()!=null){
            throw new ApiException(HttpStatus.GONE, "Account has already been deleted!");
        }
        user.setFullName("Deleted User");
        user.setEmail("deleted" + user.getUserId() + "@waymate.invalid");
        user.setPhoneNo(null);
        user.setAvatarUrl(null);
        user.setPasswordHash(UUID.randomUUID().toString());
        user.setEmailVerified(false);
        user.setDeletedAt(LocalDateTime.now());
        user.setAccountStatus("DELETED");

        user.setGoogleId(null);
        refreshTokenService.revokeAllRefreshTokens(user);
    }
}
