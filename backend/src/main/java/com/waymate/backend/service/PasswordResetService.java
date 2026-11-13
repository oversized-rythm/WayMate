package com.waymate.backend.service;


import com.waymate.backend.dto.RequestOtpResponse;
import com.waymate.backend.dto.VerifyOtpRequest;
import com.waymate.backend.dto.VerifyOtpResponse;
import com.waymate.backend.entity.EmailOtp;
import com.waymate.backend.entity.OtpPurpose;
import com.waymate.backend.entity.User;
import com.waymate.backend.exception.ApiException;
import com.waymate.backend.repository.EmailOtpRepository;

import com.waymate.backend.repository.RefreshTokenRepository;
import com.waymate.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class PasswordResetService {
    private final EmailOtpService emailOtpService;
    private final EmailOtpRepository emailOtpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public PasswordResetService(EmailOtpService emailOtpService, EmailOtpRepository emailOtpRepository, RefreshTokenRepository refreshTokenRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.emailOtpRepository = emailOtpRepository;
        this.emailOtpService = emailOtpService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public RequestOtpResponse requestPasswordResetOtp(String email) {
        return emailOtpService.requestPasswordResetOtp(email);
    }

    public VerifyOtpResponse verifyPasswordResetOtp(VerifyOtpRequest request) {
        return emailOtpService.verifyPasswordResetOtp(request);
    }

    @Transactional
    public void resetPassword(String email, String newPassword){
        if(newPassword==null || newPassword.isBlank()){
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password cannot be empty!");
        }
        String normalizedEmail= email.trim().toLowerCase(Locale.ROOT);
        EmailOtp emailOtp=emailOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(normalizedEmail, OtpPurpose.PASSWORD_RESET)
                .orElseThrow(()-> new ApiException(HttpStatus.NOT_FOUND, "No Password reset request found. Please request an OTP first!"));

        if(!emailOtp.isVerified()){
            throw new ApiException(HttpStatus.FORBIDDEN, "OTP has not been verified!");
        }
        if(!emailOtp.getExpiresAt().isAfter(LocalDateTime.now())){
            throw new ApiException(HttpStatus.GONE, "Password reset window has expired. Please request a new OTP");
        }
        User user= userRepository.findByEmail(normalizedEmail)
                .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"User Not Found!"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Consume the OTP so it cannot be reused
        emailOtpRepository.delete(emailOtp);
        // Revoke all active refresh tokens for security
        refreshTokenRepository.revokeAllByUser(user, LocalDateTime.now());
    }

}