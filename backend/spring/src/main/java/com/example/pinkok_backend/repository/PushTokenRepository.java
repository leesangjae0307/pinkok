package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByToken(String token);

    List<PushToken> findAllByUser_Id(Long userId);

    List<PushToken> findAllByTokenIn(Collection<String> tokens);
}
