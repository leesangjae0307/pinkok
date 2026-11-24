package com.example.pinkok_backend.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 중복 확인 API 의 IP 별 속도 제한. 한도를 3 으로 낮춘 별도 설정으로 확인한다. */
@SpringBootTest(properties = "auth.check.rate-limit-per-minute=3")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountCheckRateLimitTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("같은 IP 가 한도를 넘으면 429, 다른 IP 는 영향이 없고, 이메일·아이디 확인이 한도를 같이 쓴다")
    void rateLimit_isPerIp() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/auth/check-email").param("email", "x" + i + "@pinkok.com")
                            .header("X-Forwarded-For", "10.0.0.1"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/auth/check-username").param("username", "limit_user")
                        .header("X-Forwarded-For", "10.0.0.1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/auth/check-email").param("email", "y@pinkok.com")
                        .header("X-Forwarded-For", "10.0.0.1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));

        // 맨 앞 값은 클라이언트가 지어낼 수 있어서 쓰지 않는다 - 맨 뒤 값이 같으면 같은 IP 로 본다
        mockMvc.perform(get("/auth/check-email").param("email", "z@pinkok.com")
                        .header("X-Forwarded-For", "1.2.3.4, 10.0.0.1"))
                .andExpect(status().isTooManyRequests());

        // 다른 IP 는 따로 센다
        mockMvc.perform(get("/auth/check-email").param("email", "z@pinkok.com")
                        .header("X-Forwarded-For", "10.0.0.2"))
                .andExpect(status().isOk());
    }
}
