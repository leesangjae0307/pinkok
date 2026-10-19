package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.UserSetting;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 설정.
 *
 * <p>테마와 언어는 값만 내려주고 바꾸는 기능은 없다. 앱 디자인이 도트 테마 하나뿐이고
 * 한국어만 지원해서 고를 게 없기 때문이다. 나중에 늘어나면 그때 수정 기능을 연다.
 */
@Getter
public class UserSettingResponse {

    /** 알림 받기. 끄면 폰 푸시 알림을 보내지 않는다 (앱 안 알림함에는 계속 쌓인다). */
    private final boolean notificationEnabled;
    /** 지금은 PIXEL_MINT 고정 */
    private final String theme;
    /** 지금은 ko 고정 */
    private final String language;
    private final LocalDateTime updatedAt;

    private UserSettingResponse(UserSetting setting) {
        this.notificationEnabled = Boolean.TRUE.equals(setting.getNotificationEnabled());
        this.theme = setting.getTheme();
        this.language = setting.getLanguage();
        this.updatedAt = setting.getUpdatedAt();
    }

    public static UserSettingResponse from(UserSetting setting) {
        return new UserSettingResponse(setting);
    }
}
