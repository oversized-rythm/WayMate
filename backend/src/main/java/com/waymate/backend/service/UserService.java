package com.waymate.backend.service;

import com.waymate.backend.dto.LoginRequest;
import com.waymate.backend.dto.LoginResponse;
import com.waymate.backend.dto.RegisterRequest;
import com.waymate.backend.entity.EmailOtp;
import com.waymate.backend.entity.User;
import com.waymate.backend.repository.EmailOtpRepository;
import com.waymate.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

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
        //check if email already exists
        if(userRepository.existsByEmail(email)){
            throw new RuntimeException("Email Already Registered!");
        }
        //Find latest OTP record for the email
        EmailOtp emailOtp=emailOtpRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(()->new RuntimeException("Please verify your email first!"));

        //Check weather the OTP is verifeid
        if(!emailOtp.isVerified()){
            throw new RuntimeException("Please verify your email first!");
        }
        //check weather the regisstration window expired
        if(emailOtp.getRegistrationExpiresAt()==null || !emailOtp.getRegistrationExpiresAt().isAfter(LocalDateTime.now())){
            throw new RuntimeException("Registration session expired. Please verify your email again!");
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
        //find user by email
        User user=userRepository.findByEmail(request.getEmail())
                .orElseThrow(()->new RuntimeException("Invalid email or password"));
        //verify the entered password with the password in db

        boolean isMatch= passwordEncoder.matches(request.getPassword(),user.getPasswordHash());
        if(!isMatch){
            throw new RuntimeException("Invalid email or password");
        }
        if (!user.isEmailVerified()) {
            throw new RuntimeException("Please verify your email before logging in.");
        }
        if(!"ACTIVE".equals(user.getAccountStatus())){
            throw new RuntimeException("Account is Inactive");
        }


        //Generate JWT after a successful Login
        String accessToken = jwtService.generateToken(user.getUserId());
        String refreshToken=refreshTokenService.createRefreshToken(user, "Unknown device");
        return new LoginResponse(accessToken, "Bearer",refreshToken);
    }

}
