package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.LoginRequest;
import com.example.pinkok_backend.dto.TokenResponse;
import com.example.pinkok_backend.dto.UserResponse;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.jwt.JwtTokenProvider;
import com.example.pinkok_backend.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "이메일 또는 비밀번호가 올바르지 않습니다.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));

        if (user.getDeletedAt() != null) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        return issueTokens(user);
    }

    /** 액세스 토큰이 만료됐을 때, 리프레시 토큰으로 둘 다 새로 받는다 (재로그인 없이). */
    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(rawRefreshToken);
        return buildTokenResponse(rotated.user(), rotated.rawToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private TokenResponse issueTokens(User user) {
        String refreshToken = refreshTokenService.issue(user);
        return buildTokenResponse(user, refreshToken);
    }

    private TokenResponse buildTokenResponse(User user, String refreshToken) {
        String accessToken = tokenProvider.createAccessToken(user.getId(), user.getEmail());
        return new TokenResponse(
                accessToken,
                tokenProvider.getAccessTokenValidityMs() / 1000,
                refreshToken,
                UserResponse.from(user));
    }
}
