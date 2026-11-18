package com.example.pinkok_backend.category;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 장소 종류가 API 에 어떻게 보이는지.
 * <ul>
 *   <li>핀 응답의 place.category / categoryLabel / categoryOrder — 여행 "전체" 탭에서 종류별로 묶는 데 쓴다</li>
 *   <li>직접 추가한 장소에 사용자가 고른 종류(categoryGroupCode)가 반영되는지 — 목록과 취향 모두</li>
 *   <li>GET /places/categories — 종류 목록과 순서</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlaceCategoryApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    private String token;
    private Long tripId;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleaner.clean();
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cat@pinkok.com\",\"username\":\"cat_user\",\"password\":\"pass1234\",\"nickname\":\"분류\"}"))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cat@pinkok.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        MvcResult trip = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"강릉 여행\",\"region\":\"강릉\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        tripId = ((Number) JsonPath.read(trip.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    private String addPin(String placeJson) throws Exception {
        return mockMvc.perform(post("/itinerary-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"place\":%s}", tripId, placeJson)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("핀 응답에 장소 종류(코드·이름·순서)가 들어온다")
    void pinHasCategory() throws Exception {
        String json = addPin("{\"name\":\"모모스커피\",\"latitude\":35.1,\"longitude\":129.0,"
                + "\"categoryGroupCode\":\"CE7\",\"categoryName\":\"음식점 > 카페 > 커피전문점\"}");

        assertThat((String) JsonPath.read(json, "$.place.category")).isEqualTo("CE7");
        assertThat((String) JsonPath.read(json, "$.place.categoryLabel")).isEqualTo("카페");
        assertThat((Integer) JsonPath.read(json, "$.place.categoryOrder")).isEqualTo(1);
    }

    @Test
    @DisplayName("직접 추가한 장소에 종류를 고르면 목록에는 '숙소', 프로필 취향에는 '숙박'으로 보인다")
    void customPlaceWithPickedCategory() throws Exception {
        addPin("{\"name\":\"강릉 바다뷰 숙소\",\"roadAddress\":\"강릉시 임영로207번길 6-1\","
                + "\"latitude\":37.7556,\"longitude\":128.8942,\"categoryGroupCode\":\"AD5\"}");
        addPin("{\"name\":\"안목해변 앞 벤치\",\"latitude\":37.77,\"longitude\":128.95,\"categoryGroupCode\":\"SEA\"}");

        String items = mockMvc.perform(get("/itinerary-items").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((List<String>) JsonPath.read(items, "$[*].place.categoryLabel"))
                .containsExactlyInAnyOrder("숙소", "바다");

        String stats = mockMvc.perform(get("/users/me/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((List<String>) JsonPath.read(stats, "$.tastes[*].name")).containsExactlyInAnyOrder("숙박", "바다");
    }

    @Test
    @DisplayName("종류를 안 고르고 직접 추가한 장소는 '기타' (목록에는 보이고, 취향에서는 빠진다)")
    void customPlaceWithoutCategory() throws Exception {
        String json = addPin("{\"name\":\"친구네 집\",\"latitude\":37.7,\"longitude\":128.9}");
        assertThat((String) JsonPath.read(json, "$.place.category")).isEqualTo("ETC");
        assertThat((String) JsonPath.read(json, "$.place.categoryLabel")).isEqualTo("기타");
        assertThat((Integer) JsonPath.read(json, "$.place.categoryOrder")).isEqualTo(8);

        String stats = mockMvc.perform(get("/users/me/stats").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        assertThat((List<?>) JsonPath.read(stats, "$.tastes")).isEmpty();
    }

    @Test
    @DisplayName("GET /places/categories — 종류 9개를 보여줄 순서대로 (맛집 처음, 기타 마지막)")
    void categoryList() throws Exception {
        mockMvc.perform(get("/places/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[0].code").value("FD6"))
                .andExpect(jsonPath("$[0].label").value("맛집"))
                .andExpect(jsonPath("$[2].code").value("AD5"))
                .andExpect(jsonPath("$[2].label").value("숙소"))
                .andExpect(jsonPath("$[2].tasteLabel").value("숙박"))
                .andExpect(jsonPath("$[8].code").value("ETC"))
                .andExpect(jsonPath("$[8].order").value(8));
    }
}
