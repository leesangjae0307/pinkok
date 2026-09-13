package com.example.pinkok_backend.auth;

import com.example.pinkok_backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    private static final String EMAIL = "tester@pinkok.com";
    private static final String USERNAME = "tester01";
    private static final String PASSWORD = "pass1234";
    private static final String NICKNAME = "테스터";

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    private static String signupBody(String email, String username, String password, String nickname) {
        return String.format(
                "{\"email\":\"%s\",\"username\":\"%s\",\"password\":\"%s\",\"nickname\":\"%s\"}",
                email, username, password, nickname);
    }

    private static String loginBody(String email, String password) {
        return String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
    }

    private void signup(String email, String username, String password, String nickname) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(email, username, password, nickname)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    @DisplayName("회원가입 성공 - 201, 비밀번호는 해시로 저장되고 응답에 노출되지 않는다")
    void signup_success() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(EMAIL, USERNAME, PASSWORD, NICKNAME)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.nickname").value(NICKNAME))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        String stored = userRepository.findByEmail(EMAIL).orElseThrow().getPasswordHash();
        assertNotNull(stored);
        assertNotEquals(PASSWORD, stored);
        assertTrue(stored.startsWith("$2"), "BCrypt 해시 형식이어야 한다");
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복 시 409")
    void signup_duplicateEmail() throws Exception {
        signup(EMAIL, USERNAME, PASSWORD, NICKNAME);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(EMAIL, "other02", "other1234", "다른사람")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("회원가입 실패 - 아이디(username) 중복 시 409")
    void signup_duplicateUsername() throws Exception {
        signup(EMAIL, USERNAME, PASSWORD, NICKNAME);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("other@pinkok.com", USERNAME, "other1234", "다른사람")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("회원가입 실패 - 짧은 비밀번호/잘못된 이메일/규칙 위반 username 이면 400")
    void signup_validation() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("not-an-email", "AB", "short", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("로그인 성공 - 200, Bearer accessToken 발급")
    void login_success() throws Exception {
        signup(EMAIL, USERNAME, PASSWORD, NICKNAME);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.email").value(EMAIL));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치면 401")
    void login_wrongPassword() throws Exception {
        signup(EMAIL, USERNAME, PASSWORD, NICKNAME);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, "wrongpass1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일이면 401")
    void login_unknownEmail() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody@pinkok.com", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/users/me - 토큰 없으면 401")
    void me_withoutToken() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/users/me - 변조된 토큰이면 401")
    void me_withTamperedToken() throws Exception {
        signup(EMAIL, USERNAME, PASSWORD, NICKNAME);
        String token = loginAndGetToken(EMAIL, PASSWORD);

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token + "tampered"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("전체 흐름 - 회원가입 후 로그인해서 받은 토큰으로 내 정보 조회")
    void fullFlow_signup_login_me() throws Exception {
        signup(EMAIL, USERNAME, PASSWORD, NICKNAME);
        String token = loginAndGetToken(EMAIL, PASSWORD);

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.nickname").value(NICKNAME));
    }
}
