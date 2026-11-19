package com.example.pinkok_backend.trip;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 여행 자유 해시태그: 정리 규칙(#·공백·중복), 개수·길이 제한, 수정 방식. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripTagsTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleaner.clean();
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tag@pinkok.com\",\"username\":\"tagger\",\"password\":\"pass1234\",\"nickname\":\"태그\"}"))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tag@pinkok.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    private MvcResult createTrip(String tagsJson) throws Exception {
        return mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"부산 여행\",\"tags\":" + tagsJson + "}"))
                .andReturn();
    }

    private MvcResult patchTrip(long tripId, String body) throws Exception {
        return mockMvc.perform(patch("/trips/" + tripId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    @Test
    @DisplayName("만들 때 태그를 정리한다: 앞의 # 제거, 공백 제거, 빈 태그 버림, 대소문자 무시 중복 제거, 순서 유지")
    void normalizeOnCreate() throws Exception {
        MvcResult result = createTrip("[\"#부산\", \"##바다 여행\", \" \", \"#\", \"Cafe\", \"cafe\", \"부산\", \"맛집\"]");
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat((List<String>) JsonPath.read(result.getResponse().getContentAsString(), "$.tags"))
                .containsExactly("부산", "바다여행", "Cafe", "맛집");

        // 목록에도 같은 태그가 나온다
        String list = mockMvc.perform(get("/trips").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        assertThat((List<String>) JsonPath.read(list, "$[0].tags")).containsExactly("부산", "바다여행", "Cafe", "맛집");
    }

    @Test
    @DisplayName("태그를 안 보내면 빈 목록")
    void noTags() throws Exception {
        MvcResult result = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"태그 없는 여행\"}"))
                .andReturn();
        assertThat((List<?>) JsonPath.read(result.getResponse().getContentAsString(), "$.tags")).isEmpty();
    }

    @Test
    @DisplayName("수정: 안 보내면 그대로, 보내면 통째로 바꾸고, 빈 배열이면 전부 지운다")
    void updateTags() throws Exception {
        long tripId = ((Number) JsonPath.read(createTrip("[\"부산\", \"바다\"]").getResponse().getContentAsString(), "$.id"))
                .longValue();

        String unchanged = patchTrip(tripId, "{\"title\":\"부산 2박 3일\"}").getResponse().getContentAsString();
        assertThat((List<String>) JsonPath.read(unchanged, "$.tags")).containsExactly("부산", "바다");

        String replaced = patchTrip(tripId, "{\"tags\":[\"#야경\", \"부산\"]}").getResponse().getContentAsString();
        assertThat((List<String>) JsonPath.read(replaced, "$.tags")).containsExactly("야경", "부산");

        String cleared = patchTrip(tripId, "{\"tags\":[]}").getResponse().getContentAsString();
        assertThat((List<?>) JsonPath.read(cleared, "$.tags")).isEmpty();
    }

    @Test
    @DisplayName("11개 이상이거나 20자를 넘거나 중간에 # 이 있으면 400")
    void limits() throws Exception {
        assertThat(createTrip("[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\",\"10\",\"11\"]")
                .getResponse().getStatus()).isEqualTo(400);
        assertThat(createTrip("[\"" + "가".repeat(21) + "\"]").getResponse().getStatus()).isEqualTo(400);
        assertThat(createTrip("[\"부산#바다\"]").getResponse().getStatus()).isEqualTo(400);

        // 중복을 지우고 나서 10개면 괜찮다
        assertThat(createTrip("[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\",\"10\",\"#10\"]")
                .getResponse().getStatus()).isEqualTo(201);
        // 정확히 20자는 괜찮다
        assertThat(createTrip("[\"" + "가".repeat(20) + "\"]").getResponse().getStatus()).isEqualTo(201);
    }
}
