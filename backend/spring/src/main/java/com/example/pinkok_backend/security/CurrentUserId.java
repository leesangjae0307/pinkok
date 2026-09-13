package com.example.pinkok_backend.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * "지금 로그인한 사람"의 userId 를 컨트롤러 파라미터로 바로 받는 애너테이션.
 *
 * <p>JwtAuthenticationFilter 가 토큰을 검증하면서 SecurityContext 의 principal 로
 * userId(Long) 를 넣어준다. 이 애너테이션은 그걸 꺼내오는 걸 짧게 쓰기 위한 것뿐이고,
 * 별도의 설정(Resolver 등록)이 필요 없다 — {@code @AuthenticationPrincipal} 의 별칭이다.
 *
 * <h3>사용 예</h3>
 * <pre>{@code
 * @GetMapping("/diaries")
 * public List<DiaryResponse> myDiaries(@CurrentUserId Long userId) {
 *     return diaryService.findAllByUser(userId);
 * }
 * }</pre>
 *
 * <p>토큰이 없거나 유효하지 않은 요청은 이 값이 채워지기 전에 SecurityConfig 에서
 * 이미 401 로 막힌다 (아래 예외: {@code /auth/signup}, {@code /auth/login}).
 * 서비스 레이어(리포지토리 접근 등)에서 필요하면 {@link SecurityUtils#currentUserId()} 를 쓴다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal
public @interface CurrentUserId {
}
