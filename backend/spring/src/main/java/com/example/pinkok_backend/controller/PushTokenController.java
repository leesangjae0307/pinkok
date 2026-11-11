package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.PushTokenRequest;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.PushTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 폰 푸시를 받을 기기 토큰. 앱은 로그인 직후(그리고 토큰이 바뀔 때마다) 등록하고, 로그아웃할 때 해제한다.
 */
@RestController
@RequestMapping("/users/me/push-tokens")
public class PushTokenController {

    private final PushTokenService pushTokenService;

    public PushTokenController(PushTokenService pushTokenService) {
        this.pushTokenService = pushTokenService;
    }

    /** 등록. 같은 토큰을 여러 번 보내도 하나만 남는다. */
    @PostMapping
    public ResponseEntity<Void> register(@CurrentUserId Long userId, @Valid @RequestBody PushTokenRequest request) {
        pushTokenService.register(userId, request.getToken(), request.getPlatform());
        return ResponseEntity.noContent().build();
    }

    /** 해제 (로그아웃할 때). 없는 토큰이어도 204. */
    @DeleteMapping
    public ResponseEntity<Void> unregister(@CurrentUserId Long userId, @Valid @RequestBody PushTokenRequest request) {
        pushTokenService.unregister(userId, request.getToken());
        return ResponseEntity.noContent().build();
    }
}
