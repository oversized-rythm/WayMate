package com.waymate.backend.dto;

public class RequestOtpRequest {
    private String email;

    public RequestOtpRequest(){}

    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email=email;
    }
}
