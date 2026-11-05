package com.waymate.backend.dto;

public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private String refreshToken;

    public LoginResponse() {}

    public LoginResponse(String accessToken, String tokenType, String refreshToken) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.refreshToken=refreshToken;
    }

    public String getRefreshToken(){
        return refreshToken;
    }
    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }
}
