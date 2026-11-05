package com.waymate.backend.dto;

public class RequestOtpResponse {
    private String message;

    //no arg constructor to create an empty object when converting JSON into Java Objects
    public RequestOtpResponse(){}

    public RequestOtpResponse(String message){
        this.message=message;
    }
    public String getMessage(){
        return message;
    }
    public void setMessage(String message){
        this.message=message;
    }
}
