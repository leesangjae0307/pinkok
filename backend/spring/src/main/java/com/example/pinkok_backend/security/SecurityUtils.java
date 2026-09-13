package com.example.pinkok_backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 컨트롤러가 아닌 곳(서비스 레이어 등)에서 "지금 로그인한 사람"의 userId 가 필요할 때 쓴다.
 * 컨트롤러라면 파라미터에 {@link CurrentUserId} 를 붙이는 쪽을 우선 고려할 것.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 현재 요청의 인증된 userId 를 반환한다.
     *
     * @throws IllegalStateException 인증 정보가 없을 때 — SecurityConfig 에서
     *         인증이 필요한 요청만 여기까지 오게 막아두므로, 정상 흐름에서는 발생하지 않는다.
     */
    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalStateException("인증된 사용자 정보가 없습니다. 컨트롤러가 인증이 필요한 경로인지 확인하세요.");
        }
        return userId;
    }
}
