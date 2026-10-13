package com.example.pinkok_backend.dto;

import lombok.Getter;
import lombok.Setter;

/** 설정 변경. 보낸 항목만 바뀐다(안 보낸 항목은 그대로). */
@Getter
@Setter
public class UserSettingUpdateRequest {

    /** 알림 받기 */
    private Boolean notificationEnabled;
}
