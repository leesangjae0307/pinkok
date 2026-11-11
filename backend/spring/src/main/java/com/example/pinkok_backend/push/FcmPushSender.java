package com.example.pinkok_backend.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * FCM(Firebase Cloud Messaging)으로 폰 푸시를 보낸다.
 *
 * <p>서비스 계정 키(JSON)가 설정돼 있지 않으면 아무것도 보내지 않는다. 키는 비밀이라 팀원마다
 * 갖고 있지 않을 수 있는데, 그 때문에 서버가 안 켜지면 곤란하기 때문이다 (앱 안 알림함은 그대로 동작한다).
 */
@Component
public class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);

    private static final String APP_NAME = "pinkok";
    /** FCM 이 한 번에 받아주는 최대 토큰 수 */
    private static final int MAX_TOKENS_PER_REQUEST = 500;
    /** 이 오류가 나면 그 토큰은 다시 써도 소용없다 (앱 삭제, 다른 Firebase 프로젝트의 토큰 등) */
    private static final Set<MessagingErrorCode> DEAD_TOKEN_ERRORS =
            Set.of(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.SENDER_ID_MISMATCH);

    private final FirebaseMessaging messaging;

    public FcmPushSender(@Value("${firebase.credentials-path:}") String credentialsPath) {
        this.messaging = initialize(credentialsPath);
    }

    private static FirebaseMessaging initialize(String credentialsPath) {
        if (!StringUtils.hasText(credentialsPath)) {
            log.info("firebase.credentials-path 가 비어 있어 폰 푸시는 보내지 않는다 (앱 안 알림함은 그대로 동작)");
            return null;
        }
        try (InputStream in = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(existing -> APP_NAME.equals(existing.getName()))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(options, APP_NAME));
            log.info("Firebase 초기화 완료 - 폰 푸시를 보낸다");
            return FirebaseMessaging.getInstance(app);
        } catch (IOException | RuntimeException e) {
            // 키 경로가 틀려도 서버는 켜져야 한다. 대신 로그로 크게 남긴다.
            log.error("Firebase 초기화 실패 - 폰 푸시 없이 서버를 띄운다 (path={})", credentialsPath, e);
            return null;
        }
    }

    @Override
    public List<String> send(List<String> tokens, Message message) {
        if (messaging == null || tokens.isEmpty()) {
            return List.of();
        }

        List<String> deadTokens = new ArrayList<>();
        for (int from = 0; from < tokens.size(); from += MAX_TOKENS_PER_REQUEST) {
            List<String> chunk = tokens.subList(from, Math.min(from + MAX_TOKENS_PER_REQUEST, tokens.size()));

            MulticastMessage multicast = MulticastMessage.builder()
                    .addAllTokens(chunk)
                    .setNotification(Notification.builder()
                            .setTitle(message.title())
                            .setBody(message.body())
                            .build())
                    .putAllData(message.data())
                    // 앱이 꺼져 있어도 바로 뜨도록 높은 우선순위로 보낸다
                    .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                    .build();
            try {
                BatchResponse response = messaging.sendEachForMulticast(multicast);
                List<SendResponse> results = response.getResponses();
                for (int i = 0; i < results.size(); i++) {
                    FirebaseMessagingException error = results.get(i).getException();
                    if (error != null && DEAD_TOKEN_ERRORS.contains(error.getMessagingErrorCode())) {
                        deadTokens.add(chunk.get(i));
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.warn("FCM 발송 실패 ({}건)", chunk.size(), e);
            }
        }
        return deadTokens;
    }
}
