package com.example.pinkok_backend.profile;

import com.example.pinkok_backend.support.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 사용자 설정 (알림 받기 · 기본 동행 유형). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSettingTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    private String signupAndLogin(String username) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s@pinkok.com\",\"username\":\"%s\",\"password\":\"pass1234\",\"nickname\":\"설정테스터\"}",
                                username, username)))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s@pinkok.com\",\"password\":\"pass1234\"}", username)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    @DisplayName("설정을 한 번도 안 건드렸어도 기본값으로 조회된다")
    void defaultSettings() throws Exception {
        String token = signupAndLogin("setting1");

        mockMvc.perform(get("/users/me/settings").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationEnabled").value(true))
                .andExpect(jsonPath("$.theme").value("PIXEL_MINT"))
                .andExpect(jsonPath("$.language").value("ko"));
    }

    @Test
    @DisplayName("알림 받기를 끄고 켤 수 있다")
    void updateSettings() throws Exception {
        String token = signupAndLogin("setting2");

        mockMvc.perform(patch("/users/me/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificationEnabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationEnabled").value(false));

        mockMvc.perform(get("/users/me/settings").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.notificationEnabled").value(false));

        mockMvc.perform(patch("/users/me/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificationEnabled\":true}"))
                .andExpect(jsonPath("$.notificationEnabled").value(true));
    }

    @Test
    @DisplayName("토큰 없으면 401")
    void withoutToken() throws Exception {
        mockMvc.perform(get("/users/me/settings"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/users/me/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificationEnabled\":false}"))
                .andExpect(status().isUnauthorized());
    }
}
