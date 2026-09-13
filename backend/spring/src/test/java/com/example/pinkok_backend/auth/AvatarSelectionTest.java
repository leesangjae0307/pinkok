package com.example.pinkok_backend.auth;

import com.example.pinkok_backend.entity.Avatar;
import com.example.pinkok_backend.repository.AvatarRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 아바타 목록 조회 · 회원가입 시 선택 · 이후 변경(/users/me/avatar) 테스트.
 * 시드 데이터(진짜 아바타 6종)는 별도 작업이라, 여기서는 테스트용 아바타를 직접 넣는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvatarSelectionTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AvatarRepository avatarRepository;

    private Long avatarId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        avatarRepository.deleteAll();

        Avatar avatar = new Avatar();
        avatar.setCode("M1");
        avatar.setName("모험가");
        avatar.setImageUrl("https://cdn.pinkok.app/avatars/m1.png");
        avatar.setGender("MALE");
        avatar.setDisplayOrder(1);
        avatarId = avatarRepository.save(avatar).getId();
    }

    private static String signupBodyWithAvatar(Long avatarId) {
        return String.format(
                "{\"email\":\"avatar@pinkok.com\",\"username\":\"avataruser\",\"password\":\"pass1234\",\"nickname\":\"테스터\",\"avatarId\":%d}",
                avatarId);
    }

    @Test
    @DisplayName("GET /avatars - 로그인 없이도 목록 조회 가능")
    void listAvatars_withoutAuth() throws Exception {
        mockMvc.perform(get("/avatars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("M1"))
                .andExpect(jsonPath("$[0].name").value("모험가"));
    }

    @Test
    @DisplayName("회원가입에 avatarId를 포함하면 응답에 아바타 정보가 담긴다")
    void signup_withAvatar() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBodyWithAvatar(avatarId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avatar.code").value("M1"));
    }

    @Test
    @DisplayName("회원가입에 avatarId가 없으면 아바타 없이 가입되고, 이후 PATCH로 선택할 수 있다")
    void signup_withoutAvatar_thenSelectLater() throws Exception {
        String bodyWithoutAvatar = "{\"email\":\"noavatar@pinkok.com\",\"username\":\"noavataruser\",\"password\":\"pass1234\",\"nickname\":\"테스터\"}";

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithoutAvatar))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avatar").doesNotExist());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"noavatar@pinkok.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(patch("/users/me/avatar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"avatarId\":%d}", avatarId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.code").value("M1"));
    }

    @Test
    @DisplayName("회원가입 시 존재하지 않는 avatarId면 400")
    void signup_withInvalidAvatar_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBodyWithAvatar(999_999L)))
                .andExpect(status().isBadRequest());
    }
}
