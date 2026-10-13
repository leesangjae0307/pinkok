package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.NotificationResponse;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 알림함. 여행 초대, AI 추출 완료, PinLog 완성을 여기서 알려준다.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 내 알림 목록 (최근 100건). */
    @GetMapping
    public List<NotificationResponse> list(@CurrentUserId Long userId) {
        return notificationService.list(userId);
    }

    /** 안 읽은 개수 — 하단 탭의 빨간 뱃지에 쓴다. */
    @GetMapping("/unread-count")
    public Map<String, Integer> unreadCount(@CurrentUserId Long userId) {
        return Map.of("count", notificationService.unreadCount(userId));
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(@CurrentUserId Long userId, @PathVariable Long notificationId) {
        return notificationService.markRead(notificationId, userId);
    }

    /** 모두 읽음. 방금 읽음 처리한 개수를 돌려준다. */
    @PatchMapping("/read-all")
    public Map<String, Integer> markAllRead(@CurrentUserId Long userId) {
        return Map.of("updated", notificationService.markAllRead(userId));
    }
}
