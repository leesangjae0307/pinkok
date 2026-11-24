package com.example.pinkok_backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 요청을 보낸 쪽의 IP.
 *
 * <p>Cloud Run 앞단(Google 프런트엔드)은 {@code X-Forwarded-For} 의 <b>맨 뒤</b>에 실제 접속자 IP 를 덧붙인다.
 * 맨 앞 값은 클라이언트가 마음대로 적어 보낼 수 있어서 믿지 않고, 맨 뒤 값을 쓴다.
 * 헤더가 없으면(로컬 실행 등) 연결 상대 주소를 쓴다.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String[] hops = forwarded.split(",");
            String last = hops[hops.length - 1].trim();
            if (StringUtils.hasText(last)) {
                return last;
            }
        }
        return request.getRemoteAddr();
    }
}
