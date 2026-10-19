package com.example.pinkok_backend.social;

/**
 * 앱이 소셜 SDK 로 받아 온 토큰을 제공자에게 직접 확인하는 클라이언트.
 * 토큰이 진짜인지, <b>우리 앱에 발급된 것인지</b>까지 확인해야 한다 (다른 앱에서 발급된 토큰으로 로그인되면 안 된다).
 */
public interface SocialAuthClient {

    /**
     * @throws org.springframework.web.server.ResponseStatusException 토큰이 잘못됐으면 401,
     *         서버에 제공자 설정이 없으면 503, 제공자 호출이 실패하면 502
     */
    SocialProfile verify(String token);
}
