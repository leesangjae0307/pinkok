package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.SignupRequest;
import com.example.pinkok_backend.entity.Avatar;
import com.example.pinkok_backend.entity.TripMember;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.TripMemberRepository;
import com.example.pinkok_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {

    private static final String OWNER = "OWNER";
    private static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarService avatarService;
    private final FileService fileService;
    private final TripMemberRepository tripMemberRepository;
    private final RefreshTokenService refreshTokenService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AvatarService avatarService, FileService fileService,
                       TripMemberRepository tripMemberRepository, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarService = avatarService;
        this.fileService = fileService;
        this.tripMemberRepository = tripMemberRepository;
        this.refreshTokenService = refreshTokenService;
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
        fileService.deleteAfterCommit(oldImageUrl);

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
            fileService.deleteAfterCommit(oldImageUrl);
        }
        return userRepository.save(user);
    }

    /**
     * 회원 탈퇴. 행을 지우지 않고 <b>지웠다고 표시</b>하고 개인정보만 없앤다(소프트 삭제).
     *
     * <p>여행을 여럿이 함께 쓰기 때문이다. 정훈이 탈퇴했다고 그가 남긴 기록과 영상을 지워버리면
     * 같이 간 사람의 여행 화면과 PinLog 영상이 깨진다. 그래서 기록은 남기고,
     * 이메일·닉네임·프로필 사진 같은 <b>개인을 알아볼 수 있는 정보만</b> 지운다.
     *
     * <p>만든 여행(OWNER)이 있으면 탈퇴를 막는다. 주인이 사라진 여행은 아무도 수정·삭제할 수
     * 없게 되기 때문이다. 먼저 그 여행을 삭제하고 탈퇴하면 된다.
     * (주인을 다른 팀원에게 넘기는 기능은 아직 없다)
     */
    @Transactional
    public void delete(Long userId, String password) {
        User user = getUserOrThrow(userId);

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다.");
        }

        List<TripMember> ownedTrips = tripMemberRepository.findAllByUser_IdOrderByJoinedAtDesc(userId).stream()
                .filter(member -> OWNER.equals(member.getRole()))
                .filter(member -> member.getTrip().getDeletedAt() == null)
                .toList();
        if (!ownedTrips.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "내가 만든 여행이 " + ownedTrips.size() + "개 있습니다. 먼저 정리한 뒤에 탈퇴해주세요.");
        }

        String oldProfileImageUrl = user.getProfileImageUrl();

        user.setDeletedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        // 같은 이메일로 다시 가입할 수 있도록 비운다 (이메일에 UNIQUE 제약이 있다)
        user.setEmail(null);
        user.setPasswordHash(null);
        user.setNickname(WITHDRAWN_NICKNAME);
        user.setProfileImageUrl(null);
        user.setAvatar(null);
        user.setProviderUid(null);
        userRepository.save(user);

        // 아이디(username)는 남긴다. 팀원 목록이나 기록 작성자를 추적할 수 있어야 하고,
        // 지워버리면 남이 같은 아이디로 가입해 "그 사람인 척" 할 수 있다.

        refreshTokenService.revokeAllForUser(userId);
        fileService.deleteAfterCommit(oldProfileImageUrl);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

}
