package com.example.pinkok_backend.profile;

import com.example.pinkok_backend.entity.Avatar;
import com.example.pinkok_backend.repository.AvatarRepository;
import com.example.pinkok_backend.repository.UserRepository;
import com.example.pinkok_backend.seed.AvatarSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 기본 아바타 6종 자동 등록과 이미지 파일 제공 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvatarSeederTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AvatarSeeder avatarSeeder;

    @Autowired
    AvatarRepository avatarRepository;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        avatarRepository.deleteAll();
    }

    @Test
    @DisplayName("비어 있으면 6개를 넣고, 다시 실행해도 중복으로 넣지 않는다")
    void seed_isIdempotent() {
        assertThat(avatarSeeder.seed()).isEqualTo(6);
        assertThat(avatarSeeder.seed()).isZero();
        assertThat(avatarRepository.count()).isEqualTo(6);
    }

    @Test
    @DisplayName("일부만 있으면 빠진 것만 채운다")
    void seed_fillsOnlyMissing() {
        avatarSeeder.seed();
        Avatar m2 = avatarRepository.findAll().stream()
                .filter(a -> a.getCode().equals("M2"))
                .findFirst().orElseThrow();
        avatarRepository.delete(m2);

        assertThat(avatarSeeder.seed()).isEqualTo(1);
        assertThat(avatarRepository.count()).isEqualTo(6);
    }

    @Test
    @DisplayName("GET /avatars 는 남자1~3, 여자1~3 순서로 나오고, 이미지 주소가 실제로 열린다 (로그인 없이)")
    void avatarsListAndImages() throws Exception {
        avatarSeeder.seed();

        mockMvc.perform(get("/avatars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].code").value("M1"))
                .andExpect(jsonPath("$[0].name").value("남자1"))
                .andExpect(jsonPath("$[0].imageUrl").value("/images/avatars/m1.png"))
                .andExpect(jsonPath("$[5].code").value("F3"))
                .andExpect(jsonPath("$[5].gender").value("FEMALE"));

        for (String code : new String[]{"m1", "m2", "m3", "f1", "f2", "f3"}) {
            mockMvc.perform(get("/images/avatars/" + code + ".png"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("image/png"));
        }
    }
}
