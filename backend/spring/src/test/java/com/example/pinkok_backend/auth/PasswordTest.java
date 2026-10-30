package com.example.pinkok_backend.auth;

import com.example.pinkok_backend.entity.PasswordResetCode;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.PasswordResetCodeRepository;
import com.example.pinkok_backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordTest {

    private static final String EMAIL = "pw@pinkok.com";
    private static final String OLD = "oldpass12";
    private static final String NEW = "newpass34";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetCodeRepository codeRepository;

    @MockitoBean
    com.example.pinkok_backend.service.PasswordResetMailer mailer;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"username\":\"pwuser\",\"password\":\"" + OLD
                                + "\",\"nickname\":\"비번\"}"))
                .andExpect(status().isCreated());
    }

    private MvcResult login(String password) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + password + "\"}")).andReturn();
    }

    private String body(MvcResult r, String path) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), path);
    }

    private String json(String... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append('"').append(kv[i]).append("\":\"").append(kv[i + 1]).append('"');
        }
        return sb.append('}').toString();
    }

    private int postStatus(String url, String content) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(content))
                .andReturn().getResponse().getStatus();
    }

    private int changeStatus(String token, String current, String next) throws Exception {
        return mockMvc.perform(patch("/users/me/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("currentPassword", current, "newPassword", next)))
                .andReturn().getResponse().getStatus();
    }

    /** 인증번호 발송을 요청하고 메일로 간 인증번호를 꺼낸다. */
    private String requestCode() throws Exception {
        assertEquals(204, postStatus("/auth/password-reset/request", json("email", EMAIL)));
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailer, times(1)).sendCode(eq(EMAIL), code.capture(), anyLong());
        return code.getValue();
    }

    // ---------------------------------------------------------------- 변경

    @Test
    @DisplayName("비밀번호 변경: 새 비밀번호로 로그인되고 옛 비밀번호는 막히며, 새 토큰을 돌려준다")
    void change_ok() throws Exception {
        MvcResult first = login(OLD);
        String access = body(first, "$.accessToken");
        String oldRefresh = body(first, "$.refreshToken");

        MvcResult changed = mockMvc.perform(patch("/users/me/password").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("currentPassword", OLD, "newPassword", NEW)))
                .andExpect(status().isOk()).andReturn();
        String newRefresh = body(changed, "$.refreshToken");

        assertEquals(401, login(OLD).getResponse().getStatus());
        assertEquals(200, login(NEW).getResponse().getStatus());

        // 다른 기기(옛 리프레시 토큰)는 끊기고, 응답으로 받은 새 토큰은 쓸 수 있다
        assertEquals(401, postStatus("/auth/refresh", json("refreshToken", oldRefresh)));
        assertEquals(200, postStatus("/auth/refresh", json("refreshToken", newRefresh)));
    }

    @Test
    @DisplayName("비밀번호 변경: 현재 비밀번호가 틀리면 401 이 아니라 400")
    void change_wrongCurrent_is400() throws Exception {
        String access = body(login(OLD), "$.accessToken");
        assertEquals(400, changeStatus(access, "wrongpass99", NEW));
        assertEquals(200, login(OLD).getResponse().getStatus());
    }

    @Test
    @DisplayName("비밀번호 변경: 같은 비밀번호·짧은 비밀번호·토큰 없음은 막힌다")
    void change_rejects() throws Exception {
        String access = body(login(OLD), "$.accessToken");
        assertEquals(400, changeStatus(access, OLD, OLD));
        assertEquals(400, changeStatus(access, OLD, "short"));
        assertEquals(401, changeStatus("not-a-token", OLD, NEW));
    }

    @Test
    @DisplayName("비밀번호 변경: 소셜 계정(비밀번호 없음)은 400")
    void change_socialAccount_is400() throws Exception {
        String access = body(login(OLD), "$.accessToken");
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        user.setPasswordHash(null);
        userRepository.save(user);
        assertEquals(400, changeStatus(access, OLD, NEW));
    }

    // ---------------------------------------------------------------- 재설정

    @Test
    @DisplayName("재설정: 인증번호 → 토큰 → 새 비밀번호로 끝까지 되고, 기존 로그인은 끊긴다")
    void reset_fullFlow() throws Exception {
        String oldRefresh = body(login(OLD), "$.refreshToken");
        String code = requestCode();
        assertEquals(6, code.length());

        MvcResult verified = mockMvc.perform(post("/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON).content(json("email", EMAIL, "code", code)))
                .andExpect(status().isOk()).andReturn();
        String resetToken = body(verified, "$.resetToken");

        assertEquals(204, postStatus("/auth/password-reset/confirm", json("resetToken", resetToken, "newPassword", NEW)));

        assertEquals(401, login(OLD).getResponse().getStatus());
        assertEquals(200, login(NEW).getResponse().getStatus());
        assertEquals(401, postStatus("/auth/refresh", json("refreshToken", oldRefresh)));

        // 토큰은 한 번만 쓸 수 있다
        assertEquals(400, postStatus("/auth/password-reset/confirm", json("resetToken", resetToken, "newPassword", "another123")));
    }

    @Test
    @DisplayName("재설정: 없는 이메일·소셜 계정에도 똑같이 204 이고 메일은 안 간다")
    void request_doesNotRevealAccounts() throws Exception {
        assertEquals(204, postStatus("/auth/password-reset/request", json("email", "nobody@pinkok.com")));

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        user.setPasswordHash(null);
        userRepository.save(user);
        assertEquals(204, postStatus("/auth/password-reset/request", json("email", EMAIL)));

        verify(mailer, never()).sendCode(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), anyLong());
    }

    @Test
    @DisplayName("재설정: 60초 안에 다시 요청하면 메일을 또 보내지 않는다")
    void request_cooldown() throws Exception {
        requestCode();
        assertEquals(204, postStatus("/auth/password-reset/request", json("email", EMAIL)));
        verify(mailer, times(1)).sendCode(eq(EMAIL), org.mockito.ArgumentMatchers.anyString(), anyLong());
    }

    @Test
    @DisplayName("재설정: 새 인증번호를 받으면 이전 인증번호는 못 쓴다")
    void newCodeInvalidatesOld() throws Exception {
        String first = requestCode();
        // 재발송 대기시간을 지난 것으로 만든다
        PasswordResetCode entry = codeRepository.findAll().get(0);
        entry.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        codeRepository.save(entry);

        assertEquals(204, postStatus("/auth/password-reset/request", json("email", EMAIL)));
        ArgumentCaptor<String> codes = ArgumentCaptor.forClass(String.class);
        verify(mailer, times(2)).sendCode(eq(EMAIL), codes.capture(), anyLong());
        String second = codes.getAllValues().get(1);

        if (!first.equals(second)) { // 우연히 같은 번호가 나오면 이 검사는 의미가 없다
            assertEquals(400, postStatus("/auth/password-reset/verify", json("email", EMAIL, "code", first)));
        }
        assertEquals(200, postStatus("/auth/password-reset/verify", json("email", EMAIL, "code", second)));
    }

    @Test
    @DisplayName("재설정: 5번 틀리면 맞는 번호도 거부된다")
    void verify_lockedAfterTooManyAttempts() throws Exception {
        String code = requestCode();
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            assertEquals(400, postStatus("/auth/password-reset/verify", json("email", EMAIL, "code", wrong)));
        }
        assertEquals(400, postStatus("/auth/password-reset/verify", json("email", EMAIL, "code", code)));
    }

    @Test
    @DisplayName("재설정: 만료된 인증번호·재설정 토큰은 거부된다")
    void expired() throws Exception {
        String code = requestCode();
        PasswordResetCode entry = codeRepository.findAll().get(0);
        entry.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        codeRepository.save(entry);
        assertEquals(400, postStatus("/auth/password-reset/verify", json("email", EMAIL, "code", code)));

        entry.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        codeRepository.save(entry);
        MvcResult verified = mockMvc.perform(post("/auth/password-reset/verify")
                .contentType(MediaType.APPLICATION_JSON).content(json("email", EMAIL, "code", code))).andReturn();
        String resetToken = body(verified, "$.resetToken");

        entry.setResetTokenExpiresAt(LocalDateTime.now().minusSeconds(1));
        codeRepository.save(entry);
        assertEquals(400, postStatus("/auth/password-reset/confirm", json("resetToken", resetToken, "newPassword", NEW)));
        assertTrue(login(OLD).getResponse().getStatus() == 200);
    }

    @Test
    @DisplayName("재설정: 인증번호 확인 없이는 토큰을 만들 수 없고, 입력 형식도 검증한다")
    void validation() throws Exception {
        assertEquals(400, postStatus("/auth/password-reset/confirm", json("resetToken", "made-up", "newPassword", NEW)));
        assertEquals(400, postStatus("/auth/password-reset/verify", json("email", EMAIL, "code", "12ab56")));
        assertEquals(400, postStatus("/auth/password-reset/request", json("email", "not-an-email")));
    }
}
