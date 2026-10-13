package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.UserSettingResponse;
import com.example.pinkok_backend.dto.UserSettingUpdateRequest;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.UserSettingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 설정 (알림 받기 · 기본 동행 유형).
 * 테마·언어는 아직 고를 게 없어서 값만 내려주고 바꾸는 기능은 두지 않았다.
 */
@RestController
@RequestMapping("/users/me/settings")
public class UserSettingController {

    private final UserSettingService userSettingService;

    public UserSettingController(UserSettingService userSettingService) {
        this.userSettingService = userSettingService;
    }

    /** 설정 화면을 열 때. 아직 설정한 적이 없으면 기본값으로 만들어서 돌려준다. */
    @GetMapping
    public UserSettingResponse get(@CurrentUserId Long userId) {
        return userSettingService.get(userId);
    }

    /** 보낸 항목만 바뀐다. 예: {"notificationEnabled": false} */
    @PatchMapping
    public UserSettingResponse update(@CurrentUserId Long userId,
                                      @Valid @RequestBody UserSettingUpdateRequest request) {
        return userSettingService.update(userId, request);
    }
}
