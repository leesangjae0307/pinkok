package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.LoginRequest;
import com.example.pinkok_backend.dto.RefreshTokenRequest;
import com.example.pinkok_backend.dto.SignupRequest;
import com.example.pinkok_backend.dto.SocialLoginRequest;
import com.example.pinkok_backend.dto.TokenResponse;
import com.example.pinkok_backend.dto.UserResponse;
import com.example.pinkok_backend.service.AuthService;
import com.example.pinkok_backend.service.SocialAuthService;
import com.example.pinkok_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final SocialAuthService socialAuthService;

    public AuthController(UserService userService, AuthService authService, SocialAuthService socialAuthService) {
        this.userService = userService;
        this.authService = authService;
        this.socialAuthService = socialAuthService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        UserResponse body = UserResponse.from(userService.register(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * 소셜 로그인 (카카오·구글). 앱이 SDK 로 받은 토큰을 보내면 우리 서비스의 토큰을 돌려준다.
     * 카카오는 액세스 토큰, 구글은 ID 토큰이다. 처음이면 계정이 자동으로 만들어지고 응답의 {@code newUser} 가 true 다.
     */
    @PostMapping("/social/{provider}")
    public ResponseEntity<TokenResponse> socialLogin(@PathVariable String provider,
                                                     @Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(socialAuthService.login(provider, request.token()));
    }

    /**
     * 액세스 토큰(1시간)이 만료됐을 때 재로그인 없이 둘 다 새로 받는다.
     * 리프레시 토큰은 한 번 쓰면 그 자리에서 새 것으로 교체된다(rotation) -
     * 응답의 refreshToken으로 매번 갱신해서 다음 refresh에 써야 한다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    /** 이 리프레시 토큰만 폐기한다(다른 기기 세션엔 영향 없음). 액세스 토큰은 만료될 때까지는 그대로 유효하다. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
