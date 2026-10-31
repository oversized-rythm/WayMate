package com.waymate.backend.service;

import com.waymate.backend.dto.LoginRequest;
import com.waymate.backend.dto.LoginResponse;
import com.waymate.backend.dto.RegisterRequest;
import com.waymate.backend.entity.User;
import com.waymate.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,JwtService jwtService){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.jwtService=jwtService;
    }
    public User getUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(()->
                        new RuntimeException("User not Found"));
    }
    //register a new user
    public User registerUser(RegisterRequest request){
        //check if email already exists
        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Email Already Registered!");
        }
        //Creating a new user entity
        User user= new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        //Hash the password
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAccountStatus("ACTIVE");
        return userRepository.save(user);
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
        if(!"ACTIVE".equals(user.getAccountStatus())){
            throw new RuntimeException("Account is Inactive");
        }

        //Generate JWT after a successful Login
        String token = jwtService.generateToken(user.getUserId());
        return new LoginResponse(token, "Bearer");
    }

}
