package com.example.pinkok_backend.auth;

import com.example.pinkok_backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 리프레시 토큰: 로그인 시 발급 -> /auth/refresh 로 재발급(rotation) -> /auth/logout 으로 폐기,
 * 그리고 폐기된(이미 쓴) 토큰 재사용 시 탈취로 간주해 전체 세션을 끊는 것까지 확인.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RefreshTokenTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    private static final String EMAIL = "refresh@pinkok.com";
    private static final String PASSWORD = "pass1234";

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"username\":\"refreshuser\",\"password\":\"" + PASSWORD + "\",\"nickname\":\"테스터\"}"))
                .andExpect(status().isCreated());
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DisplayName("로그인 응답에 refreshToken이 같이 온다")
    void login_includesRefreshToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("리프레시 토큰으로 새 액세스/리프레시 토큰을 받을 수 있다 (재로그인 없이)")
    void refresh_issuesNewTokens() throws Exception {
        String refreshToken = JsonPath.read(login().getResponse().getContentAsString(), "$.refreshToken");

        MvcResult refreshed = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String newRefreshToken = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.refreshToken");
        assertNotEquals(refreshToken, newRefreshToken, "재발급 때마다 토큰이 새로 교체(rotation)돼야 한다");
    }

    @Test
    @DisplayName("이미 재발급에 쓴(폐기된) 리프레시 토큰을 다시 쓰면 거부되고, 그 사용자의 새 토큰도 같이 무효화된다")
    void reuseOfRevokedToken_isRejected_andRevokesAllSessions() throws Exception {
        String oldRefreshToken = JsonPath.read(login().getResponse().getContentAsString(), "$.refreshToken");

        MvcResult firstRefresh = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String rotatedToken = JsonPath.read(firstRefresh.getResponse().getContentAsString(), "$.refreshToken");

        // 이미 재발급에 쓰여서 폐기된 oldRefreshToken을 다시 사용 -> 탈취 의심 처리
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        // 그 사용자의 최신(정상) 토큰까지 같이 강제 폐기됐어야 한다
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotatedToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("존재하지 않는 리프레시 토큰이면 401")
    void refresh_withUnknownToken_returns401() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"not-a-real-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃하면 그 리프레시 토큰은 더 이상 못 쓴다")
    void logout_revokesToken() throws Exception {
        String refreshToken = JsonPath.read(login().getResponse().getContentAsString(), "$.refreshToken");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃은 토큰 없이/이미 로그아웃된 토큰이어도 에러 없이 처리된다 (멱등)")
    void logout_isIdempotent() throws Exception {
        String refreshToken = JsonPath.read(login().getResponse().getContentAsString(), "$.refreshToken");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());
    }
}
