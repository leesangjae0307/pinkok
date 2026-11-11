package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 폰 푸시를 받을 기기 토큰 등록·해제. */
@Getter
@Setter
public class PushTokenRequest {

    /** 앱이 Firebase 에서 받은 FCM 토큰 */
    @NotBlank
    @Size(max = 512)
    private String token;

    /** ANDROID / IOS. 비워두면 ANDROID. 해제할 때는 보내지 않아도 된다. */
    private String platform;
}
