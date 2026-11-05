package com.waymate.backend.service;


import com.waymate.backend.entity.PasswordResetToken;
import com.waymate.backend.entity.User;
import com.waymate.backend.repository.PasswordResetTokenRepository;

import com.waymate.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class PasswordResetService {
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final BrevoEmailService brevoEmailService;
    public PasswordResetService(PasswordResetTokenRepository passwordResetTokenRepository, PasswordEncoder passwordEncoder, UserRepository userRepository, BrevoEmailService brevoEmailService){
        this.passwordResetTokenRepository=passwordResetTokenRepository;
        this.passwordEncoder=passwordEncoder;
        this.userRepository=userRepository;
        this.brevoEmailService= brevoEmailService;
    }

    // Generate a secure reset token
    private String generateSecureToken(){
        SecureRandom secureRandom=new SecureRandom();

        byte[] randomBytes= new byte[32];
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
            throw new RuntimeException("Error Hashing Password Reset Token", e);
        }
    }

    // Token-saving method

    @Transactional
    public String createPasswordResetToken(User user){

        LocalDateTime now= LocalDateTime.now();
        //Invalidate all previous unused tokens belonging to this user
        passwordResetTokenRepository.invalidatePreviousTokens(user,now);
        // Generate the original token and hash it
        String rawToken = generateSecureToken();
        String hashedToken= hashToken(rawToken);

        // Create a new token record
        PasswordResetToken resetToken=new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(hashedToken);
        resetToken.setCreatedAt(now);
        resetToken.setExpiresAt(now.plusMinutes(15));

        // save the hashed token in repository
        passwordResetTokenRepository.save(resetToken);

        //return the raw token to generate the link
        return rawToken;
    }

    //Validate the rest password
    public boolean isTokenValid(String rawToken){

        //reject empty or missing token
        if(rawToken==null || rawToken.isBlank()){
            return false;
        }
        String hashedToken= hashToken(rawToken);
        // Find matched token record
        Optional<PasswordResetToken> resetToken=passwordResetTokenRepository.findByTokenHash(hashedToken);

        //check for already existed hashed token
        if(resetToken.isEmpty()){
            return false;
        }
        PasswordResetToken token=resetToken.get();
        // checking is the token is already been used
        if(token.getUsedAt()!=null){
            return false;
        }
        //checking if the token is expired
        if(!token.getExpiresAt().isAfter(LocalDateTime.now())){
            return false;
        }
        return true;
    }

    // Reset the user's password
    @Transactional
    public boolean resetPassword(String rawToken, String newPassword){
        // Reject empty or missing passwords
        if(newPassword == null || newPassword.isBlank()){
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if(!isTokenValid(rawToken)){
            return false;
        }
        String hashedToken= hashToken(rawToken);
        PasswordResetToken resetToken=passwordResetTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow();

        // Get the user associated with this reset token
        User user= resetToken.getUser();
        //Hash the new password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Mark the reset token as used
        resetToken.setUsedAt(LocalDateTime.now());
        return true;
    }

    // Password reset email method
    private void sendPasswordResetEmail(String email, String rawToken) {

        String resetLink = "http://localhost:3000/reset-password?token=" + rawToken;

        String htmlContent = """
            <h2>Reset Your WayMate Password</h2>
            <p>We received a request to reset your password.</p>
            <p>Click the link below to reset it:</p>
            <a href="%s">Reset Password</a>
            <p>This link expires in 15 minutes.</p>
            <p>If you didn't request this, you can ignore this email.</p>
            """.formatted(resetLink);

        brevoEmailService.sendEmail(
                email,
                "WayMate Password Reset",
                htmlContent
        );
    }
    // password reset email sending method
    public void requestPasswordReset(String email){
        String normalizedEmail= email.trim().toLowerCase();
        Optional<User> optionalUser=userRepository.findByEmail(normalizedEmail);
        if(optionalUser.isEmpty()){
            return;
        }
        User user= optionalUser.get();
        String rawToken= createPasswordResetToken(user);
        sendPasswordResetEmail(user.getEmail(),rawToken);

    }

    // Scheduled clean-up method
    @Scheduled(fixedRate = 3600000)
    public void cleanupPasswordResetTokens() {

        passwordResetTokenRepository.deleteExpiredAndUsedTokens(
                LocalDateTime.now()
        );
    }
}
