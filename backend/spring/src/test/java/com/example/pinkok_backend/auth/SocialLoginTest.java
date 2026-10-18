package com.example.pinkok_backend.auth;

import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.UserRepository;
import com.example.pinkok_backend.seed.AvatarSeeder;
import com.example.pinkok_backend.social.GoogleSocialClient;
import com.example.pinkok_backend.social.KakaoSocialClient;
import com.example.pinkok_backend.social.SocialProfile;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 소셜 로그인 (제공자 확인은 가짜로 대신하고, 우리 쪽 계정 생성·토큰 발급·예외 처리를 확인한다). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SocialLoginTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AvatarSeeder avatarSeeder;

    @MockitoBean
    KakaoSocialClient kakao;
    @MockitoBean
    GoogleSocialClient google;

    @BeforeEach
    void seedAvatars() {
        avatarSeeder.seed();
    }

    private MvcResult socialLogin(String provider, String token) throws Exception {
        return mockMvc.perform(post("/auth/social/" + provider)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andReturn();
    }

    private static SocialProfile kakaoProfile(String uid, String email, boolean verified, String nickname) {
        return new SocialProfile("KAKAO", uid, email, verified, nickname);
    }

    @Test
    @DisplayName("처음 로그인하면 계정이 자동으로 만들어지고 토큰이 발급된다 (newUser=true, 아이디·아바타 자동)")
    void firstLogin_createsAccount() throws Exception {
        when(kakao.verify("kakao-token")).thenReturn(kakaoProfile("1001", "k1@example.com", true, "카카오유저"));

        mockMvc.perform(post("/auth/social/kakao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"kakao-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(true))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.nickname").value("카카오유저"))
                .andExpect(jsonPath("$.user.email").value("k1@example.com"))
                .andExpect(jsonPath("$.user.username").value(org.hamcrest.Matchers.matchesPattern("^kakao_[0-9a-f]{8}$")))
                .andExpect(jsonPath("$.user.avatar.id").isNotEmpty());

        User saved = userRepository.findByProviderAndProviderUid("KAKAO", "1001").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNull(saved.getPasswordHash());
    }

    @Test
    @DisplayName("같은 사람이 다시 로그인하면 같은 계정이고 newUser=false, 받은 토큰으로 내 정보도 조회된다")
    void secondLogin_sameAccount_andTokenWorks() throws Exception {
        when(kakao.verify(anyString())).thenReturn(kakaoProfile("1002", "k2@example.com", true, "두번째"));

        MvcResult first = socialLogin("kakao", "t1");
        MvcResult second = socialLogin("kakao", "t2");

        String body1 = first.getResponse().getContentAsString();
        String body2 = second.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(
                ((Number) JsonPath.read(body1, "$.user.id")).longValue(),
                ((Number) JsonPath.read(body2, "$.user.id")).longValue());
        org.junit.jupiter.api.Assertions.assertEquals(false, JsonPath.read(body2, "$.newUser"));

        String accessToken = JsonPath.read(body2, "$.accessToken");
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("두번째"));
    }

    @Test
    @DisplayName("소셜 로그인으로 받은 refresh 토큰으로도 토큰 갱신이 된다")
    void refreshTokenWorks() throws Exception {
        when(kakao.verify(anyString())).thenReturn(kakaoProfile("1003", "k3@example.com", true, "갱신"));
        String refresh = JsonPath.read(socialLogin("kakao", "t").getResponse().getContentAsString(), "$.refreshToken");

        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("이메일을 안 주거나 미인증이면 이메일 없는 계정으로 만들고, 그래도 로그인과 내 정보 조회가 된다")
    void noOrUnverifiedEmail_createsAccountWithoutEmail() throws Exception {
        when(kakao.verify("no-email")).thenReturn(kakaoProfile("2001", null, false, null));
        when(kakao.verify("unverified")).thenReturn(kakaoProfile("2002", "u@example.com", false, "미인증"));

        MvcResult a = socialLogin("kakao", "no-email");
        MvcResult b = socialLogin("kakao", "unverified");

        org.junit.jupiter.api.Assertions.assertEquals(200, a.getResponse().getStatus());
        org.junit.jupiter.api.Assertions.assertNull(userRepository.findByProviderAndProviderUid("KAKAO", "2001").orElseThrow().getEmail());
        org.junit.jupiter.api.Assertions.assertNull(userRepository.findByProviderAndProviderUid("KAKAO", "2002").orElseThrow().getEmail());
        // 닉네임을 못 받으면 기본 닉네임
        org.junit.jupiter.api.Assertions.assertEquals("여행자", JsonPath.read(a.getResponse().getContentAsString(), "$.user.nickname"));

        String token = JsonPath.read(b.getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("같은 이메일의 일반 계정이 이미 있으면 자동으로 합치지 않고 409")
    void emailOfExistingLocalAccount_returns409() throws Exception {
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"same@example.com\",\"username\":\"localuser1\",\"password\":\"pass1234\",\"nickname\":\"기존\"}"))
                .andExpect(status().isCreated());
        when(google.verify("g")).thenReturn(new SocialProfile("GOOGLE", "9001", "same@example.com", true, "구글"));

        mockMvc.perform(post("/auth/social/google").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"g\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("이미 가입된")));
    }

    @Test
    @DisplayName("카카오와 구글은 사용자 ID 가 같아도 서로 다른 계정이다")
    void sameUidDifferentProviders_areDifferentUsers() throws Exception {
        when(kakao.verify(anyString())).thenReturn(kakaoProfile("5555", "a@example.com", true, "카"));
        when(google.verify(anyString())).thenReturn(new SocialProfile("GOOGLE", "5555", "b@example.com", true, "구"));

        long kakaoId = ((Number) JsonPath.read(socialLogin("kakao", "t").getResponse().getContentAsString(), "$.user.id")).longValue();
        long googleId = ((Number) JsonPath.read(socialLogin("google", "t").getResponse().getContentAsString(), "$.user.id")).longValue();

        org.junit.jupiter.api.Assertions.assertNotEquals(kakaoId, googleId);
    }

    @Test
    @DisplayName("탈퇴한(삭제 표시된) 계정은 소셜 로그인으로 못 들어온다 (401)")
    void deletedAccount_returns401() throws Exception {
        when(kakao.verify(anyString())).thenReturn(kakaoProfile("6001", "d@example.com", true, "탈퇴"));
        socialLogin("kakao", "t");
        User user = userRepository.findByProviderAndProviderUid("KAKAO", "6001").orElseThrow();
        user.setDeletedAt(LocalDateTime.now());
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/auth/social/kakao").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"t\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("제공자가 토큰을 거부하면 그 상태(401)를, 설정이 없으면 503 을 그대로 돌려주고 계정은 만들어지지 않는다")
    void providerErrors_arePassedThrough() throws Exception {
        when(kakao.verify("bad")).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다."));
        when(google.verify("any")).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "구글 로그인이 설정되지 않았습니다."));
        long before = userRepository.count();

        mockMvc.perform(post("/auth/social/kakao").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"bad\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("카카오 인증에 실패했습니다."));
        mockMvc.perform(post("/auth/social/google").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"any\"}"))
                .andExpect(status().isServiceUnavailable());

        org.junit.jupiter.api.Assertions.assertEquals(before, userRepository.count());
    }

    @Test
    @DisplayName("지원하지 않는 제공자는 400, token 이 비어 있으면 400, 로그인 없이 호출할 수 있다")
    void validation() throws Exception {
        mockMvc.perform(post("/auth/social/naver").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"t\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/social/kakao").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/social/kakao").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("일반 로그인 응답에는 newUser=false 가 들어 있다 (기존 응답에 필드만 추가)")
    void normalLogin_hasNewUserFalse() throws Exception {
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"plain@example.com\",\"username\":\"plainuser\",\"password\":\"pass1234\",\"nickname\":\"일반\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"plain@example.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(false));
    }

    @Test
    @DisplayName("소셜 계정에는 비밀번호가 없어서 이메일·비밀번호 로그인으로는 들어올 수 없다")
    void socialAccountCannotLoginWithPassword() throws Exception {
        when(kakao.verify(anyString())).thenReturn(kakaoProfile("7001", "nopw@example.com", true, "비번없음"));
        socialLogin("kakao", "t");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nopw@example.com\",\"password\":\"anything1\"}"))
                .andExpect(status().isUnauthorized());
    }
}
