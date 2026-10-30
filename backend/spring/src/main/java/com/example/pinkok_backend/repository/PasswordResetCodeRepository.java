package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    /** 그 사용자의 가장 최근 인증번호 중 아직 안 쓴 것. */
    Optional<PasswordResetCode> findFirstByUser_IdAndUsedAtIsNullOrderByIdDesc(Long userId);

    Optional<PasswordResetCode> findByResetTokenHashAndUsedAtIsNull(String resetTokenHash);

    List<PasswordResetCode> findAllByUser_IdAndUsedAtIsNull(Long userId);
}
