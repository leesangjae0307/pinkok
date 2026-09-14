package com.example.pinkok_backend.service;

import com.example.pinkok_backend.entity.RefreshToken;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 리프레시 토큰 발급 · 검증 · 재발급(rotate) · 폐기.
 *
 * <p>액세스 토큰(JWT)은 서버에 저장하지 않지만, 리프레시 토큰은 즉시 로그아웃/탈취
 * 대응을 위해 서버에 저장하고 대조한다. 원문은 절대 저장하지 않고 SHA-256 해시만
 * 저장한다 (DB가 유출돼도 토큰 자체를 재구성할 수 없게).
 *
 * <p>재발급할 때마다 이전 토큰은 폐기하고 새 토큰을 발급한다(rotation). 이미 폐기된
 * 토큰으로 다시 재발급을 시도하면 "탈취돼서 원래 주인과 공격자가 같이 쓰고 있는" 상황일
 * 수 있으므로, 그 사용자의 모든 리프레시 토큰을 폐기해 강제로 다시 로그인하게 만든다.
 */
@Service
public class RefreshTokenService {

    private static final String REUSE_DETECTED =
            "이미 사용된 리프레시 토큰입니다. 보안을 위해 다시 로그인해주세요.";
    private static final String INVALID_OR_EXPIRED =
            "리프레시 토큰이 유효하지 않거나 만료되었습니다. 다시 로그인해주세요.";

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenValidityMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidityMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    public long getRefreshTokenValidityMs() {
        return refreshTokenValidityMs;
    }

    /** 로그인 성공 시 새 리프레시 토큰을 발급한다. 원문을 반환하니 응답으로만 내보내고 저장하지 않는다. */
    @Transactional
    public String issue(User user) {
        return save(user);
    }

    /**
     * 리프레시 토큰으로 새 액세스 토큰을 발급받기 위해 검증하고, 토큰 자체도 새로 교체한다.
     *
     * @return 검증에 성공한 사용자와 새로 발급된 리프레시 토큰 원문
     */
    @Transactional
    public RotatedToken rotate(String rawToken) {
        String hash = sha256Hex(rawToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_OR_EXPIRED));

        LocalDateTime now = LocalDateTime.now();

        if (existing.getRevokedAt() != null) {
            // 이미 폐기된(=한 번 썼던) 토큰이 다시 들어옴 -> 탈취 의심, 그 사용자 전체 세션 강제 종료
            revokeAllForUser(existing.getUser().getId(), now);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, REUSE_DETECTED);
        }
        if (!existing.isActive(now)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_OR_EXPIRED);
        }

        existing.setRevokedAt(now);
        refreshTokenRepository.save(existing);

        String newRawToken = save(existing.getUser());
        return new RotatedToken(existing.getUser(), newRawToken);
    }

    /** 로그아웃. 이미 없거나 폐기된 토큰이어도 조용히 넘어간다(로그아웃은 멱등해도 됨). */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256Hex(rawToken))
                .ifPresent(token -> {
                    if (token.getRevokedAt() == null) {
                        token.setRevokedAt(LocalDateTime.now());
                        refreshTokenRepository.save(token);
                    }
                });
    }

    private void revokeAllForUser(Long userId, LocalDateTime now) {
        refreshTokenRepository.findAllByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(userId, now)
                .forEach(token -> token.setRevokedAt(now));
    }

    private String save(User user) {
        String rawToken = generateRawToken();
        LocalDateTime now = LocalDateTime.now();

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(sha256Hex(rawToken));
        token.setExpiresAt(now.plus(java.time.Duration.ofMillis(refreshTokenValidityMs)));
        token.setCreatedAt(now);
        refreshTokenRepository.save(token);

        return rawToken;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    public record RotatedToken(User user, String rawToken) {
    }
}
