package com.example.pinkok_backend.auth;

import com.jayway.jsonpath.JsonPath;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 지은 피드백: 잘못된 요청(404/400 등)이 전부 401로 보여서 원인을 못 찾는 문제.
 *
 * <p>확인해보니 두 가지가 섞여 있었다:
 * <ul>
 *   <li>토큰을 아예 안 보낸 요청은 - 어떤 주소든 - Spring Security가 컨트롤러까지
 *       가보지도 않고 401부터 준다. 이건 버그가 아니라 의도된 동작(주소가 있는지
 *       없는지도 비로그인 사용자에게 알려주지 않는 것)이라 고칠 수 없다.
 *       -> Postman에서 그 요청에 토큰을 빼먹지 않았는지 먼저 확인해야 한다.
 *   <li>토큰을 제대로 보냈는데도 에러(404/400 등)가 401로 나오는 경우 -
 *       Spring이 에러를 /error 로 forward할 때 SecurityConfig의
 *       anyRequest().authenticated() 에 걸려서 이렇게 된다. "/error" permitAll
 *       로 고쳤다.
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityErrorHandlingTest {

    @Autowired
    MockMvc mockMvc;

    private String signupAndLogin() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"errtest@pinkok.com\",\"username\":\"errtester\",\"password\":\"pass1234\",\"nickname\":\"테스터\"}"))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"errtest@pinkok.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    @DisplayName("토큰 없이 존재하지 않는 주소를 치면 401 (컨트롤러까지 가지 않음 - 정상 동작)")
    void unknownPath_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/auth/files"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰을 제대로 보냈으면, 존재하지 않는 주소는 401이 아니라 404가 나온다")
    void unknownPath_withValidToken_returns404_notUnauthorized() throws Exception {
        String token = signupAndLogin();

        mockMvc.perform(get("/users/mee") // 오타 - 존재하지 않는 경로
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("회원가입 요청 body가 아예 깨진 JSON이면 400이 나온다 (permitAll 경로라 토큰 불필요)")
    void malformedJson_returns400_notUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("this is not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ResponseStatusException도 {timestamp, status, message} 형태로 통일된다")
    void responseStatusException_usesUnifiedErrorFormat() throws Exception {
        String token = signupAndLogin();

        mockMvc.perform(patch("/users/me/avatar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatarId\":999999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("존재하지 않는 아바타입니다."));
    }
}
