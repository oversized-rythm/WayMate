package com.waymate.backend.service;

import com.waymate.backend.dto.RequestOtpRequest;
import com.waymate.backend.dto.RequestOtpResponse;
import com.waymate.backend.dto.VerifyOtpRequest;
import com.waymate.backend.dto.VerifyOtpResponse;
import com.waymate.backend.entity.EmailOtp;
import com.waymate.backend.repository.EmailOtpRepository;
import com.waymate.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailOtpService {
    private final EmailOtpRepository emailOtpRepository;
    private final WebClient webClient;
    private final UserRepository userRepository;

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    public EmailOtpService(EmailOtpRepository emailOtpRepository, WebClient webClient, UserRepository userRepository){
        this.emailOtpRepository= emailOtpRepository;
        this.webClient=webClient;
        this.userRepository=userRepository;
    }

    //Generating a secured 6-Digit OTP
    private String generateOtp(){
        SecureRandom random= new SecureRandom();
        int otp= 100000 + random.nextInt(900000);

        return String.valueOf(otp);
    }

    //SHA-256 hashed OTP
    private String hashOtp(String otp){
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            byte[]hash= digest.digest(otp.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        }catch (NoSuchAlgorithmException e){
            throw new RuntimeException("Error Hashing OTP" ,e);
        }
    }

    //email-sending method, connecting WayMate to Brevo

    private void sendOtpEmail(String email, String otp){

        //send the HTTP POST request
        webClient.post()

                //target Brevo's email endpoint
                .uri("/smtp/email")
                //authenticating using Brevo API key
                .header("api-key",brevoApiKey)
                .header("Content-Type","application/json")

                //provide the sender, recipient, subject, and email content
                .bodyValue(Map.of(
                        "sender", Map.of(
                        "name", senderName,
                        "email",senderEmail
                ),
                        "to", List.of(
                                Map.of("email", email)
                        ),
                        "subject", "Your WayMate Verification Code",
                        "htmlContent","<h2>Your WayMate Otp is "+ otp +
                                "</h2><p>This code expires in 5 minutes. Do not share it with anyone.</p>"
                ))

                //sends the request and retriece the response
                .retrieve()
                .bodyToMono(String.class)
                //wait for brevo's response before continuing
                .block();
    }

    //OTP registration process

    public RequestOtpResponse requestOtp(RequestOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        //checking for already registered email
        if(userRepository.existsByEmail(email)){
            throw new RuntimeException("Email is already registered");
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
        emailOtpRepository.save(emailOtp);

        //Send OTP via Brevo
        sendOtpEmail(email,otp);

        return new RequestOtpResponse("OTP sent successfully to your email.");

    }

    //Verify the OTP entered by user
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request){
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        //Validate OTP format
        if (request.getOtp() == null || !request.getOtp().matches("\\d{6}")) {
            throw new RuntimeException("OTP must be exactly 6 digits");
        }
        //find the latest OTP record
        EmailOtp emailOtp= emailOtpRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(()->new RuntimeException("No OTP Found!"));

        //check if OTP is already verified
        if(emailOtp.isVerified()){
            throw new RuntimeException("OTP already verified!");
        }
        //check OTP expiration
        if(!emailOtp.getExpiresAt().isAfter(LocalDateTime.now())){
            throw new RuntimeException("OTP has expired!");
        }
        //check maximum attempts
        if(emailOtp.getAttempts()>=5){
            throw new RuntimeException("Maximum OTP attempts exceeded!");
        }
        //hash the OTP by the user
        String enteredOtpHash= hashOtp(request.getOtp());
        //compare entered OTP with the stored one
        boolean isMatch= MessageDigest.isEqual(
                emailOtp.getOtpHash().getBytes(StandardCharsets.UTF_8),
                enteredOtpHash.getBytes(StandardCharsets.UTF_8)
        );
        //handles incorrect OTP
        if(!isMatch){
            emailOtp.setAttempts(emailOtp.getAttempts()+1);
            emailOtpRepository.save(emailOtp);
            return new VerifyOtpResponse("Invalid OT    P", false, null);
        }

        //mark OTP as verified
        emailOtp.setVerified(true);
        emailOtp.setRegistrationExpiresAt(LocalDateTime.now().plusMinutes(10));

        emailOtpRepository.save(emailOtp);

        return new VerifyOtpResponse("Email verified successfully!", true, email);
    }
}
