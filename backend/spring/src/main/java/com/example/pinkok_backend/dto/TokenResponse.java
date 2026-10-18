package com.example.pinkok_backend.dto;

import lombok.Getter;

@Getter
public class TokenResponse {

    private final String tokenType = "Bearer";
    private final String accessToken;
    private final long expiresIn;
    private final String refreshToken;
    private final UserResponse user;
    /** 소셜 로그인으로 방금 계정이 만들어졌으면 true (앱이 닉네임·아바타·취향 설정 화면을 보여줄 때 쓴다). 그 외에는 항상 false */
    private final boolean newUser;

    public TokenResponse(String accessToken, long expiresIn, String refreshToken, UserResponse user) {
        this(accessToken, expiresIn, refreshToken, user, false);
    }

    public TokenResponse(String accessToken, long expiresIn, String refreshToken, UserResponse user, boolean newUser) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.user = user;
        this.newUser = newUser;
    }
}
