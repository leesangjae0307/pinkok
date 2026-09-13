package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.AvatarResponse;
import com.example.pinkok_backend.service.AvatarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 기본 도트 아바타 6종(남3 · 여3) 목록. 로그인 전(회원가입 화면)에서도 볼 수 있어야
 * 하므로 인증 없이 열려있다 (SecurityConfig 참고).
 */
@RestController
@RequestMapping("/avatars")
public class AvatarController {

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @GetMapping
    public List<AvatarResponse> list() {
        return avatarService.getAll();
    }
}
