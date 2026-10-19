package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.NotificationResponse;
import com.example.pinkok_backend.entity.Notification;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.NotificationRepository;
import com.example.pinkok_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림. 오래 걸리거나 다른 사람이 일으킨 일을 사용자에게 알려준다.
 *
 * <ul>
 *   <li>{@code TRIP_INVITED} — 여행에 초대받음 (수락/거절 필요)</li>
 *   <li>{@code AI_EXTRACT_DONE} — AI 장소 추출이 끝남</li>
 *   <li>{@code PINLOG_DONE} — PinLog 영상 합성이 끝남</li>
 * </ul>
 *
 * <p>뒤의 둘은 수십 초가 걸려서 그 사이 사용자가 앱을 꺼둘 수 있다. 그래서 결과를 알림으로 남긴다.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public static final String TYPE_TRIP_INVITED = "TRIP_INVITED";
    public static final String TYPE_AI_EXTRACT_DONE = "AI_EXTRACT_DONE";
    public static final String TYPE_PINLOG_DONE = "PINLOG_DONE";

    public static final String TARGET_TRIP = "TRIP";
    public static final String TARGET_PINLOG = "PINLOG";
    public static final String TARGET_AI_REQUEST = "AI_REQUEST";
    public static final String TARGET_INVITATION = "INVITATION";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------
    // 알림 보내기 (다른 서비스가 부른다)
    // ------------------------------------------------------------------

    /**
     * 알림 하나를 남긴다.
     *
     * <p><b>알림을 못 남겼다고 원래 작업까지 실패시키지 않는다.</b> 초대는 됐는데 알림 저장에서
     * 터져서 초대 자체가 취소되면 더 곤란하기 때문이다. 실패는 로그로만 남긴다.
     */
    @Transactional
    public void notify(User user, String type, String title, String body, String targetType, Long targetId) {
        try {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType(type);
            notification.setTitle(truncate(title, 100));
            notification.setBody(truncate(body, 500));
            notification.setTargetType(targetType);
            notification.setTargetId(targetId);
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);

        } catch (RuntimeException e) {
            log.warn("알림을 남기지 못했다 (userId={}, type={})", user.getId(), type, e);
        }
    }

    /** 받는 사람을 ID로만 알고 있을 때. */
    @Transactional
    public void notify(Long userId, String type, String title, String body, String targetType, Long targetId) {
        userRepository.findById(userId)
                .ifPresent(user -> notify(user, type, title, body, targetType, targetId));
    }

    // ------------------------------------------------------------------
    // 알림함
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId) {
        return notificationRepository.findTop100ByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /** 하단 탭에 띄울 빨간 점(안 읽은 개수). */
    @Transactional(readOnly = true)
    public int unreadCount(Long userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));
        if (!notification.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 알림만 읽음 처리할 수 있습니다.");
        }

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return NotificationResponse.from(notification);
    }

    /** 모두 읽음. 읽은 개수를 돌려준다. */
    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId, LocalDateTime.now());
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
