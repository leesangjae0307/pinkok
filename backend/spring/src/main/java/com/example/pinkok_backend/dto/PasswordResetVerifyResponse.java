package com.example.pinkok_backend.dto;

/** 인증번호가 맞으면 받는 일회용 토큰. 새 비밀번호를 정할 때 함께 보낸다. */
public record PasswordResetVerifyResponse(String resetToken, long expiresIn) {
}
