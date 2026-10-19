package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(Long userId, LocalDateTime now);

    /** 탈퇴할 때 그 사람의 모든 세션을 끊는다 (기기 여러 대에 로그인돼 있을 수 있다). */
    List<RefreshToken> findAllByUser_IdAndRevokedAtIsNull(Long userId);

}
