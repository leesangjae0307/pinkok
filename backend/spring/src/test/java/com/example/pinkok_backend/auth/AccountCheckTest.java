package com.example.pinkok_backend.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 가입 화면의 이메일·아이디 중복 확인과, 가입이 중복으로 거절될 때의 사유 코드. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountCheckTest {

    @Autowired MockMvc mockMvc;

    private void signup(String email, String username) throws Exception {
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"username\":\"" + username
                                + "\",\"password\":\"pass1234\",\"nickname\":\"닉\"}"))
                .andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- 중복 확인

    @Test
    @DisplayName("이메일 확인: 로그인 없이 쓸 수 있고, 없으면 사용 가능 / 가입돼 있으면 사용 불가")
    void checkEmail_availability() throws Exception {
        mockMvc.perform(get("/auth/check-email").param("email", "new@pinkok.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        signup("taken@pinkok.com", "takenuser");

        mockMvc.perform(get("/auth/check-email").param("email", "taken@pinkok.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("아이디 확인: 로그인 없이 쓸 수 있고, 없으면 사용 가능 / 가입돼 있으면 사용 불가")
    void checkUsername_availability() throws Exception {
        mockMvc.perform(get("/auth/check-username").param("username", "fresh_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        signup("a@pinkok.com", "taken_name");

        mockMvc.perform(get("/auth/check-username").param("username", "taken_name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("이메일 확인: 형식이 틀리거나 비어 있거나 값이 없으면 400")
    void checkEmail_badFormat_is400() throws Exception {
        mockMvc.perform(get("/auth/check-email").param("email", "not-an-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("형식이 올바르지 않습니다."));
        mockMvc.perform(get("/auth/check-email").param("email", ""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/auth/check-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("형식이 올바르지 않습니다."));
        mockMvc.perform(get("/auth/check-email").param("email", "a".repeat(250) + "@b.com"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("아이디 확인: 가입과 같은 규칙(3~30자, 영문 소문자·숫자·밑줄)을 어기면 400")
    void checkUsername_badFormat_is400() throws Exception {
        for (String bad : new String[]{"ab", "Upper", "with space", "한글아이디", "a".repeat(31), ""}) {
            mockMvc.perform(get("/auth/check-username").param("username", bad))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("형식이 올바르지 않습니다."));
        }
        mockMvc.perform(get("/auth/check-username"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- 가입 실패 사유

    @Test
    @DisplayName("가입: 이메일이 중복이면 409 + errorCode=EMAIL_DUPLICATED + errors.email")
    void signup_duplicateEmail_hasErrorCode() throws Exception {
        signup("dup@pinkok.com", "firstuser");

        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@pinkok.com\",\"username\":\"seconduser\","
                                + "\"password\":\"pass1234\",\"nickname\":\"닉\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("EMAIL_DUPLICATED"))
                .andExpect(jsonPath("$.errors.email").value("이미 존재하는 이메일입니다."))
                // 옛 앱처럼 message 만 읽어도 지금까지와 같다
                .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다."));
    }

    @Test
    @DisplayName("가입: 아이디가 중복이면 409 + errorCode=USERNAME_DUPLICATED + errors.username")
    void signup_duplicateUsername_hasErrorCode() throws Exception {
        signup("one@pinkok.com", "sameuser");

        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"two@pinkok.com\",\"username\":\"sameuser\","
                                + "\"password\":\"pass1234\",\"nickname\":\"닉\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USERNAME_DUPLICATED"))
                .andExpect(jsonPath("$.errors.username").value("이미 존재하는 아이디입니다."));
    }

    @Test
    @DisplayName("가입: 확인에서 사용 가능으로 나온 뒤 먼저 가입한 사람이 있으면, 가입이 최종적으로 막는다")
    void signup_isStillTheFinalJudge() throws Exception {
        mockMvc.perform(get("/auth/check-email").param("email", "race@pinkok.com"))
                .andExpect(jsonPath("$.available").value(true));

        signup("race@pinkok.com", "winner"); // 그 사이 다른 사람이 가입

        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"race@pinkok.com\",\"username\":\"loser\","
                                + "\"password\":\"pass1234\",\"nickname\":\"닉\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_DUPLICATED"));
    }
}
