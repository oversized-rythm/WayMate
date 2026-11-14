package com.waymate.backend.controller;

import com.waymate.backend.dto.*;

import java.util.UUID;


import com.waymate.backend.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final EmailOtpService emailOtpService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;
    private final GoogleAuthService googleAuthService;
    public AuthController(UserService userService, RefreshTokenService refreshTokenService, EmailOtpService emailOtpService, PasswordResetService passwordResetService, GoogleAuthService googleAuthService){
        this.userService=userService;
        this.emailOtpService=emailOtpService;
        this.refreshTokenService=refreshTokenService;
        this.passwordResetService=passwordResetService;
        this.googleAuthService=googleAuthService;
    }

    //Register a new User
    @PostMapping("/register")
    public ResponseEntity<String>register(
           @Valid @RequestBody RegisterRequest request){
        userService.registerUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("User Registered Successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse>login(
            @Valid @RequestBody LoginRequest request){
        LoginResponse response=userService.loginUser(request);
        return ResponseEntity.ok(response);
    }
    //Temporary testing endpoint!
    //Get the currently authenticated user's ID
    @GetMapping("/me")
    public ResponseEntity<UUID> getCurrentUser(Authentication authentication){
        UUID userId=(UUID)authentication.getPrincipal();
        return ResponseEntity.ok(userId);
    }

    //Generate the new access and refresh tokens
    @PostMapping("/refresh")
    public  ResponseEntity<RefreshTokenResponse>refresh(
            @RequestBody RefreshTokenRequest request){
        RefreshTokenResponse response=refreshTokenService.refreshTokens(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String>logout(
            @RequestBody RefreshTokenRequest request, Authentication authentication){
        UUID userId=(UUID) authentication.getPrincipal();
        refreshTokenService.revokeRefreshToken(request.getRefreshToken(),userId);

        return ResponseEntity.ok("Logged out successfully!");
    }

    //request OTP
    @PostMapping("/request-otp")
    public ResponseEntity<RequestOtpResponse> requestOtp(
            @Valid @RequestBody RequestOtpRequest request) {

        RequestOtpResponse response = emailOtpService.requestOtp(request);
        return ResponseEntity.ok(response);
    }
    //Validate OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @RequestBody VerifyOtpRequest request) {

        VerifyOtpResponse response = emailOtpService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    // Request a password-reset otp email
    @PostMapping("/forgot-password")
    public ResponseEntity<RequestOtpResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request){
        RequestOtpResponse response=passwordResetService.requestPasswordResetOtp(request.getEmail());
        return ResponseEntity.ok(response);
    }

    //Verify the password-reset otp
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<VerifyOtpResponse>verifyResetOtp(@Valid @RequestBody VerifyOtpRequest request){
        VerifyOtpResponse response=passwordResetService.verifyPasswordResetOtp(request);
        return ResponseEntity.ok(response);

    }
    // Reset the password after otp verification
    @PostMapping("/reset-password")
    public ResponseEntity<String>resetPassword(
            @Valid @RequestBody ResetPasswordRequest request){
        passwordResetService.resetPassword(
                request.getEmail(),
                request.getNewPassword()
        );
        return ResponseEntity.ok("Password reset successfully!");
    }

    // Delete account
    @DeleteMapping("/delete-account")
    public ResponseEntity<String>deleteAccount(Authentication authentication){
        UUID userId=(UUID)authentication.getPrincipal();
        userService.deleteAccount(userId);
        return ResponseEntity.ok("Account Deleted Successfully!");
    }

    //Google SSO authenticate and register
    @PostMapping("/google")
    public ResponseEntity<LoginResponse>googleLogin(@Valid @RequestBody GoogleLoginRequest request){
        //Delegates token verification and user lookup/creation to GoogleAuthService
        LoginResponse response=googleAuthService.authenticateWithGoogle(request.getIdToken());
        return ResponseEntity.ok(response);
    }
}
