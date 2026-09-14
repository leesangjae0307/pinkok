package com.example.pinkok_backend.dto;

import lombok.Getter;

@Getter
public class TokenResponse {

    private final String tokenType = "Bearer";
    private final String accessToken;
    private final long expiresIn;
    private final String refreshToken;
    private final UserResponse user;

    public TokenResponse(String accessToken, long expiresIn, String refreshToken, UserResponse user) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.user = user;
    }
}
