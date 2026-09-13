package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.SignupRequest;
import com.example.pinkok_backend.entity.Avatar;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarService avatarService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AvatarService avatarService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarService = avatarService;
    }

    @Transactional
    public User register(SignupRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setProvider("LOCAL");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        if (request.getAvatarId() != null) {
            Avatar avatar = avatarService.getEntityOrThrow(request.getAvatarId());
            user.setAvatar(avatar);
        }

        return userRepository.save(user);
    }

    /** 프로필에서 아바타를 고르거나 바꿀 때. */
    @Transactional
    public User selectAvatar(Long userId, Long avatarId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
        user.setAvatar(avatarService.getEntityOrThrow(avatarId));
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
