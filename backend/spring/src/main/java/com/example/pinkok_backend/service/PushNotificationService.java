package com.example.pinkok_backend.service;

import com.example.pinkok_backend.entity.Notification;
import com.example.pinkok_backend.entity.PushToken;
import com.example.pinkok_backend.push.PushSender;
import com.example.pinkok_backend.repository.PushTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 알림함에 남긴 알림을 폰 푸시로도 보낸다.
 *
 * <ul>
 *   <li><b>저장이 확정된 뒤에</b> 보낸다. 초대가 롤백됐는데 "초대받았어요" 푸시만 가면 안 되기 때문이다.</li>
 *   <li><b>별도 스레드</b>에서 보낸다. FCM 이 느리면 초대 버튼을 누른 사람까지 기다리게 되기 때문이다.</li>
 *   <li>설정에서 "알림 받기"를 끈 사람에게는 보내지 않는다. 앱 안 알림함에는 그대로 쌓인다.</li>
 *   <li>푸시가 실패해도 아무 데도 영향을 주지 않는다. 알림함이 진짜 기록이고 푸시는 알려주는 수단일 뿐이다.</li>
 * </ul>
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushTokenRepository pushTokenRepository;
    private final PushTokenService pushTokenService;
    private final UserSettingService userSettingService;
    private final PushSender pushSender;
    private final TaskExecutor pushTaskExecutor;

    public PushNotificationService(PushTokenRepository pushTokenRepository,
                                   PushTokenService pushTokenService,
                                   UserSettingService userSettingService,
                                   PushSender pushSender,
                                   @Qualifier("pushTaskExecutor") TaskExecutor pushTaskExecutor) {
        this.pushTokenRepository = pushTokenRepository;
        this.pushTokenService = pushTokenService;
        this.userSettingService = userSettingService;
        this.pushSender = pushSender;
        this.pushTaskExecutor = pushTaskExecutor;
    }

    /** 방금 저장한 알림을 저장이 확정된 뒤에 폰 푸시로 보낸다. */
    public void sendAfterCommit(Notification notification) {
        Long userId = notification.getUser().getId();
        PushSender.Message message = new PushSender.Message(
                notification.getTitle(), notification.getBody(), dataOf(notification));

        Runnable task = () -> {
            try {
                pushTaskExecutor.execute(() -> send(userId, message));
            } catch (RuntimeException e) {
                // 대기열이 꽉 찬 경우 등. 이미 커밋된 원래 작업(초대 등)에 영향을 주면 안 된다.
                log.warn("폰 푸시를 대기열에 넣지 못했다 (userId={})", userId, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    void send(Long userId, PushSender.Message message) {
        try {
            if (!userSettingService.isNotificationEnabled(userId)) {
                return;
            }
            List<String> tokens = pushTokenRepository.findAllByUser_Id(userId).stream()
                    .map(PushToken::getToken)
                    .toList();
            if (tokens.isEmpty()) {
                return;
            }
            List<String> deadTokens = pushSender.send(tokens, message);
            pushTokenService.removeTokens(deadTokens);
        } catch (RuntimeException e) {
            log.warn("폰 푸시를 보내지 못했다 (userId={})", userId, e);
        }
    }

    /** 앱이 알림을 눌렀을 때 어느 화면으로 갈지 정하는 값. 알림함 응답과 같은 값을 넣는다. FCM 은 문자열만 받는다. */
    private Map<String, String> dataOf(Notification notification) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("notificationId", String.valueOf(notification.getId()));
        data.put("type", notification.getType());
        if (notification.getTargetType() != null) {
            data.put("targetType", notification.getTargetType());
        }
        if (notification.getTargetId() != null) {
            data.put("targetId", String.valueOf(notification.getTargetId()));
        }
        return data;
    }
}
