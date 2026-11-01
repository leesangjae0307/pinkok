package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 한 번의 시도. 이메일로 보낸 인증번호 → 맞으면 재설정 토큰 → 새 비밀번호.
 *
 * <p>인증번호와 재설정 토큰 모두 원문은 저장하지 않고 해시만 저장한다.
 */
@Entity
@Table(name = "password_reset_codes")
@Getter
@Setter
public class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 6자리 인증번호의 해시. */
    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 틀린 횟수. 한도를 넘으면 이 인증번호는 더 이상 쓸 수 없다. */
    @Column(nullable = false)
    private int attempts;

    /** 인증번호를 맞춘 뒤 발급한 재설정 토큰의 해시. */
    @Column(name = "reset_token_hash", length = 64)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    /** 비밀번호를 바꿨거나, 새 인증번호가 발급돼서 무효가 되면 채워짐. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
