package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.AvatarSelectionRequest;
import com.example.pinkok_backend.dto.UserResponse;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.UserRepository;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * 현재 로그인한 사용자 정보. Authorization: Bearer &lt;accessToken&gt; 필요.
     * 다른 컨트롤러에서 "지금 로그인한 사람"이 필요하면 이 예시처럼
     * {@link CurrentUserId} 를 파라미터에 붙이면 된다.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@CurrentUserId Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * 아바타 선택 · 변경. 회원가입 때 고르지 않았거나 나중에 바꾸고 싶을 때 사용.
     * 목록은 {@code GET /avatars} 로 조회.
     */
    @PatchMapping("/me/avatar")
    public ResponseEntity<UserResponse> selectAvatar(
            @CurrentUserId Long userId,
            @Valid @RequestBody AvatarSelectionRequest request) {
        User user = userService.selectAvatar(userId, request.getAvatarId());
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
