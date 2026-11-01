package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.PasswordResetVerifyResponse;
import com.example.pinkok_backend.dto.TokenResponse;
import com.example.pinkok_backend.entity.PasswordResetCode;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.PasswordResetCodeRepository;
import com.example.pinkok_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 비밀번호 변경(로그인한 상태)과 재설정(비밀번호를 잊은 상태).
 *
 * <p>재설정 흐름: 인증번호 발송 → 인증번호 확인(재설정 토큰 발급) → 새 비밀번호 설정.
 * 비밀번호가 바뀌면 그 사용자의 모든 로그인 세션(리프레시 토큰)을 끊는다.
 *
 * <p>잘못된 입력은 401 이 아니라 400 으로 답한다. 프론트의 axios 인터셉터가 401 을 "액세스 토큰
 * 만료"로 보고 토큰 갱신을 시도하기 때문이다.
 */
@Service
public class PasswordService {

    static final long CODE_VALID_MINUTES = 10;
    static final long RESET_TOKEN_VALID_MINUTES = 15;
    static final long RESEND_COOLDOWN_SECONDS = 60;
    static final int MAX_ATTEMPTS = 5;

    private static final String BAD_CODE = "인증번호가 올바르지 않거나 만료되었습니다.";
    private static final String BAD_RESET_TOKEN = "재설정 시간이 지났습니다. 인증번호부터 다시 받아 주세요.";

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuthService authService;
    private final PasswordResetMailer mailer;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordService(UserRepository userRepository, PasswordResetCodeRepository codeRepository,
                           PasswordEncoder passwordEncoder, RefreshTokenService refreshTokenService,
                           AuthService authService, PasswordResetMailer mailer) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.authService = authService;
        this.mailer = mailer;
    }

    /**
     * 로그인한 상태에서 비밀번호 변경. 다른 기기의 로그인은 모두 끊고, 지금 기기는 새 토큰을 받아
     * 로그인을 이어간다.
     */
    @Transactional
    public TokenResponse change(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if (user.getPasswordHash() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "소셜 로그인 계정은 비밀번호가 없어서 변경할 수 없습니다.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 비밀번호와 다른 비밀번호를 입력해 주세요.");
        }
        applyNewPassword(user, newPassword);
        return authService.issueTokens(user, false);
    }

    /**
     * 인증번호를 이메일로 보낸다. 가입된 이메일인지 알 수 없도록 항상 같은 응답을 준다
     * (없는 이메일·소셜 계정·탈퇴 계정·재발송 대기 중이어도 아무 일 없이 끝난다).
     */
    @Transactional
    public void requestReset(String email) {
        Optional<User> found = userRepository.findByEmail(email.strip());
        if (found.isEmpty()) {
            return;
        }
        User user = found.get();
        if (user.getDeletedAt() != null || user.getPasswordHash() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<PasswordResetCode> latest = codeRepository.findFirstByUser_IdAndUsedAtIsNullOrderByIdDesc(user.getId());
        if (latest.isPresent()
                && latest.get().getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            return; // 너무 자주 요청 — 메일 폭탄 방지
        }
        // 이전에 보낸 인증번호는 모두 무효로 한다
        codeRepository.findAllByUser_IdAndUsedAtIsNull(user.getId()).forEach(c -> c.setUsedAt(now));

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        PasswordResetCode entity = new PasswordResetCode();
        entity.setUser(user);
        entity.setCodeHash(hashCode(user.getId(), code));
        entity.setExpiresAt(now.plusMinutes(CODE_VALID_MINUTES));
        entity.setCreatedAt(now);
        codeRepository.save(entity);

        mailer.sendCode(user.getEmail(), code, CODE_VALID_MINUTES);
    }

    /**
     * 인증번호를 확인하고 재설정 토큰을 준다. 틀린 횟수는 롤백되지 않고 남아야 하므로
     * {@code noRollbackFor} 를 건다.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public PasswordResetVerifyResponse verify(String email, String code) {
        User user = userRepository.findByEmail(email.strip())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_CODE));
        LocalDateTime now = LocalDateTime.now();

        PasswordResetCode entry = codeRepository.findFirstByUser_IdAndUsedAtIsNullOrderByIdDesc(user.getId())
                .filter(c -> c.getExpiresAt().isAfter(now) && c.getAttempts() < MAX_ATTEMPTS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_CODE));

        if (!constantTimeEquals(entry.getCodeHash(), hashCode(user.getId(), code))) {
            entry.setAttempts(entry.getAttempts() + 1);
            codeRepository.save(entry);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_CODE);
        }

        String resetToken = randomToken();
        entry.setResetTokenHash(sha256Hex(resetToken));
        entry.setResetTokenExpiresAt(now.plusMinutes(RESET_TOKEN_VALID_MINUTES));
        codeRepository.save(entry);
        return new PasswordResetVerifyResponse(resetToken, Duration.ofMinutes(RESET_TOKEN_VALID_MINUTES).toSeconds());
    }

    /** 재설정 토큰으로 새 비밀번호를 정한다. 토큰은 한 번만 쓸 수 있다. */
    @Transactional
    public void confirmReset(String resetToken, String newPassword) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetCode entry = codeRepository.findByResetTokenHashAndUsedAtIsNull(sha256Hex(resetToken))
                .filter(c -> c.getResetTokenExpiresAt() != null && c.getResetTokenExpiresAt().isAfter(now))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_RESET_TOKEN));
        User user = entry.getUser();
        if (user.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_RESET_TOKEN);
        }
        entry.setUsedAt(now);
        codeRepository.save(entry);
        applyNewPassword(user, newPassword);
    }

    private void applyNewPassword(User user, String newPassword) {
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        refreshTokenService.deleteAllForUser(user.getId());
    }

    /** 6자리는 경우의 수가 적으니 사용자 ID 를 섞어 해시한다. 시도 횟수 제한이 실제 방어선이다. */
    private String hashCode(Long userId, String code) {
        return sha256Hex(userId + ":" + code);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
