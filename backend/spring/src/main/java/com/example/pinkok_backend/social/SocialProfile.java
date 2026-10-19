package com.example.pinkok_backend.social;

/**
 * 소셜 로그인 제공자(카카오·구글)가 "이 토큰의 주인은 이 사람"이라고 확인해 준 정보.
 *
 * @param provider      KAKAO / GOOGLE
 * @param uid           제공자 쪽 사용자 고유 ID (우리 DB의 provider_uid)
 * @param email         이메일 (사용자가 동의 안 했으면 null)
 * @param emailVerified 제공자가 이 이메일을 확인된 것으로 보증하는지. false 면 이메일로 계정을 엮지 않는다
 * @param nickname      제공자의 표시 이름 (없으면 null)
 */
public record SocialProfile(String provider, String uid, String email, boolean emailVerified, String nickname) {
}
