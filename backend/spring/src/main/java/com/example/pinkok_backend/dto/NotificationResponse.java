package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.Notification;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 알림 한 건.
 *
 * <p>{@code targetType} 과 {@code targetId} 는 "눌렀을 때 어디로 갈지"다.
 * 예를 들어 초대 알림은 INVITATION + 초대 ID 라서, 앱은 그 화면에서 수락/거절을 띄우면 된다.
 */
@Getter
public class NotificationResponse {

    private final Long id;
    /** TRIP_INVITED / AI_EXTRACT_DONE / PINLOG_DONE */
    private final String type;
    private final String title;
    private final String body;
    /** TRIP / PINLOG / AI_REQUEST / INVITATION */
    private final String targetType;
    private final Long targetId;
    private final boolean read;
    private final LocalDateTime createdAt;

    private NotificationResponse(Notification notification) {
        this.id = notification.getId();
        this.type = notification.getType();
        this.title = notification.getTitle();
        this.body = notification.getBody();
        this.targetType = notification.getTargetType();
        this.targetId = notification.getTargetId();
        this.read = Boolean.TRUE.equals(notification.getIsRead());
        this.createdAt = notification.getCreatedAt();
    }

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification);
    }
}
