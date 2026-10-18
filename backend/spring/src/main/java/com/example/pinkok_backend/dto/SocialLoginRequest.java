package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param token 앱이 소셜 SDK 로 받은 토큰. 카카오는 <b>액세스 토큰</b>, 구글은 <b>ID 토큰</b>.
 */
public record SocialLoginRequest(
        @NotBlank(message = "token 은 필수입니다.")
        String token
) {
}
