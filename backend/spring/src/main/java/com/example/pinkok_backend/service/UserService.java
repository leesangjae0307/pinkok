package com.example.pinkok_backend.service;

import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(User user) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // users.provider 컬럼이 NOT NULL 이므로 일반 회원가입은 LOCAL 로 지정
        if (user.getProvider() == null) {
            user.setProvider("LOCAL");
        }

        return userRepository.save(user);
    }
}
