package com.waymate.backend.controller;

import com.waymate.backend.dto.LoginRequest;
import com.waymate.backend.dto.LoginResponse;
import com.waymate.backend.dto.RegisterRequest;

import com.waymate.backend.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService){
        this.userService=userService;
    }

    //Register a new User
    @PostMapping("/register")
    public ResponseEntity<String>register(
            @RequestBody RegisterRequest request){
        userService.registerUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("User Registered Successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse>login(@RequestBody LoginRequest request){
        LoginResponse response=userService.loginUser(request);
        return ResponseEntity.ok(response);
    }

}
