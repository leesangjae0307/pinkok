package com.example.pinkok_backend.dto;

/**
 * 이메일·아이디 사용 가능 여부 (가입 화면의 중복 확인).
 *
 * @param available 사용할 수 있으면 true, 이미 쓰고 있으면 false
 */
public record AvailabilityResponse(boolean available) {
}
