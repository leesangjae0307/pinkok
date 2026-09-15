package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.SignupRequest;
import com.example.pinkok_backend.entity.Avatar;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarService avatarService;
    private final FileService fileService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AvatarService avatarService, FileService fileService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarService = avatarService;
        this.fileService = fileService;
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

    /**
     * 프로필에서 아바타를 고르거나 바꿀 때.
     * 아바타와 프로필 사진은 둘 중 하나만 쓰므로, 올려둔 프로필 사진은 지운다.
     */
    @Transactional
    public User selectAvatar(Long userId, Long avatarId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
        user.setAvatar(avatarService.getEntityOrThrow(avatarId));

        String oldImageUrl = user.getProfileImageUrl();
        user.setProfileImageUrl(null);
        deleteImageAfterCommit(oldImageUrl);

        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /** 닉네임 변경. */
    @Transactional
    public User updateNickname(Long userId, String nickname) {
        User user = getUserOrThrow(userId);
        user.setNickname(nickname.strip());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * 프로필 사진 변경. POST /files 로 먼저 올리고 받은 주소를 넘긴다.
     * 아바타와 프로필 사진은 둘 중 하나만 쓰므로, 고른 아바타는 해제한다.
     */
    @Transactional
    public User updateProfileImage(Long userId, String profileImageUrl) {
        fileService.validateImageUrl(profileImageUrl);

        User user = getUserOrThrow(userId);
        String oldImageUrl = user.getProfileImageUrl();

        user.setProfileImageUrl(profileImageUrl);
        user.setAvatar(null);
        user.setUpdatedAt(LocalDateTime.now());

        // 같은 사진을 다시 고른 경우엔 지우면 안 된다
        if (!Objects.equals(oldImageUrl, profileImageUrl)) {
            deleteImageAfterCommit(oldImageUrl);
        }
        return userRepository.save(user);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    /**
     * 예전 사진 파일은 DB 저장이 끝난 뒤에 지운다.
     * 먼저 지웠다가 DB 저장이 실패하면, DB는 예전 주소를 가리키는데 파일은 없는 상태가 되기 때문이다.
     */
    private void deleteImageAfterCommit(String imageUrl) {
        if (imageUrl == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileService.deleteImageQuietly(imageUrl);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileService.deleteImageQuietly(imageUrl);
            }
        });
    }
}
