package com.example.pinkok_backend.service;

import com.example.pinkok_backend.entity.PushToken;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.PushTokenRepository;
import com.example.pinkok_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/** 폰 푸시를 받을 기기 토큰 관리. 앱이 로그인할 때 등록하고, 로그아웃할 때 해제한다. */
@Service
public class PushTokenService {

    private static final Set<String> PLATFORMS = Set.of("ANDROID", "IOS");

    private final PushTokenRepository pushTokenRepository;
    private final UserRepository userRepository;

    public PushTokenService(PushTokenRepository pushTokenRepository, UserRepository userRepository) {
        this.pushTokenRepository = pushTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * 토큰을 등록한다. 이미 있는 토큰이면 주인을 지금 로그인한 사람으로 바꾼다.
     * 같은 폰에서 계정을 바꿔 로그인한 경우, 이전 계정의 알림이 그 폰으로 가면 안 되기 때문이다.
     */
    @Transactional
    public void register(Long userId, String token, String platform) {
        String normalizedPlatform = normalizePlatform(platform);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        PushToken pushToken = pushTokenRepository.findByToken(token.trim()).orElseGet(() -> {
            PushToken created = new PushToken();
            created.setToken(token.trim());
            created.setCreatedAt(now);
            return created;
        });
        pushToken.setUser(user);
        pushToken.setPlatform(normalizedPlatform);
        pushToken.setUpdatedAt(now);
        pushTokenRepository.save(pushToken);
    }

    /** 로그아웃할 때. 내 토큰이 아니거나 이미 없으면 조용히 넘어간다. */
    @Transactional
    public void unregister(Long userId, String token) {
        pushTokenRepository.findByToken(token.trim())
                .filter(found -> found.getUser().getId().equals(userId))
                .ifPresent(pushTokenRepository::delete);
    }

    /** 탈퇴할 때 그 사람의 모든 기기 토큰을 지운다 (기기 토큰도 개인정보다). */
    @Transactional
    public void removeAllOf(Long userId) {
        pushTokenRepository.deleteAll(pushTokenRepository.findAllByUser_Id(userId));
    }

    /** FCM 이 더 이상 쓸 수 없다고 알려준 토큰을 지운다 (앱을 지운 폰 등). */
    @Transactional
    public void removeTokens(Collection<String> tokens) {
        if (!tokens.isEmpty()) {
            pushTokenRepository.deleteAll(pushTokenRepository.findAllByTokenIn(tokens));
        }
    }

    private String normalizePlatform(String platform) {
        if (!StringUtils.hasText(platform)) {
            return "ANDROID";
        }
        String normalized = platform.trim().toUpperCase(Locale.ROOT);
        if (!PLATFORMS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "platform 은 ANDROID 또는 IOS 여야 합니다.");
        }
        return normalized;
    }
}
