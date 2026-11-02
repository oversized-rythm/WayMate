package com.waymate.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RequestOtpRequest {
    @NotBlank(message = "Email is Required!")
    @Email(message = "Please enter a valid email address!")
    private String email;

    public RequestOtpRequest(){}

    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email=email;
    }
}
