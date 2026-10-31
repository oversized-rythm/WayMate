package com.waymate.backend.service;

import com.waymate.backend.dto.RegisterRequest;
import com.waymate.backend.entity.User;
import com.waymate.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
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

}
