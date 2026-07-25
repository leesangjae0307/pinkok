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

        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }
}
