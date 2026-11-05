package com.waymate.backend.service;

import com.waymate.backend.dto.RequestOtpRequest;
import com.waymate.backend.dto.RequestOtpResponse;
import com.waymate.backend.dto.VerifyOtpRequest;
import com.waymate.backend.dto.VerifyOtpResponse;
import com.waymate.backend.entity.EmailOtp;
import com.waymate.backend.exception.ApiException;
import com.waymate.backend.repository.EmailOtpRepository;
import com.waymate.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;



import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import java.util.Locale;


@Service
public class EmailOtpService {
    private final EmailOtpRepository emailOtpRepository;

    private final UserRepository userRepository;
    private final BrevoEmailService brevoEmailService;
    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    @Value("${otp.hmac.secret}")
    private String otpHmacSecret;

    public EmailOtpService(EmailOtpRepository emailOtpRepository, UserRepository userRepository, BrevoEmailService brevoEmailService){
        this.emailOtpRepository= emailOtpRepository;

        this.brevoEmailService=brevoEmailService;
        this.userRepository=userRepository;
    }

    //Generating a secured 6-Digit OTP
    private String generateOtp(){
        SecureRandom random= new SecureRandom();
        int otp= 100000 + random.nextInt(900000);

        return String.valueOf(otp);
    }

    //SHA-256 hashed OTP
//    private String hashOtp(String otp){
//        try{
//            MessageDigest digest=MessageDigest.getInstance("SHA-256");
//            byte[]hash= digest.digest(otp.getBytes(StandardCharsets.UTF_8));
//
//            return Base64.getEncoder().encodeToString(hash);
//        }catch (NoSuchAlgorithmException e){
//            throw new RuntimeException("Error Hashing OTP" ,e);
//        }
//    }
    //HMAC hashed OTP
    private String hashOtp(String otp){
            try{
                Mac mac = Mac.getInstance("HmacSHA256");

                SecretKeySpec secretKey = new SecretKeySpec(
                        otpHmacSecret.getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256"
                );
                mac.init(secretKey);

                byte[] hash = mac.doFinal(otp.getBytes(StandardCharsets.UTF_8));

                return Base64.getEncoder().encodeToString(hash);
            }catch (GeneralSecurityException e){
                throw new IllegalStateException("Unable to hash OTP", e);
            }
    }

    //email-sending method, connecting WayMate to Brevo

    private void sendOtpEmail(String email, String otp){

        String htmlContent = "<h2>Your WayMate OTP is " + otp +
                "</h2><p>This code expires in 5 minutes. Do not share it with anyone.</p>";

        brevoEmailService.sendEmail(
                email,
                "Your WayMate Verification Code",
                htmlContent
        );
    }

    //OTP registration process

    public RequestOtpResponse requestOtp(RequestOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        // Prevent registration using an email that already has an account
        if(userRepository.existsByEmail(email)){
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }
        //create otp and record
        String otp= generateOtp();
        EmailOtp emailOtp=new EmailOtp();
        emailOtp.setEmail(email);
        emailOtp.setOtpHash(hashOtp(otp));
        emailOtp.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        //Remove previous OTP records
        emailOtpRepository.deleteByEmail(email);

        //Save new OTP
        EmailOtp savedOtp=emailOtpRepository.save(emailOtp);

        //Send OTP via Brevo
        try {
            sendOtpEmail(email, otp);
        }catch (Exception e){
            //remove the OTP from DB if email delivery fails
            emailOtpRepository.delete(savedOtp);
            System.err.println("Brevo email sending failed: " + e.getMessage());
            // Return 502 because an external email service failed.
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to send verification email. Please try again later!"
            );
        }
        return new RequestOtpResponse("OTP sent successfully to your email.");

    }

    //Verify the OTP entered by user
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request){
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        // Reject OTP values that don't contain exactly six digits.
        if (request.getOtp() == null || !request.getOtp().matches("\\d{6}")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "OTP must be exactly 6 digits."
            );
        }
        //find the latest OTP record
        EmailOtp emailOtp= emailOtpRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                // Return 404 when no OTP record exists for the supplied email.
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "No OTP found. Please request a new OTP."
                ));

        // Prevent the same OTP from being verified more than once.
        if(emailOtp.isVerified()){
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "OTP already verified!"
            );
        }
        // Prevent users from verifying an OTP after its expiry time.
        if(!emailOtp.getExpiresAt().isAfter(LocalDateTime.now())){
            throw new ApiException(
                    HttpStatus.GONE,
                    "OTP has expired. Please request a new OTP."
            );
        }
        // Stop OTP verification after five incorrect attempts.
        if(emailOtp.getAttempts()>=5){
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Maximum OTP attempts exceeded. Please request a new OTP."
            );
        }
        //hash the OTP by the user
        String enteredOtpHash= hashOtp(request.getOtp());
        //compare entered OTP with the stored one
        boolean isMatch= MessageDigest.isEqual(
                emailOtp.getOtpHash().getBytes(StandardCharsets.UTF_8),
                enteredOtpHash.getBytes(StandardCharsets.UTF_8)
        );
        // Reject an incorrect OTP while recording the failed attempt
        if(!isMatch){
            emailOtp.setAttempts(emailOtp.getAttempts()+1);
            emailOtpRepository.save(emailOtp);
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid OTP. Please try again."
            );
        }

        //mark OTP as verified
        emailOtp.setVerified(true);
        emailOtp.setRegistrationExpiresAt(LocalDateTime.now().plusMinutes(10));

        emailOtpRepository.save(emailOtp);

        return new VerifyOtpResponse("Email verified successfully!", true, email);
    }
}