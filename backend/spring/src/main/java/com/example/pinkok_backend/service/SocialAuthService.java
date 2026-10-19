package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.TokenResponse;
import com.example.pinkok_backend.entity.Avatar;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.AvatarRepository;
import com.example.pinkok_backend.repository.UserRepository;
import com.example.pinkok_backend.social.GoogleSocialClient;
import com.example.pinkok_backend.social.KakaoSocialClient;
import com.example.pinkok_backend.social.SocialAuthClient;
import com.example.pinkok_backend.social.SocialProfile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 소셜 로그인(카카오·구글). 앱이 SDK 로 받은 토큰을 제공자에게 확인한 뒤, <b>우리 서비스의 토큰</b>을 발급한다.
 * 이후에는 일반 로그인과 똑같이 accessToken / refreshToken 으로 동작한다.
 *
 * <ul>
 *   <li>처음 보는 사용자면 계정을 자동으로 만든다 (아이디·아바타는 자동, 닉네임은 제공자 이름). 응답의 {@code newUser=true}</li>
 *   <li>이메일이 같다는 이유로 기존 이메일 계정과 <b>자동으로 합치지 않는다</b> — 이메일을 가진 사람이 곧 그 계정 주인이라는 보장이 없다.</li>
 * </ul>
 */
@Service
public class SocialAuthService {

    private static final int NICKNAME_MAX = 50;
    private static final String DEFAULT_NICKNAME = "여행자";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, SocialAuthClient> clients;
    private final UserRepository userRepository;
    private final AvatarRepository avatarRepository;
    private final AuthService authService;

    public SocialAuthService(KakaoSocialClient kakao,
                             GoogleSocialClient google,
                             UserRepository userRepository,
                             AvatarRepository avatarRepository,
                             AuthService authService) {
        this.clients = Map.of("KAKAO", kakao, "GOOGLE", google);
        this.userRepository = userRepository;
        this.avatarRepository = avatarRepository;
        this.authService = authService;
    }

    @Transactional
    public TokenResponse login(String providerName, String token) {
        String provider = providerName.toUpperCase(Locale.ROOT);
        SocialAuthClient client = clients.get(provider);
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "지원하지 않는 소셜 로그인입니다: " + providerName + " (kakao, google)");
        }

        SocialProfile profile = client.verify(token);

        User user = userRepository.findByProviderAndProviderUid(provider, profile.uid()).orElse(null);
        if (user != null) {
            if (user.getDeletedAt() != null) {
                throw new BadCredentialsException("탈퇴한 계정입니다.");
            }
            return authService.issueTokens(user, false);
        }
        return authService.issueTokens(createUser(provider, profile), true);
    }

    private User createUser(String provider, SocialProfile profile) {
        String email = null;
        if (StringUtils.hasText(profile.email()) && profile.emailVerified()) {
            if (userRepository.existsByEmail(profile.email())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "이 이메일로 이미 가입된 계정이 있습니다. 이메일과 비밀번호로 로그인해 주세요.");
            }
            email = profile.email();
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setEmail(email);
        user.setProvider(provider);
        user.setProviderUid(profile.uid());
        user.setUsername(newUsername(provider));
        user.setNickname(nicknameOf(profile));
        user.setAvatar(randomAvatar());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    /** 영문 소문자·숫자·밑줄만 (일반 가입의 @아이디 규칙과 같게). 예: kakao_3fa9c01b */
    private String newUsername(String provider) {
        for (int i = 0; i < 10; i++) {
            String candidate = provider.toLowerCase(Locale.ROOT) + "_" + String.format("%08x", RANDOM.nextInt());
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "계정 아이디를 만들지 못했습니다. 다시 시도해 주세요.");
    }

    private String nicknameOf(SocialProfile profile) {
        if (!StringUtils.hasText(profile.nickname())) {
            return DEFAULT_NICKNAME;
        }
        String nickname = profile.nickname().trim();
        return nickname.length() <= NICKNAME_MAX ? nickname : nickname.substring(0, NICKNAME_MAX);
    }

    private Avatar randomAvatar() {
        List<Avatar> avatars = avatarRepository.findAllByOrderByDisplayOrderAsc();
        return avatars.isEmpty() ? null : avatars.get(RANDOM.nextInt(avatars.size()));
    }
}
